package conten.java.form;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.*;
import login.java.form.LoginDesign;

public class Dasboard extends JFrame {

    // ---- Theme colors ----
    static final Color BG = new Color(253, 248, 242);
    static final Color SIDEBAR_BG = new Color(58, 41, 30);
    static final Color SIDEBAR_HOVER = new Color(75, 54, 40);
    static final Color ACCENT = new Color(168, 100, 60);
    static final Color CARD_BG = Color.WHITE;
    static final Color TEXT_DARK = new Color(40, 28, 20);
    static final Color TEXT_GRAY = new Color(140, 130, 120);
    static final Color GREEN = new Color(46, 139, 87);
    static final Color GREEN_BG = new Color(224, 245, 233);
    static final Color ORANGE = new Color(214, 138, 55);
    static final Color ORANGE_BG = new Color(252, 236, 213);
    static final Color BORDER = new Color(235, 227, 217);

    private static final String SEARCH_PLACEHOLDER = "Search anything...";

    // MySQL (ben_cafedb) is now the single source of truth for orders and products.
    private final Dabmager db = new Dabmager();

    // ---- Live state ----
    private JTextField searchField;
    private DefaultTableModel ordersModel;
    private TableRowSorter<DefaultTableModel> ordersSorter;
    private JTable ordersTable;
    private JLabel ordersCardTitleLabel;

    private JLabel lblTotalSalesValue;
    private JLabel lblTotalOrdersValue;
    private JLabel lblTotalCustomersValue;
    private JLabel lblTotalProductsValue;

    private JButton welcomeButton;
    private String currentRole = "Admin";
    private JPanel sidebarNavPanel;

    private JButton dateButton;
    private LocalDate selectedDate = LocalDate.now();

    private JPanel topProductsListPanel;
    private JPanel customersListPanel;
    private ChartPanel chartPanel;
    private JButton chartPeriodButton;
    private String currentChartPeriod = "This Week";

    private final Map<String, Double> productPrices = new LinkedHashMap<>();

    // ---- Persistent order data - the single source of truth for everything shown on the dashboard ----
    private List<Order> allOrders = new ArrayList<>();

    // "All Orders" window (kept alive so it can be refreshed live from anywhere new data is entered)
    private JDialog allOrdersDialog;
    private DefaultTableModel allOrdersModel;
    private TableRowSorter<DefaultTableModel> allOrdersSorter;

    // "All Products" window (same idea, for the Top Selling Products "View All")
    private JDialog allProductsDialog;
    private DefaultTableModel allProductsModel;
    private TableRowSorter<DefaultTableModel> allProductsSorter;

    // "All Customers" window (same idea, for the Recent Customers "View All")
    private JDialog allCustomersDialog;
    private DefaultTableModel allCustomersModel;
    private TableRowSorter<DefaultTableModel> allCustomersSorter;

    public Dasboard() {
        setTitle("Ben Cafe - Management System");
        try {
            ImageIcon icon = new ImageIcon("image/BenCafeLogo.png");
            this.setIconImage(icon.getImage());
        } catch (Exception ignored) {
            // Missing icon file shouldn't crash the app
        }
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);

