package conten.java.form;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handles all MySQL reads/writes for Ben Cafe, matching the ben_cafedb.sql schema you already
 * have (tblorder, tblorderdetail, tblproduct, tblcustomer). Requires the MySQL Connector/J jar
 * on the classpath at runtime:  https://dev.mysql.com/downloads/connector/j/
 *
 * The dashboard's current model is "one product per order", so every Dasboard.Order maps to
 * exactly one tblorder row + one matching tblorderdetail row.
 */
public class Dabmager {

    // ---- Update these 3 values for your own MySQL server ----
    private static final String URL = "jdbc:mysql://localhost:3306/ben_cafedb?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /** Quick check used at startup to warn (not crash) if MySQL isn't reachable. */
    public boolean testConnection() {
        try (Connection c = connect()) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    // ---------------- PRODUCTS ----------------

    /** Loads the menu (name -> price) from tblproduct. */
    public Map<String, Double> loadProducts() {
        Map<String, Double> products = new LinkedHashMap<>();
        String sql = "SELECT productName, productPrice FROM tblproduct WHERE productStatus = 'Available' ORDER BY productName";
        try (Connection conn = connect();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                products.put(rs.getString("productName"), rs.getDouble("productPrice"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }

    /** Inserts a new product, or updates its price if the name already exists. Used by the
     *  "+ Add Product" dialog. */
    public boolean saveProduct(String name, double price) {
        try (Connection conn = connect()) {
            Integer id = findProductId(conn, name);
            if (id != null) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE tblproduct SET productPrice = ? WHERE productId = ?")) {
                    ps.setDouble(1, price);
                    ps.setInt(2, id);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO tblproduct (productName, productPrice, productStatus) VALUES (?, ?, 'Available')")) {
                    ps.setString(1, name);
                    ps.setDouble(2, price);
                    ps.executeUpdate();
                }
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Integer findProductId(Connection conn, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT productId FROM tblproduct WHERE productName = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("productId");
            }
        }
        return null;
    }

    /** Finds an existing product by name, or creates it (used when saving an order). */
    private int getOrCreateProductId(Connection conn, String name, double price) throws SQLException {
        Integer id = findProductId(conn, name);
        if (id != null) return id;
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO tblproduct (productName, productPrice, productStatus) VALUES (?, ?, 'Available')",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setDouble(2, price);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Could not create product: " + name);
    }

    // ---------------- CUSTOMERS ----------------

    /** Finds an existing customer by name (first match), or creates a new tblcustomer row. */
    private int getOrCreateCustomerId(Connection conn, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT customerId FROM tblcustomer WHERE customerName = ? LIMIT 1")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("customerId");
            }
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO tblcustomer (customerName) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Could not create customer: " + name);
    }

    // ---------------- ORDERS ----------------

    /** Loads every order, joined back into the dashboard's flat Order shape. Assumes the
     *  dashboard's one-product-per-order model: exactly one tblorderdetail row per tblorder row. */
    public java.util.List<Dasboard.Order> loadOrders() {
        java.util.List<Dasboard.Order> orders = new java.util.ArrayList<>();
        String sql = "SELECT o.orderId, c.customerName, p.productName, d.quantity, " +
                     "o.paymentStatus, o.totalAmount, o.orderDate " +
                     "FROM tblorder o " +
                     "LEFT JOIN tblcustomer c ON o.customerId = c.customerId " +
                     "JOIN tblorderdetail d ON d.orderId = o.orderId " +
                     "JOIN tblproduct p ON d.productId = p.productId " +
                     "ORDER BY o.orderDate DESC";
        try (Connection conn = connect();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                LocalDateTime createdAt = rs.getTimestamp("orderDate").toLocalDateTime();
                String customerName = rs.getString("customerName");
                orders.add(new Dasboard.Order(
                        "#" + rs.getInt("orderId"),
                        customerName != null ? customerName : "Guest",
                        rs.getString("productName"),
                        rs.getInt("quantity"),
                        rs.getString("paymentStatus"),
                        rs.getDouble("totalAmount"),
                        createdAt.toLocalDate(),
                        createdAt));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }

    /** Inserts a brand-new order + its single order-detail line inside one transaction.
     *  Returns the generated numeric order id (shown in the UI as "#id"), or -1 on failure. */
    public int insertOrder(String customer, String product, int qty, String status,
                            double pricePerUnit, double total, LocalDateTime createdAt) {
        try (Connection conn = connect()) {
            conn.setAutoCommit(false);
            try {
                int customerId = getOrCreateCustomerId(conn, customer);
                int productId = getOrCreateProductId(conn, product, pricePerUnit);
                int orderId;

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO tblorder (customerId, orderDate, paymentMethod, paymentStatus, totalAmount) " +
                        "VALUES (?, ?, 'Cash', ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, customerId);
                    ps.setTimestamp(2, Timestamp.valueOf(createdAt));
                    ps.setString(3, status);
                    ps.setDouble(4, total);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("No generated order id returned");
                        orderId = keys.getInt(1);
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO tblorderdetail (orderId, productId, quantity, price, subTotal) " +
                        "VALUES (?, ?, ?, ?, ?)")) {
                    ps.setInt(1, orderId);
                    ps.setInt(2, productId);
                    ps.setInt(3, qty);
                    ps.setDouble(4, pricePerUnit);
                    ps.setDouble(5, total);
                    ps.executeUpdate();
                }

                conn.commit();
                return orderId;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /** Updates an existing order + its order-detail line together, inside one transaction.
     *  orderId is the app's display id, e.g. "#12". */
    public boolean updateOrder(String orderIdStr, String customer, String product, int qty,
                                String status, double pricePerUnit, double total) {
        int orderId = parseId(orderIdStr);
        if (orderId < 0) return false;
        try (Connection conn = connect()) {
            conn.setAutoCommit(false);
            try {
                int customerId = getOrCreateCustomerId(conn, customer);
                int productId = getOrCreateProductId(conn, product, pricePerUnit);

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE tblorder SET customerId = ?, paymentStatus = ?, totalAmount = ? WHERE orderId = ?")) {
                    ps.setInt(1, customerId);
                    ps.setString(2, status);
                    ps.setDouble(3, total);
                    ps.setInt(4, orderId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE tblorderdetail SET productId = ?, quantity = ?, price = ?, subTotal = ? WHERE orderId = ?")) {
                    ps.setInt(1, productId);
                    ps.setInt(2, qty);
                    ps.setDouble(3, pricePerUnit);
                    ps.setDouble(4, total);
                    ps.setInt(5, orderId);
                    ps.executeUpdate();
                }

                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Flips just the Paid/Pending status - used by clicking the Status column. */
    public boolean updateOrderStatus(String orderIdStr, String status) {
        int orderId = parseId(orderIdStr);
        if (orderId < 0) return false;
        String sql = "UPDATE tblorder SET paymentStatus = ? WHERE orderId = ?";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, orderId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Deletes an order; its tblorderdetail row(s) are removed automatically via ON DELETE CASCADE. */
    public boolean deleteOrder(String orderIdStr) {
        int orderId = parseId(orderIdStr);
        if (orderId < 0) return false;
        String sql = "DELETE FROM tblorder WHERE orderId = ?";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private int parseId(String orderIdStr) {
        try {
            return Integer.parseInt(orderIdStr.replace("#", "").trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}