        initProductCatalog();
        loadData();
        if (!db.testConnection()) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                    "Could not connect to the MySQL database (ben_cafedb).\n" +
                    "Check the URL / username / password at the top of DBManager.java.\n" +
                    "The app will still open, but nothing will be saved until this is fixed.",
                    "Database Connection", JOptionPane.WARNING_MESSAGE));
        }

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(buildMainContent(), BorderLayout.CENTER);

        setContentPane(root);

        refreshEverything();
    }

    private void initProductCatalog() {
        productPrices.put("Cappuccino", 4.50);
        productPrices.put("Latte", 4.50);
        productPrices.put("Espresso", 4.00);
        productPrices.put("Green Tea", 5.00);
        productPrices.put("Cheese Cake", 4.00);
    }

    // ---------------- DATA PERSISTENCE (MySQL) ----------------
    // Orders and products both live in MySQL now (ben_cafedb). Every add/edit/delete/status-toggle
    // below writes straight to the database via DBManager, so Total Sales, Total Orders, Total
    // Customers, Top Products, Recent Customers and the Sales Overview chart are always built
    // from real, saved data - and it survives restarts because MySQL lives outside the app.

    private void loadData() {
        Map<String, Double> loadedProducts = db.loadProducts();
        if (loadedProducts != null && !loadedProducts.isEmpty()) {
            productPrices.clear();
            productPrices.putAll(loadedProducts);
        }
        allOrders = db.loadOrders();
    }

    /** Central refresh: call after ANY data change so every panel reflects the persisted orders. */
    private void refreshEverything() {
        refreshTodaysOrdersTable();
        rebuildTopProducts();
        rebuildRecentCustomers();
        refreshChart();
        updateStats();
        refreshAllOrdersWindowIfOpen();
        refreshAllProductsWindowIfOpen();
        refreshAllCustomersWindowIfOpen();
    }

    private Order findOrderById(String id) {
        for (Order o : allOrders) {
            if (o.orderId.equals(id)) return o;
        }
        return null;
    }

    /**
     * Adds a new product to the menu (or updates its price if the name already exists).
     * This is the function Admin/Staff use to save a product - it writes straight to the
     * persisted data file so the product is still there next time the app opens.
     * Returns false if the name/price given wasn't valid, so the caller can show an error.
     */
    private boolean addProduct(String name, double price) {
        if (name == null || name.trim().isEmpty() || price <= 0) return false;
        String trimmed = name.trim();
        if (!db.saveProduct(trimmed, price)) return false;
        productPrices.put(trimmed, price);
        return true;
    }

    /** Small dialog Admin/Staff use to type in a product name + price and save it via addProduct(). */
    private void showAddProductDialog() {
        JTextField nameField = new JTextField();
        JTextField priceField = new JTextField();

        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        panel.add(new JLabel("Product Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Price ($):"));
        panel.add(priceField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add / Update Product",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        String name = nameField.getText().trim();
        double price;
        try {
            price = Double.parseDouble(priceField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid price.", "Invalid Price", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!addProduct(name, price)) {
            JOptionPane.showMessageDialog(this, "Please enter a product name and a price greater than 0.",
                    "Invalid Product", JOptionPane.ERROR_MESSAGE);
            return;
        }

        updateStats(); // Total Products count updates immediately
        JOptionPane.showMessageDialog(this, "\"" + name + "\" saved to the menu at $" + String.format("%.2f", price) + ".",
                "Product Saved", JOptionPane.INFORMATION_MESSAGE);
    }

    // ---------------- SIDEBAR ----------------
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(230, 0));
        sidebar.setBackground(SIDEBAR_BG);
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(20, 15, 20, 15));

        // Logo
        ImageIcon logo = new ImageIcon("image/BenCafe_logo.png");
        Image logoImage = logo.getImage().getScaledInstance(240, 200, Image.SCALE_SMOOTH);
        JLabel lblogo = new JLabel(new ImageIcon(logoImage));
        lblogo.setBounds(150, 10, 80, 80);
        lblogo.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(lblogo);

        JLabel logo2 = new JLabel("Ben Cafe");
        logo2.setFont(new Font("SansSerif", Font.BOLD, 20));
        logo2.setForeground(Color.WHITE);
        logo2.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Management System");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 11));
        sub.setForeground(new Color(240, 225, 200));
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        sidebar.add(logo2);
        sidebar.add(sub);
        sidebar.add(Box.createRigidArea(new Dimension(0, 30)));

        sidebarNavPanel = new JPanel();
        sidebarNavPanel.setOpaque(false);
        sidebarNavPanel.setLayout(new BoxLayout(sidebarNavPanel, BoxLayout.Y_AXIS));
        sidebarNavPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(sidebarNavPanel);
        rebuildSidebarNav(currentRole);

        sidebar.add(Box.createVerticalGlue());
        sidebar.add(logoutButton());

        return sidebar;
    }

    /** Rebuilds the nav list according to the active role (Admin sees everything, Staff sees a limited set). */
    private void rebuildSidebarNav(String role) {
        sidebarNavPanel.removeAll();
        String[] items = role.equals("Staff")
                ? new String[]{"Dashboard", "Orders", "Products", "Inventory"}
                : new String[]{"Dashboard", "Orders", "Products", "Staff", "Reports", "Settings"};

        for (int i = 0; i < items.length; i++) {
            sidebarNavPanel.add(navButton(items[i], i == 0));
            sidebarNavPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        }
        sidebarNavPanel.revalidate();
        sidebarNavPanel.repaint();
    }

    private JButton navButton(String text, boolean active) {
        JButton btn = new JButton(text);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(200, 40));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFont(new Font("SansSerif", active ? Font.BOLD : Font.PLAIN, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(active ? ACCENT : SIDEBAR_BG);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setBorder(new EmptyBorder(8, 15, 8, 10));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (btn.getBackground() != ACCENT) btn.setBackground(SIDEBAR_HOVER);
            }
            public void mouseExited(MouseEvent e) {
                if (btn.getBackground() != ACCENT) btn.setBackground(SIDEBAR_BG);
            }
        });
        btn.addActionListener(e -> {
            for (Component c : sidebarNavPanel.getComponents()) {
                if (c instanceof JButton) {
                    JButton b = (JButton) c;
                    b.setBackground(SIDEBAR_BG);
                    b.setFont(new Font("SansSerif", Font.PLAIN, 13));
                }
            }
            btn.setBackground(ACCENT);
            btn.setFont(new Font("SansSerif", Font.BOLD, 13));
            if (!text.equals("Dashboard")) {
                JOptionPane.showMessageDialog(this, text + " page is not built yet - Dashboard is the working view.",
                        text, JOptionPane.INFORMATION_MESSAGE);
            }
        });
        return btn;
    }

    private JButton logoutButton() {
        JButton btn = new JButton("Logout");
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(200, 40));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(SIDEBAR_BG);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setBorder(new EmptyBorder(8, 15, 8, 10));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(SIDEBAR_HOVER); }
            public void mouseExited(MouseEvent e) { btn.setBackground(SIDEBAR_BG); }
        });

        btn.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(this, "Are you sure you want to log out?",
                    "Confirm Logout", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                dispose();
                SwingUtilities.invokeLater(() -> {
                    LoginDesign ld = new LoginDesign();
                    ld.setVisible(true);
                });
            }
        });
        return btn;
    }

    // ---------------- MAIN CONTENT ----------------
    private JScrollPane buildMainContent() {
        JPanel content = new JPanel();
        content.setBackground(BG);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(20, 25, 20, 25));

        content.add(buildTopBar());
        content.add(Box.createRigidArea(new Dimension(0, 20)));
        content.add(buildGreetingRow());
        content.add(Box.createRigidArea(new Dimension(0, 20)));
        content.add(buildStatsRow());
        content.add(Box.createRigidArea(new Dimension(0, 20)));
        content.add(buildMiddleRow());
        content.add(Box.createRigidArea(new Dimension(0, 20)));
        content.add(buildBottomRow());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));

        searchField = new JTextField(SEARCH_PLACEHOLDER);
        searchField.setForeground(TEXT_GRAY);
        searchField.setPreferredSize(new Dimension(320, 40));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(8, 15, 8, 15)));
        searchField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (searchField.getText().equals(SEARCH_PLACEHOLDER)) {
                    searchField.setText("");
                    searchField.setForeground(TEXT_DARK);
                }
            }
            public void focusLost(FocusEvent e) {
                if (searchField.getText().trim().isEmpty()) {
                    searchField.setForeground(TEXT_GRAY);
                    searchField.setText(SEARCH_PLACEHOLDER);
                }
            }
        });
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applySearchFilter(); }
            public void removeUpdate(DocumentEvent e) { applySearchFilter(); }
            public void changedUpdate(DocumentEvent e) { applySearchFilter(); }
        });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        right.setBackground(BG);
        JLabel bell = new JLabel("\uD83D\uDD14");
        bell.setFont(new Font("SansSerif", Font.PLAIN, 18));

        welcomeButton = new JButton("Welcome, " + currentRole + " \u25BE");
        welcomeButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        welcomeButton.setForeground(TEXT_DARK);
        welcomeButton.setBackground(BG);
        welcomeButton.setBorderPainted(false);
        welcomeButton.setContentAreaFilled(false);
        welcomeButton.setFocusPainted(false);
        welcomeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        welcomeButton.addActionListener(e -> {
            JPopupMenu menu = new JPopupMenu();
            JMenuItem adminItem = new JMenuItem("Admin");
            JMenuItem staffItem = new JMenuItem("Staff");
            adminItem.addActionListener(a -> switchRole("Admin"));
            staffItem.addActionListener(a -> switchRole("Staff"));
            menu.add(adminItem);
            menu.add(staffItem);
            menu.show(welcomeButton, 0, welcomeButton.getHeight());
        });

        right.add(bell);
        right.add(welcomeButton);

        bar.add(searchField, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void switchRole(String role) {
        currentRole = role;
        welcomeButton.setText("Welcome, " + currentRole + " \u25BE");
        rebuildSidebarNav(currentRole);
    }

    private void applySearchFilter() {
        if (ordersSorter == null) return;
        String text = searchField.getText();
        if (text.equals(SEARCH_PLACEHOLDER) || text.trim().isEmpty()) {
            ordersSorter.setRowFilter(null);
            return;
        }
        try {
            RowFilter<DefaultTableModel, Object> rf = RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text));
            ordersSorter.setRowFilter(rf);
        } catch (Exception ex) {
            ordersSorter.setRowFilter(null);
        }
    }

    private JPanel buildGreetingRow() {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(BG);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        JLabel greet = new JLabel("<html><b style='font-size:16px'>\u2600 Good Morning, Admin!</b><br>"
                + "<span style='color:#8a7d70;font-size:12px'>Here's what's happening at your cafe today.</span></html>");

        dateButton = new JButton();
        dateButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        dateButton.setBackground(Color.WHITE);
        dateButton.setForeground(TEXT_DARK);
        dateButton.setFocusPainted(false);
        dateButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        dateButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true), new EmptyBorder(8, 15, 8, 15)));
        updateDateButtonText();
        dateButton.addActionListener(e -> {
            JPopupMenu popup = buildCalendarPopup();
            popup.show(dateButton, 0, dateButton.getHeight());
        });

        row.add(greet, BorderLayout.WEST);
        row.add(dateButton, BorderLayout.EAST);
        return row;
    }

    private void updateDateButtonText() {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"));
        dateButton.setText("\uD83D\uDCC5 " + selectedDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) + " | " + time);
    }

    /** Builds a small interactive month calendar popup used to browse which day's orders are shown. */
    private JPopupMenu buildCalendarPopup() {
        JPopupMenu popup = new JPopupMenu();
        JPanel panel = new JPanel(new BorderLayout(5, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setBackground(Color.WHITE);

        final YearMonth[] viewMonth = {YearMonth.from(selectedDate)};

        JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
        monthLabel.setFont(new Font("SansSerif", Font.BOLD, 13));

        JButton prev = new JButton("<");
        JButton next = new JButton(">");
        prev.setFocusPainted(false);
        next.setFocusPainted(false);
        prev.setCursor(new Cursor(Cursor.HAND_CURSOR));
        next.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(prev, BorderLayout.WEST);
        header.add(monthLabel, BorderLayout.CENTER);
        header.add(next, BorderLayout.EAST);

        JPanel grid = new JPanel(new GridLayout(0, 7, 3, 3));
        grid.setOpaque(false);

        Runnable[] refresh = new Runnable[1];
        refresh[0] = () -> {
            grid.removeAll();
            YearMonth ym = viewMonth[0];
            monthLabel.setText(ym.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + ym.getYear());

            String[] dow = {"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"};
            for (String d : dow) {
                JLabel l = new JLabel(d, SwingConstants.CENTER);
                l.setFont(new Font("SansSerif", Font.BOLD, 11));
                l.setForeground(TEXT_GRAY);
                grid.add(l);
            }

            LocalDate first = ym.atDay(1);
            int lead = first.getDayOfWeek().getValue() % 7; // Sunday = 0
            for (int i = 0; i < lead; i++) grid.add(new JLabel(""));

            for (int day = 1; day <= ym.lengthOfMonth(); day++) {
                LocalDate d = ym.atDay(day);
                JButton db = new JButton(String.valueOf(day));
                db.setMargin(new Insets(2, 2, 2, 2));
                db.setFocusPainted(false);
                db.setCursor(new Cursor(Cursor.HAND_CURSOR));
                boolean isSelected = d.equals(selectedDate);
                db.setOpaque(isSelected);
                db.setContentAreaFilled(isSelected);
                db.setBorderPainted(!isSelected);
                if (isSelected) {
                    db.setBackground(ACCENT);
                    db.setForeground(Color.WHITE);
                }
                db.addActionListener(e -> {
                    selectedDate = d;
                    updateDateButtonText();
                    popup.setVisible(false);
                    refreshTodaysOrdersTable();
                    updateStats();
                });
                grid.add(db);
            }
            grid.revalidate();
            grid.repaint();
        };

        prev.addActionListener(e -> {
            viewMonth[0] = viewMonth[0].minusMonths(1);
            refresh[0].run();
        });
        next.addActionListener(e -> {
            viewMonth[0] = viewMonth[0].plusMonths(1);
            refresh[0].run();
        });

        refresh[0].run();

        panel.add(header, BorderLayout.NORTH);
        panel.add(grid, BorderLayout.CENTER);
        popup.add(panel);
        return popup;
    }

    private JPanel buildStatsRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 20, 0));
        row.setBackground(BG);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        RoundedPanel salesCard = statCard("\uD83D\uDCB2", "Total Sales", GREEN);
        RoundedPanel ordersCard = statCard("\uD83D\uDED2", "Total Orders", GREEN);
        RoundedPanel customersCard = statCard("\uD83D\uDC65", "Total Customers", GREEN);
        RoundedPanel productsCard = statCard("\u2615", "Total Products", TEXT_GRAY);

        row.add(salesCard);
        row.add(ordersCard);
        row.add(customersCard);
        row.add(productsCard);
        return row;
    }

    /** Builds a stat card and stashes a handle to its value label so it can be updated live. */
    private RoundedPanel statCard(String icon, String title, Color deltaColor) {
        RoundedPanel card = new RoundedPanel(16, CARD_BG);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 18, 15, 18));

        JLabel iconLbl = new JLabel(icon);
        iconLbl.setFont(new Font("SansSerif", Font.PLAIN, 22));
        iconLbl.setOpaque(true);
        iconLbl.setBackground(new Color(250, 235, 220));
        iconLbl.setHorizontalAlignment(SwingConstants.CENTER);
        iconLbl.setPreferredSize(new Dimension(45, 45));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel t = new JLabel(title);
        t.setFont(new Font("SansSerif", Font.PLAIN, 12));
        t.setForeground(TEXT_GRAY);
        JLabel v = new JLabel("--");
        v.setFont(new Font("SansSerif", Font.BOLD, 20));
        JLabel d = new JLabel("Live");
        d.setFont(new Font("SansSerif", Font.PLAIN, 11));
        d.setForeground(deltaColor);
        text.add(t);
        text.add(v);
        text.add(d);

        switch (title) {
            case "Total Sales": lblTotalSalesValue = v; break;
            case "Total Orders": lblTotalOrdersValue = v; break;
            case "Total Customers": lblTotalCustomersValue = v; break;
            case "Total Products": lblTotalProductsValue = v; break;
            default: break;
        }

        card.add(iconLbl, BorderLayout.WEST);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(new EmptyBorder(0, 12, 0, 0));
        wrap.add(text, BorderLayout.CENTER);
        card.add(wrap, BorderLayout.CENTER);
        return card;
    }

    /**
     * Recomputes every stat card from the real persisted orders.
     * Total Sales / Total Orders reflect the day currently shown in Today's Orders (selectedDate).
     * Total Customers is the count of unique customers across every order ever entered.
     */
    private void updateStats() {
        double totalSales = 0;
        int totalOrders = ordersModel.getRowCount();
        for (int i = 0; i < ordersModel.getRowCount(); i++) {
            String status = String.valueOf(ordersModel.getValueAt(i, 3));
            if (status.equalsIgnoreCase("Paid")) {
                String totalStr = String.valueOf(ordersModel.getValueAt(i, 4)).replace("$", "").trim();
                try {
                    totalSales += Double.parseDouble(totalStr);
                } catch (NumberFormatException ignored) { }
            }
        }

        Set<String> uniqueCustomers = new HashSet<>();
        for (Order o : allOrders) {
            uniqueCustomers.add(o.customer.trim().toLowerCase(Locale.ROOT));
        }

        if (lblTotalSalesValue != null) lblTotalSalesValue.setText(String.format("$%.2f", totalSales));
        if (lblTotalOrdersValue != null) lblTotalOrdersValue.setText(String.valueOf(totalOrders));
        if (lblTotalCustomersValue != null) lblTotalCustomersValue.setText(String.valueOf(uniqueCustomers.size()));
        if (lblTotalProductsValue != null) lblTotalProductsValue.setText(String.valueOf(productPrices.size()));
    }

    private JPanel buildMiddleRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 20, 0));
        row.setBackground(BG);
        row.setPreferredSize(new Dimension(0, 340));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 340));

        row.add(buildOrdersCard());
        row.add(buildChartCard());
        return row;
    }

    private RoundedPanel buildOrdersCard() {
        RoundedPanel card = new RoundedPanel(16, CARD_BG);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(18, 18, 18, 18));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        ordersCardTitleLabel = new JLabel("Today's Orders");
        ordersCardTitleLabel.setFont(new Font("SansSerif", Font.BOLD, 15));

        JPanel headerButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerButtons.setOpaque(false);

        JButton newOrder = pillButton("+ New Order");
        newOrder.addActionListener(e -> showAddOrderDialog());

        // "View All Orders" opens a separate window (same visual style as this card) with
        // search, sortable columns, and Admin edit/delete for every order ever entered.
        JButton viewAll = pillButton("View All Orders");
        viewAll.addActionListener(e -> showAllOrdersWindow());

        headerButtons.add(newOrder);
        headerButtons.add(viewAll);

        header.add(ordersCardTitleLabel, BorderLayout.WEST);
        header.add(headerButtons, BorderLayout.EAST);

        String[] cols = {"Order ID", "Product", "Qty", "Status", "Total"};
        ordersModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        ordersTable = new JTable(ordersModel);
        ordersTable.setRowHeight(34);
        ordersTable.setShowGrid(false);
        ordersTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        ordersTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        ordersTable.getTableHeader().setForeground(TEXT_GRAY);
        ordersTable.setSelectionBackground(BG);
        ordersTable.getColumnModel().getColumn(3).setCellRenderer(new StatusRenderer());

        ordersSorter = new TableRowSorter<>(ordersModel);
        ordersTable.setRowSorter(ordersSorter);

        // Left-click the Status cell to toggle Paid <-> Pending (Admin only).
        // Right-click any row for Admin actions (Edit / Delete order).
        ordersTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e) || e.getClickCount() != 1) return;
                int viewRow = ordersTable.rowAtPoint(e.getPoint());
                int viewCol = ordersTable.columnAtPoint(e.getPoint());
                if (viewRow < 0 || viewCol != 3) return;
                if (!currentRole.equals("Admin")) {
                    JOptionPane.showMessageDialog(Dasboard.this,
                            "Only Admin can change order status.", "Permission Required",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
                int modelRow = ordersTable.convertRowIndexToModel(viewRow);
                String orderId = String.valueOf(ordersModel.getValueAt(modelRow, 0));
                Order o = findOrderById(orderId);
                if (o == null) return;
                String newStatus = o.status.equalsIgnoreCase("Paid") ? "Pending" : "Paid";
                if (!db.updateOrderStatus(orderId, newStatus)) {
                    JOptionPane.showMessageDialog(Dasboard.this, "Could not update status in the database.",
                            "Database Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                o.status = newStatus;
                refreshEverything();
            }

            public void mousePressed(MouseEvent e) { maybeShowOrderPopup(e); }
            public void mouseReleased(MouseEvent e) { maybeShowOrderPopup(e); }

            private void maybeShowOrderPopup(MouseEvent e) {
                if (!e.isPopupTrigger()) return;
                int viewRow = ordersTable.rowAtPoint(e.getPoint());
                if (viewRow < 0) return;
                ordersTable.setRowSelectionInterval(viewRow, viewRow);
                int modelRow = ordersTable.convertRowIndexToModel(viewRow);
                showOrderContextMenu(e, modelRow);
            }
        });

        ordersTable.setToolTipText("Admin: left-click Status to toggle, right-click a row to edit/delete");

        JScrollPane sp = new JScrollPane(ordersTable);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);

        card.add(header, BorderLayout.NORTH);
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    /** Redraws the Today's Orders table with only the orders that belong to the currently selected date. */
    private void refreshTodaysOrdersTable() {
        if (ordersModel == null) return;
        ordersModel.setRowCount(0);
        List<Order> dayOrders = new ArrayList<>();
        for (Order o : allOrders) {
            if (o.date.equals(selectedDate)) dayOrders.add(o);
        }
        dayOrders.sort((a, b) -> b.createdAt.compareTo(a.createdAt));
        for (Order o : dayOrders) {
            ordersModel.addRow(new Object[]{o.orderId, o.product, String.valueOf(o.qty), o.status,
                    String.format("$%.2f", o.total)});
        }
        if (ordersCardTitleLabel != null) {
            if (selectedDate.equals(LocalDate.now())) {
                ordersCardTitleLabel.setText("Today's Orders");
            } else {
                ordersCardTitleLabel.setText("Orders - " + selectedDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")));
            }
        }
    }

    /** Opens a dialog to create a new order (always dated today) and refreshes every panel that depends on it. */
    private void showAddOrderDialog() {
        JTextField customerField = new JTextField();
        JComboBox<String> productCombo = new JComboBox<>(productPrices.keySet().toArray(new String[0]));
        JSpinner qtySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 50, 1));
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"Paid", "Pending"});

        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        panel.add(new JLabel("Customer Name:"));
        panel.add(customerField);
        panel.add(new JLabel("Product:"));
        panel.add(productCombo);
        panel.add(new JLabel("Quantity:"));
        panel.add(qtySpinner);
        panel.add(new JLabel("Status:"));
        panel.add(statusCombo);

        int result = JOptionPane.showConfirmDialog(this, panel, "New Order",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        String customer = customerField.getText().trim();
        if (customer.isEmpty()) customer = "Guest";
        String product = (String) productCombo.getSelectedItem();
        int qty = (Integer) qtySpinner.getValue();
        String status = (String) statusCombo.getSelectedItem();
        double price = productPrices.getOrDefault(product, 0.0);
        double total = price * qty;

        LocalDateTime now = LocalDateTime.now();
        int generatedId = db.insertOrder(customer, product, qty, status, price, total, now);
        if (generatedId <= 0) {
            JOptionPane.showMessageDialog(this, "Could not save the order to the database.",
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String orderId = "#" + generatedId;
        Order order = new Order(orderId, customer, product, qty, status, total, now.toLocalDate(), now);
        allOrders.add(order);

        // If we're not currently looking at today, jump back to today so the new order is visible.
        selectedDate = LocalDate.now();
        updateDateButtonText();

        refreshEverything();
    }

    /** Right-click menu on an order row: Admin gets Edit/Delete, Staff sees a locked notice. */
    private void showOrderContextMenu(MouseEvent e, int modelRow) {
        String orderId = String.valueOf(ordersModel.getValueAt(modelRow, 0));
        JPopupMenu menu = new JPopupMenu();
        if (currentRole.equals("Admin")) {
            JMenuItem edit = new JMenuItem("Edit Order");
            edit.addActionListener(a -> editOrderById(orderId));
            JMenuItem delete = new JMenuItem("Delete Order");
            delete.addActionListener(a -> deleteOrderById(orderId));
            menu.add(edit);
            menu.add(delete);
        } else {
            JMenuItem locked = new JMenuItem("\uD83D\uDD12 Admin permission required");
            locked.setEnabled(false);
            menu.add(locked);
        }
        menu.show(ordersTable, e.getX(), e.getY());
    }

    /** Edits an existing order in place (Admin only), keyed by order id so it works from any table. */
    private void editOrderById(String orderId) {
        Order o = findOrderById(orderId);
        if (o == null) return;

        JTextField customerField = new JTextField(o.customer);
        JComboBox<String> productCombo = new JComboBox<>(productPrices.keySet().toArray(new String[0]));
        productCombo.setSelectedItem(o.product);
        JSpinner qtySpinner = new JSpinner(new SpinnerNumberModel(Math.max(o.qty, 1), 1, 50, 1));
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"Paid", "Pending"});
        statusCombo.setSelectedItem(o.status);

        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        panel.add(new JLabel("Customer Name:"));
        panel.add(customerField);
        panel.add(new JLabel("Product:"));
        panel.add(productCombo);
        panel.add(new JLabel("Quantity:"));
        panel.add(qtySpinner);
        panel.add(new JLabel("Status:"));
        panel.add(statusCombo);

        int result = JOptionPane.showConfirmDialog(this, panel, "Edit Order " + o.orderId,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        String newCustomer = customerField.getText().trim();
        if (newCustomer.isEmpty()) newCustomer = "Guest";
        String newProduct = (String) productCombo.getSelectedItem();
        int newQty = (Integer) qtySpinner.getValue();
        String newStatus = (String) statusCombo.getSelectedItem();
        double pricePerUnit = productPrices.getOrDefault(newProduct, 0.0);
        double newTotal = pricePerUnit * newQty;

        if (!db.updateOrder(o.orderId, newCustomer, newProduct, newQty, newStatus, pricePerUnit, newTotal)) {
            JOptionPane.showMessageDialog(this, "Could not update the order in the database.",
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        o.customer = newCustomer;
        o.product = newProduct;
        o.qty = newQty;
        o.status = newStatus;
        o.total = newTotal;

        refreshEverything();
    }

    /** Removes an order entirely (Admin only), keyed by order id so it works from any table. */
    private void deleteOrderById(String orderId) {
        int result = JOptionPane.showConfirmDialog(this,
                "Delete order " + orderId + "? This cannot be undone.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (result != JOptionPane.YES_OPTION) return;

        if (!db.deleteOrder(orderId)) {
            JOptionPane.showMessageDialog(this, "Could not delete the order from the database.",
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        allOrders.removeIf(o -> o.orderId.equals(orderId));
        refreshEverything();
    }

    // ---------------- ALL ORDERS WINDOW ----------------
    /**
     * Opens a separate window - styled the same way as the Today's Orders card (rounded white card,
     * same fonts/colors/buttons) - listing every order ever entered, with search, click-to-sort
     * columns, and Admin edit/delete.
     */
    private void showAllOrdersWindow() {
        if (allOrdersDialog != null && allOrdersDialog.isDisplayable()) {
            refreshAllOrdersWindowIfOpen();
            allOrdersDialog.setVisible(true);
            allOrdersDialog.toFront();
            return;
        }

        allOrdersDialog = new JDialog(this, "All Orders - Ben Cafe", false);
        allOrdersDialog.setSize(950, 620);
        allOrdersDialog.setLocationRelativeTo(this);
        allOrdersDialog.getContentPane().setBackground(BG);
        allOrdersDialog.setLayout(new BorderLayout());

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG);
        wrapper.setBorder(new EmptyBorder(20, 20, 20, 20));

        RoundedPanel card = new RoundedPanel(16, CARD_BG);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(18, 18, 18, 18));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("All Orders");
        title.setFont(new Font("SansSerif", Font.BOLD, 15));

        JTextField searchAll = new JTextField();
        searchAll.setPreferredSize(new Dimension(220, 34));
        searchAll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true), new EmptyBorder(6, 12, 6, 12)));

        JButton newOrderBtn = pillButton("+ New Order");
        newOrderBtn.addActionListener(e -> showAddOrderDialog());

        JPanel headerButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerButtons.setOpaque(false);
        headerButtons.add(searchAll);
        headerButtons.add(newOrderBtn);

        header.add(title, BorderLayout.WEST);
        header.add(headerButtons, BorderLayout.EAST);

        String[] cols = {"Date", "Order ID", "Customer", "Product", "Qty", "Status", "Total"};
        allOrdersModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable allTable = new JTable(allOrdersModel);
        allTable.setRowHeight(32);
        allTable.setShowGrid(false);
        allTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        allTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        allTable.getTableHeader().setForeground(TEXT_GRAY);
        allTable.setSelectionBackground(BG);
        allTable.getColumnModel().getColumn(5).setCellRenderer(new StatusRenderer());

        allOrdersSorter = new TableRowSorter<>(allOrdersModel);
        allTable.setRowSorter(allOrdersSorter);

        searchAll.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
            private void filter() {
                String text = searchAll.getText().trim();
                if (text.isEmpty()) { allOrdersSorter.setRowFilter(null); return; }
                try {
                    allOrdersSorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text)));
                } catch (Exception ex) {
                    allOrdersSorter.setRowFilter(null);
                }
            }
        });

        allTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e) || e.getClickCount() != 1) return;
                int viewRow = allTable.rowAtPoint(e.getPoint());
                int viewCol = allTable.columnAtPoint(e.getPoint());
                if (viewRow < 0 || viewCol != 5) return;
                if (!currentRole.equals("Admin")) {
                    JOptionPane.showMessageDialog(allOrdersDialog, "Only Admin can change order status.",
                            "Permission Required", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                int modelRow = allTable.convertRowIndexToModel(viewRow);
                String orderId = String.valueOf(allOrdersModel.getValueAt(modelRow, 1));
                Order o = findOrderById(orderId);
                if (o == null) return;
                String newStatus = o.status.equalsIgnoreCase("Paid") ? "Pending" : "Paid";
                if (!db.updateOrderStatus(orderId, newStatus)) {
                    JOptionPane.showMessageDialog(allOrdersDialog, "Could not update status in the database.",
                            "Database Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                o.status = newStatus;
                refreshEverything();
            }

            public void mousePressed(MouseEvent e) { maybeShowPopup(e); }
            public void mouseReleased(MouseEvent e) { maybeShowPopup(e); }

            private void maybeShowPopup(MouseEvent e) {
                if (!e.isPopupTrigger()) return;
                int viewRow = allTable.rowAtPoint(e.getPoint());
                if (viewRow < 0) return;
                allTable.setRowSelectionInterval(viewRow, viewRow);
                int modelRow = allTable.convertRowIndexToModel(viewRow);
                String orderId = String.valueOf(allOrdersModel.getValueAt(modelRow, 1));

                JPopupMenu menu = new JPopupMenu();
                if (currentRole.equals("Admin")) {
                    JMenuItem edit = new JMenuItem("Edit Order");
                    edit.addActionListener(a -> editOrderById(orderId));
                    JMenuItem delete = new JMenuItem("Delete Order");
                    delete.addActionListener(a -> deleteOrderById(orderId));
                    menu.add(edit);
                    menu.add(delete);
                } else {
                    JMenuItem locked = new JMenuItem("\uD83D\uDD12 Admin permission required");
                    locked.setEnabled(false);
                    menu.add(locked);
                }
                menu.show(allTable, e.getX(), e.getY());
            }
        });

        allTable.setToolTipText("Admin: left-click Status to toggle, right-click a row to edit/delete. Click a column header to sort.");

        JScrollPane sp = new JScrollPane(allTable);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);

        card.add(header, BorderLayout.NORTH);
        card.add(sp, BorderLayout.CENTER);
        wrapper.add(card, BorderLayout.CENTER);
        allOrdersDialog.add(wrapper, BorderLayout.CENTER);

        refreshAllOrdersWindowIfOpen();
        allOrdersDialog.setVisible(true);
    }

    /** Keeps the All Orders window (if currently open) in sync with any change made elsewhere. */
    private void refreshAllOrdersWindowIfOpen() {
        if (allOrdersModel == null) return;
        allOrdersModel.setRowCount(0);
        List<Order> sorted = new ArrayList<>(allOrders);
        sorted.sort((a, b) -> b.createdAt.compareTo(a.createdAt));
        for (Order o : sorted) {
            allOrdersModel.addRow(new Object[]{
                    o.date.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                    o.orderId, o.customer, o.product, String.valueOf(o.qty), o.status,
                    String.format("$%.2f", o.total)
            });
        }
    }

    // ---------------- ALL PRODUCTS WINDOW ----------------
    /**
     * Opens a separate window - styled the same way as the Today's Orders / All Orders card -
     * showing every product on the menu with units sold and revenue, with search and click-to-sort.
     */
    private void showAllProductsWindow() {
        if (allProductsDialog != null && allProductsDialog.isDisplayable()) {
            refreshAllProductsWindowIfOpen();
            allProductsDialog.setVisible(true);
            allProductsDialog.toFront();
            return;
        }

        allProductsDialog = new JDialog(this, "All Products - Ben Cafe", false);
        allProductsDialog.setSize(750, 560);
        allProductsDialog.setLocationRelativeTo(this);
        allProductsDialog.getContentPane().setBackground(BG);
        allProductsDialog.setLayout(new BorderLayout());

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG);
        wrapper.setBorder(new EmptyBorder(20, 20, 20, 20));

        RoundedPanel card = new RoundedPanel(16, CARD_BG);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(18, 18, 18, 18));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("All Products");
        title.setFont(new Font("SansSerif", Font.BOLD, 15));

        JTextField searchProducts = new JTextField();
        searchProducts.setPreferredSize(new Dimension(200, 34));
        searchProducts.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true), new EmptyBorder(6, 12, 6, 12)));

        JButton addProductBtn = pillButton("+ Add Product");
        addProductBtn.addActionListener(e -> showAddProductDialog());

        JPanel headerButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerButtons.setOpaque(false);
        headerButtons.add(searchProducts);
        headerButtons.add(addProductBtn);

        header.add(title, BorderLayout.WEST);
        header.add(headerButtons, BorderLayout.EAST);

        String[] cols = {"Product", "Price", "Units Sold", "Revenue"};
        allProductsModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable allProductsTable = new JTable(allProductsModel);
        allProductsTable.setRowHeight(32);
        allProductsTable.setShowGrid(false);
        allProductsTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        allProductsTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        allProductsTable.getTableHeader().setForeground(TEXT_GRAY);
        allProductsTable.setSelectionBackground(BG);

        allProductsSorter = new TableRowSorter<>(allProductsModel);
        allProductsTable.setRowSorter(allProductsSorter);

        searchProducts.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
            private void filter() {
                String text = searchProducts.getText().trim();
                if (text.isEmpty()) { allProductsSorter.setRowFilter(null); return; }
                try {
                    allProductsSorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text)));
                } catch (Exception ex) {
                    allProductsSorter.setRowFilter(null);
                }
            }
        });

        allProductsTable.setToolTipText("Click a column header to sort.");

        JScrollPane sp = new JScrollPane(allProductsTable);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);

        card.add(header, BorderLayout.NORTH);
        card.add(sp, BorderLayout.CENTER);
        wrapper.add(card, BorderLayout.CENTER);
        allProductsDialog.add(wrapper, BorderLayout.CENTER);

        refreshAllProductsWindowIfOpen();
        allProductsDialog.setVisible(true);
    }

    /** Keeps the All Products window (if currently open) in sync with every real order and the menu. */
    private void refreshAllProductsWindowIfOpen() {
        if (allProductsModel == null) return;
        allProductsModel.setRowCount(0);

        Map<String, Integer> unitsSold = new LinkedHashMap<>();
        Map<String, Double> revenue = new LinkedHashMap<>();
        for (Order o : allOrders) {
            unitsSold.merge(o.product, o.qty, Integer::sum);
            if (o.status.equalsIgnoreCase("Paid")) {
                revenue.merge(o.product, o.total, Double::sum);
            }
        }

        List<String> productNames = new ArrayList<>(productPrices.keySet());
        for (String p : unitsSold.keySet()) {
            if (!productNames.contains(p)) productNames.add(p);
        }
        productNames.sort((a, b) -> unitsSold.getOrDefault(b, 0) - unitsSold.getOrDefault(a, 0));

        for (String name : productNames) {
            double price = productPrices.getOrDefault(name, 0.0);
            int sold = unitsSold.getOrDefault(name, 0);
            double rev = revenue.getOrDefault(name, 0.0);
            allProductsModel.addRow(new Object[]{name, String.format("$%.2f", price), sold, String.format("$%.2f", rev)});
        }
    }

    // ---------------- ALL CUSTOMERS WINDOW ----------------
    /**
     * Opens a separate window - styled the same way as the Today's Orders / All Orders card -
     * showing every customer who ever ordered, with order count, total spent, and last order time.
     */
    private void showAllCustomersWindow() {
        if (allCustomersDialog != null && allCustomersDialog.isDisplayable()) {
            refreshAllCustomersWindowIfOpen();
            allCustomersDialog.setVisible(true);
            allCustomersDialog.toFront();
            return;
        }

        allCustomersDialog = new JDialog(this, "All Customers - Ben Cafe", false);
        allCustomersDialog.setSize(750, 560);
        allCustomersDialog.setLocationRelativeTo(this);
        allCustomersDialog.getContentPane().setBackground(BG);
        allCustomersDialog.setLayout(new BorderLayout());

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG);
        wrapper.setBorder(new EmptyBorder(20, 20, 20, 20));

        RoundedPanel card = new RoundedPanel(16, CARD_BG);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(18, 18, 18, 18));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("All Customers");
        title.setFont(new Font("SansSerif", Font.BOLD, 15));

        JTextField searchCustomers = new JTextField();
        searchCustomers.setPreferredSize(new Dimension(220, 34));
        searchCustomers.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true), new EmptyBorder(6, 12, 6, 12)));

        JPanel headerButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerButtons.setOpaque(false);
        headerButtons.add(searchCustomers);

        header.add(title, BorderLayout.WEST);
        header.add(headerButtons, BorderLayout.EAST);

        String[] cols = {"Customer", "Orders", "Total Spent", "Last Order"};
        allCustomersModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable allCustomersTable = new JTable(allCustomersModel);
        allCustomersTable.setRowHeight(32);
        allCustomersTable.setShowGrid(false);
        allCustomersTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        allCustomersTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        allCustomersTable.getTableHeader().setForeground(TEXT_GRAY);
        allCustomersTable.setSelectionBackground(BG);

        allCustomersSorter = new TableRowSorter<>(allCustomersModel);
        allCustomersTable.setRowSorter(allCustomersSorter);

        searchCustomers.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
            private void filter() {
                String text = searchCustomers.getText().trim();
                if (text.isEmpty()) { allCustomersSorter.setRowFilter(null); return; }
                try {
                    allCustomersSorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text)));
                } catch (Exception ex) {
                    allCustomersSorter.setRowFilter(null);
                }
            }
        });

        allCustomersTable.setToolTipText("Click a column header to sort.");

        JScrollPane sp = new JScrollPane(allCustomersTable);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);

        card.add(header, BorderLayout.NORTH);
        card.add(sp, BorderLayout.CENTER);
        wrapper.add(card, BorderLayout.CENTER);
        allCustomersDialog.add(wrapper, BorderLayout.CENTER);

        refreshAllCustomersWindowIfOpen();
        allCustomersDialog.setVisible(true);
    }

    /** Keeps the All Customers window (if currently open) in sync with every real order. */
    private void refreshAllCustomersWindowIfOpen() {
        if (allCustomersModel == null) return;
        allCustomersModel.setRowCount(0);

        // Group by lower-cased name (so "Emily" and "emily" merge) but keep the first-seen display casing.
        Map<String, String> displayNames = new LinkedHashMap<>();
        Map<String, Integer> orderCounts = new LinkedHashMap<>();
        Map<String, Double> totalSpent = new LinkedHashMap<>();
        Map<String, LocalDateTime> lastOrder = new LinkedHashMap<>();

        for (Order o : allOrders) {
            String key = o.customer.trim().toLowerCase(Locale.ROOT);
            displayNames.putIfAbsent(key, o.customer.trim());
            orderCounts.merge(key, 1, Integer::sum);
            if (o.status.equalsIgnoreCase("Paid")) {
                totalSpent.merge(key, o.total, Double::sum);
            } else {
                totalSpent.putIfAbsent(key, 0.0);
            }
            LocalDateTime prev = lastOrder.get(key);
            if (prev == null || o.createdAt.isAfter(prev)) {
                lastOrder.put(key, o.createdAt);
            }
        }

        List<String> keys = new ArrayList<>(displayNames.keySet());
        keys.sort((a, b) -> lastOrder.get(b).compareTo(lastOrder.get(a)));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d, yyyy hh:mm a");
        for (String key : keys) {
            allCustomersModel.addRow(new Object[]{
                    displayNames.get(key),
                    orderCounts.getOrDefault(key, 0),
                    String.format("$%.2f", totalSpent.getOrDefault(key, 0.0)),
                    lastOrder.get(key).format(fmt)
            });
        }
    }

    // ---------------- SALES OVERVIEW CHART ----------------
    // The chart is always computed from the real, persisted orders - never from placeholder data -
    // so it keeps showing every day/week/month/year of real sales for as long as the data exists.

    private RoundedPanel buildChartCard() {
        RoundedPanel card = new RoundedPanel(16, CARD_BG);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(18, 18, 18, 18));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Sales Overview");
        title.setFont(new Font("SansSerif", Font.BOLD, 15));

        chartPeriodButton = pillButton(currentChartPeriod + " \u25BE");
        chartPeriodButton.addActionListener(e -> {
            JPopupMenu menu = new JPopupMenu();
            for (String period : new String[]{"This Week", "This Month", "This Year"}) {
                JMenuItem item = new JMenuItem(period);
                item.addActionListener(a -> {
                    currentChartPeriod = period;
                    chartPeriodButton.setText(period + " \u25BE");
                    refreshChart();
                });
                menu.add(item);
            }
            menu.show(chartPeriodButton, 0, chartPeriodButton.getHeight());
        });

        header.add(title, BorderLayout.WEST);
        header.add(chartPeriodButton, BorderLayout.EAST);

        chartPanel = new ChartPanel(computeChartValues(currentChartPeriod), computeChartLabels(currentChartPeriod));

        card.add(header, BorderLayout.NORTH);
        card.add(chartPanel, BorderLayout.CENTER);
        return card;
    }

    private void refreshChart() {
        if (chartPanel == null) return;
        chartPanel.setData(computeChartValues(currentChartPeriod), computeChartLabels(currentChartPeriod));
    }

    private String[] computeChartLabels(String period) {
        switch (period) {
            case "This Month": return new String[]{"Wk1", "Wk2", "Wk3", "Wk4"};
            case "This Year": return new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
            default: return new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        }
    }

    private double[] computeChartValues(String period) {
        LocalDate today = LocalDate.now();
        if (period.equals("This Month")) {
            YearMonth ym = YearMonth.from(today);
            double[] vals = new double[4];
            LocalDate first = ym.atDay(1);
            LocalDate last = ym.atEndOfMonth();
            for (Order o : allOrders) {
                if (!o.status.equalsIgnoreCase("Paid")) continue;
                if (o.date.isBefore(first) || o.date.isAfter(last)) continue;
                int weekIdx = Math.min(3, (o.date.getDayOfMonth() - 1) / 7);
                vals[weekIdx] += o.total;
            }
            return vals;
        } else if (period.equals("This Year")) {
            double[] vals = new double[12];
            int year = today.getYear();
            for (Order o : allOrders) {
                if (!o.status.equalsIgnoreCase("Paid")) continue;
                if (o.date.getYear() != year) continue;
                vals[o.date.getMonthValue() - 1] += o.total;
            }
            return vals;
        } else { // This Week
            LocalDate monday = today.with(DayOfWeek.MONDAY);
            double[] vals = new double[7];
            for (int i = 0; i < 7; i++) {
                LocalDate d = monday.plusDays(i);
                double sum = 0;
                for (Order o : allOrders) {
                    if (o.date.equals(d) && o.status.equalsIgnoreCase("Paid")) sum += o.total;
                }
                vals[i] = sum;
            }
            return vals;
        }
    }

    private JPanel buildBottomRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 20, 0));
        row.setBackground(BG);
        row.setPreferredSize(new Dimension(0, 240));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));

        row.add(buildTopProductsCard());
        row.add(buildRecentCustomersCard());
        return row;
    }

    private RoundedPanel buildTopProductsCard() {
        RoundedPanel card = new RoundedPanel(16, CARD_BG);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(18, 18, 18, 18));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Top Selling Products");
        title.setFont(new Font("SansSerif", Font.BOLD, 15));

        JButton addProductBtn = pillButton("+ Add Product");
        addProductBtn.addActionListener(e -> showAddProductDialog());

        JButton viewAll = pillButton("View All");
        viewAll.addActionListener(e -> showAllProductsWindow());

        JPanel headerButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerButtons.setOpaque(false);
        headerButtons.add(addProductBtn);
        headerButtons.add(viewAll);

        header.add(title, BorderLayout.WEST);
        header.add(headerButtons, BorderLayout.EAST);

        topProductsListPanel = new JPanel();
        topProductsListPanel.setOpaque(false);
        topProductsListPanel.setLayout(new BoxLayout(topProductsListPanel, BoxLayout.Y_AXIS));
        topProductsListPanel.setBorder(new EmptyBorder(15, 0, 0, 0));

        card.add(header, BorderLayout.NORTH);
        card.add(topProductsListPanel, BorderLayout.CENTER);

        rebuildTopProducts();
        return card;
    }

    /** Redraws the top-3 product rows, sorted by units sold, recomputed fresh from every real order. */
    private void rebuildTopProducts() {
        if (topProductsListPanel == null) return;
        topProductsListPanel.removeAll();

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Order o : allOrders) {
            counts.merge(o.product, o.qty, Integer::sum);
        }
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());

        int max = sorted.isEmpty() ? 1 : sorted.get(0).getValue();
        int shown = 0;
        for (Map.Entry<String, Integer> en : sorted) {
            if (shown >= 3) break;
            topProductsListPanel.add(productRow(en.getKey(), en.getValue(), Math.max(max, 1)));
            topProductsListPanel.add(Box.createRigidArea(new Dimension(0, 14)));
            shown++;
        }
        if (shown == 0) {
            JLabel empty = new JLabel("No sales yet.");
            empty.setFont(new Font("SansSerif", Font.PLAIN, 12));
            empty.setForeground(TEXT_GRAY);
            topProductsListPanel.add(empty);
        }
        topProductsListPanel.revalidate();
        topProductsListPanel.repaint();
    }

    private JPanel productRow(String name, int value, int max) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);

        JLabel icon = new JLabel("\u2615");
        icon.setOpaque(true);
        icon.setBackground(new Color(250, 235, 220));
        icon.setHorizontalAlignment(SwingConstants.CENTER);
        icon.setPreferredSize(new Dimension(38, 38));

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        JLabel lbl = new JLabel(name);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        ProgressBar bar = new ProgressBar(value, max, ACCENT);
        bar.setPreferredSize(new Dimension(100, 8));
        center.add(lbl, BorderLayout.NORTH);
        center.add(bar, BorderLayout.SOUTH);

        JLabel val = new JLabel(String.valueOf(value));
        val.setFont(new Font("SansSerif", Font.BOLD, 13));

        row.add(icon, BorderLayout.WEST);
        row.add(center, BorderLayout.CENTER);
        row.add(val, BorderLayout.EAST);
        return row;
    }

    private RoundedPanel buildRecentCustomersCard() {
        RoundedPanel card = new RoundedPanel(16, CARD_BG);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(18, 18, 18, 18));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Recent Customers");
        title.setFont(new Font("SansSerif", Font.BOLD, 15));
        JButton viewAll = pillButton("View All");
        viewAll.addActionListener(e -> showAllCustomersWindow());
        header.add(title, BorderLayout.WEST);
        header.add(viewAll, BorderLayout.EAST);

        customersListPanel = new JPanel();
        customersListPanel.setOpaque(false);
        customersListPanel.setLayout(new BoxLayout(customersListPanel, BoxLayout.Y_AXIS));
        customersListPanel.setBorder(new EmptyBorder(15, 0, 0, 0));

        card.add(header, BorderLayout.NORTH);
        card.add(customersListPanel, BorderLayout.CENTER);

        rebuildRecentCustomers();
        return card;
    }

    /** Redraws the 3 most recent customer rows, recomputed fresh from every real order. */
    private void rebuildRecentCustomers() {
        if (customersListPanel == null) return;
        customersListPanel.removeAll();

        List<Order> sorted = new ArrayList<>(allOrders);
        sorted.sort((a, b) -> b.createdAt.compareTo(a.createdAt));

        Color[] palette = {
                new Color(120, 170, 220), new Color(90, 150, 200), new Color(220, 140, 160),
                new Color(180, 120, 200), new Color(140, 190, 140)
        };

        int shown = 0;
        for (Order o : sorted) {
            if (shown >= 3) break;
            Color c = palette[shown % palette.length];
            customersListPanel.add(customerRow(o.customer, String.format("$%.2f", o.total), o.time, c));
            customersListPanel.add(Box.createRigidArea(new Dimension(0, 12)));
            shown++;
        }
        if (shown == 0) {
            JLabel empty = new JLabel("No customers yet.");
            empty.setFont(new Font("SansSerif", Font.PLAIN, 12));
            empty.setForeground(TEXT_GRAY);
            customersListPanel.add(empty);
        } else {
            // drop the trailing spacer so there's no dangling gap at the bottom
            customersListPanel.remove(customersListPanel.getComponentCount() - 1);
        }
        customersListPanel.revalidate();
        customersListPanel.repaint();
    }

    private JPanel customerRow(String name, String amount, String time, Color avatarColor) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);

        Avatar avatar = new Avatar(name, avatarColor);
        avatar.setPreferredSize(new Dimension(38, 38));

        JLabel nameLbl = new JLabel(name);
        nameLbl.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        JLabel amt = new JLabel(amount);
        amt.setFont(new Font("SansSerif", Font.BOLD, 13));
        amt.setAlignmentX(Component.RIGHT_ALIGNMENT);
        JLabel tm = new JLabel(time);
        tm.setFont(new Font("SansSerif", Font.PLAIN, 11));
        tm.setForeground(TEXT_GRAY);
        tm.setAlignmentX(Component.RIGHT_ALIGNMENT);
        right.add(amt);
        right.add(tm);

        row.add(avatar, BorderLayout.WEST);
        row.add(nameLbl, BorderLayout.CENTER);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    private JButton pillButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        btn.setForeground(TEXT_DARK);
        btn.setBackground(Color.WHITE);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true), new EmptyBorder(6, 12, 6, 12)));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ---------------- DATA MODEL ----------------
    /** A single real order entered by Admin or Staff. Persisted to a plain .txt file so nothing is lost between runs. */
    static class Order {
        String orderId;
        String customer;
        String product;
        int qty;
        String status;
        double total;
        LocalDate date;
        LocalDateTime createdAt;
        String time;

        Order(String orderId, String customer, String product, int qty, String status,
              double total, LocalDate date, LocalDateTime createdAt) {
            this.orderId = orderId;
            this.customer = customer;
            this.product = product;
            this.qty = qty;
            this.status = status;
            this.total = total;
            this.date = date;
            this.createdAt = createdAt;
            this.time = createdAt.format(DateTimeFormatter.ofPattern("hh:mm a"));
        }
    }

    // ---------------- helper classes ----------------
    static class RoundedPanel extends JPanel {
        int radius; Color bg;
        RoundedPanel(int radius, Color bg) { this.radius = radius; this.bg = bg; setOpaque(false); }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class ProgressBar extends JComponent {
        int value, max; Color color;
        ProgressBar(int value, int max, Color color) { this.value = value; this.max = max; this.color = color; }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g2.setColor(new Color(240, 230, 220));
            g2.fillRoundRect(0, 0, w, h, h, h);
            int fillW = (int) (w * Math.min(1.0, (double) value / max));
            g2.setColor(color);
            g2.fillRoundRect(0, 0, fillW, h, h, h);
            g2.dispose();
        }
    }

    static class Avatar extends JComponent {
        String initials; Color color;
        Avatar(String name, Color color) {
            String[] parts = name.trim().split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (String p : parts) { if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0))); }
            this.initials = sb.length() > 2 ? sb.substring(0, 2) : sb.toString();
            this.color = color;
        }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(0, 0, getWidth(), getHeight());
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(initials)) / 2;
            int y = (getHeight() + fm.getAscent()) / 2 - 2;
            g2.drawString(initials, x, y);
            g2.dispose();
        }
    }

    static class ChartPanel extends JPanel {
        double[] values; String[] labels;
        ChartPanel(double[] values, String[] labels) { this.values = values; this.labels = labels; setOpaque(false); }

        void setData(double[] values, String[] labels) {
            this.values = values;
            this.labels = labels;
            repaint();
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int padLeft = 45, padBottom = 25, padTop = 10, padRight = 10;
            int chartW = w - padLeft - padRight, chartH = h - padTop - padBottom;

            double max = 0;
            for (double v : values) max = Math.max(max, v);
            double niceMax = Math.ceil(max / 500.0) * 500;
            if (niceMax == 0) niceMax = 500;

            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            for (int i = 0; i <= 5; i++) {
                int y = padTop + chartH - (int) (chartH * (i / 5.0));
                g2.setColor(new Color(230, 225, 218));
                g2.drawLine(padLeft, y, padLeft + chartW, y);
                String lbl = String.valueOf((int) (niceMax * i / 5));
                g2.setColor(new Color(150, 140, 130));
                g2.drawString(lbl, 5, y + 4);
            }

            int n = values.length;
            if (n < 2) { g2.dispose(); return; }
            int[] xs = new int[n], ys = new int[n];
            for (int i = 0; i < n; i++) {
                xs[i] = padLeft + (int) (chartW * (i / (double) (n - 1)));
                ys[i] = padTop + chartH - (int) (chartH * (values[i] / niceMax));
            }

            GeneralPath area = new GeneralPath();
            area.moveTo(xs[0], padTop + chartH);
            for (int i = 0; i < n; i++) area.lineTo(xs[i], ys[i]);
            area.lineTo(xs[n - 1], padTop + chartH);
            area.closePath();
            GradientPaint gp = new GradientPaint(0, padTop, new Color(200, 140, 90, 120), 0, padTop + chartH, new Color(200, 140, 90, 10));
            g2.setPaint(gp);
            g2.fill(area);

            g2.setColor(new Color(168, 100, 60));
            g2.setStroke(new BasicStroke(2.2f));
            for (int i = 0; i < n - 1; i++) g2.drawLine(xs[i], ys[i], xs[i + 1], ys[i + 1]);

            for (int i = 0; i < n; i++) {
                g2.setColor(new Color(168, 100, 60));
                g2.fillOval(xs[i] - 4, ys[i] - 4, 8, 8);
                g2.setColor(Color.WHITE);
                g2.fillOval(xs[i] - 2, ys[i] - 2, 4, 4);
            }

            g2.setColor(new Color(120, 110, 100));
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            for (int i = 0; i < n; i++) {
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(labels[i], xs[i] - fm.stringWidth(labels[i]) / 2, padTop + chartH + 18);
            }
            g2.dispose();
        }
    }

    static class StatusRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setOpaque(true);
            lbl.setBorder(new EmptyBorder(2, 8, 2, 8));
            String v = String.valueOf(value);
            if (v.equalsIgnoreCase("Paid")) {
                lbl.setBackground(GREEN_BG);
                lbl.setForeground(GREEN);
            } else {
                lbl.setBackground(ORANGE_BG);
                lbl.setForeground(ORANGE);
            }
            return lbl;
        }
    }

//    public static void main(String[] args) {
//        SwingUtilities.invokeLater(() -> new Dasboard().setVisible(true));
//    }
}