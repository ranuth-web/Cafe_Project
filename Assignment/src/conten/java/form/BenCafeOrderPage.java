package conten.java.form;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.*;
import java.util.List;

public class BenCafeOrderPage extends JFrame {
    // Data
    private List<Order> orders;
    private List<Order> filteredOrders;
    private int currentPage = 1;
    private int rowsPerPage = 6;
    private int totalPages = 1;

    // UI Components
    private JTable ordersTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> statusFilter;
    private JComboBox<String> dateFilter;
    private JComboBox<String> paymentFilter;
    private JComboBox<String> tableFilter;

    // Stats labels
    private JLabel totalOrdersLabel, pendingLabel, preparingLabel, completedLabel, cancelledLabel;
    private JLabel totalPercentLabel, pendingPercentLabel, preparingPercentLabel, completedPercentLabel, cancelledPercentLabel;

    // Pagination components
    private JLabel infoLabel;
    private JPanel pageButtonsPanel;

    // Color scheme
    private final Color ACCENT = new Color(245, 158, 11);
    private final Color BG = new Color(245, 247, 250);
    private final Color WHITE = Color.WHITE;

    // Icon base URL (icons8 free icons)
    private static final String ICON_BASE = "https://img.icons8.com/fluency-systems-regular/48/000000/";

    public BenCafeOrderPage() {
        setTitle("☕ Ben Cafe - Order Management");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        initializeOrders();
        buildUI();
        applyFilters();

        setVisible(true);
    }

    private void initializeOrders() {
        orders = new ArrayList<>();
        // ---- Existing 12 orders ----
        orders.add(new Order("#10068", "Emily Johnson", "T01", 3, 15.50, "Paid", "Completed", "May 25, 2026 09:30AM"));
        orders.add(new Order("#10067", "Michael Brown", "T04", 2, 9.00, "Cash", "Pending", "May 25, 2026 09:25AM"));
        orders.add(new Order("#10066", "Sarah Williams", "TakeAway", 5, 22.00, "Paid", "Preparing", "May 25, 2026 09:20AM"));
        orders.add(new Order("#10065", "David Wilson", "T02", 4, 18.75, "Card", "Completed", "May 25, 2026 09:15AM"));
        orders.add(new Order("#10064", "Jessica Miller", "T03", 1, 4.50, "Cash", "Pending", "May 25, 2026 09:10AM"));
        orders.add(new Order("#10063", "Daniel Taylor", "TakeAway", 2, 7.00, "Paid", "Cancelled", "May 25, 2026 09:05AM"));
        orders.add(new Order("#10062", "Chris Anderson", "T05", 3, 12.50, "Card", "Completed", "May 25, 2026 08:55AM"));
        orders.add(new Order("#10061", "Amanda White", "TakeAway", 1, 4.00, "Cash", "Preparing", "May 25, 2026 08:50AM"));
        orders.add(new Order("#10060", "James Miller", "T06", 4, 20.00, "Paid", "Pending", "May 25, 2026 08:45AM"));
        orders.add(new Order("#10059", "Patricia Davis", "T02", 2, 8.50, "Card", "Completed", "May 25, 2026 08:40AM"));
        orders.add(new Order("#10058", "Robert Garcia", "TakeAway", 3, 14.00, "Cash", "Completed", "May 25, 2026 08:35AM"));
        orders.add(new Order("#10057", "Jennifer Martinez", "T04", 2, 10.00, "Paid", "Preparing", "May 25, 2026 08:30AM"));

        // ---- Additional 20 orders for testing ----
        orders.add(new Order("#10056", "Thomas Lee", "T01", 3, 16.25, "Card", "Completed", "May 25, 2026 08:15AM"));
        orders.add(new Order("#10055", "Linda Clark", "T03", 2, 8.00, "Cash", "Pending", "May 25, 2026 08:10AM"));
        orders.add(new Order("#10054", "Kevin Lewis", "TakeAway", 4, 20.50, "Paid", "Preparing", "May 25, 2026 08:05AM"));
        orders.add(new Order("#10053", "Nancy Walker", "T05", 1, 3.75, "Card", "Completed", "May 25, 2026 08:00AM"));
        orders.add(new Order("#10052", "Gary Hall", "T02", 3, 14.00, "Cash", "Cancelled", "May 25, 2026 07:55AM"));
        orders.add(new Order("#10051", "Betty Allen", "T06", 2, 9.50, "Paid", "Completed", "May 25, 2026 07:50AM"));
        orders.add(new Order("#10050", "Frank Young", "TakeAway", 5, 25.00, "Card", "Pending", "May 25, 2026 07:45AM"));
        orders.add(new Order("#10049", "Helen King", "T04", 1, 4.00, "Cash", "Preparing", "May 25, 2026 07:40AM"));

        // Orders from yesterday (May 24)
        orders.add(new Order("#10048", "Carl Wright", "T03", 2, 8.50, "Paid", "Completed", "May 24, 2026 05:30PM"));
        orders.add(new Order("#10047", "Doris Scott", "TakeAway", 3, 12.00, "Card", "Completed", "May 24, 2026 05:15PM"));
        orders.add(new Order("#10046", "Edward Green", "T01", 4, 18.00, "Cash", "Pending", "May 24, 2026 05:00PM"));
        orders.add(new Order("#10045", "Ruth Adams", "T05", 2, 7.50, "Paid", "Cancelled", "May 24, 2026 04:45PM"));

        // Orders from May 23 and earlier
        orders.add(new Order("#10044", "Jose Baker", "T02", 3, 13.75, "Card", "Completed", "May 23, 2026 02:30PM"));
        orders.add(new Order("#10043", "Martha Carter", "TakeAway", 1, 3.00, "Cash", "Completed", "May 23, 2026 02:15PM"));
        orders.add(new Order("#10042", "Peter Mitchell", "T06", 4, 19.00, "Paid", "Preparing", "May 23, 2026 02:00PM"));
        orders.add(new Order("#10041", "Ann Perez", "T04", 2, 8.00, "Card", "Pending", "May 23, 2026 01:45PM"));
        orders.add(new Order("#10040", "Donald Roberts", "T03", 5, 21.50, "Cash", "Completed", "May 22, 2026 12:30PM"));
        orders.add(new Order("#10039", "Sandra Turner", "TakeAway", 2, 6.50, "Paid", "Completed", "May 22, 2026 12:15PM"));
        orders.add(new Order("#10038", "Anthony Phillips", "T01", 3, 11.00, "Card", "Cancelled", "May 22, 2026 12:00PM"));
        orders.add(new Order("#10037", "Carol Campbell", "T05", 1, 4.25, "Cash", "Completed", "May 21, 2026 11:30AM"));

        filteredOrders = new ArrayList<>(orders);
    }

    // Helper to load an ImageIcon from a URL and scale it
    private ImageIcon loadIcon(String fileName, int width, int height) {
        try {
            URL url = new URL(ICON_BASE + fileName);
            ImageIcon icon = new ImageIcon(url);
            Image img = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(img);
        } catch (Exception e) {
            System.err.println("Failed to load icon: " + fileName);
            // Fallback: create a colored placeholder
            return createFallbackIcon(width, height);
        }
    }

    private ImageIcon createFallbackIcon(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.GRAY);
        g.fillRect(0, 0, w, h);
        g.dispose();
        return new ImageIcon(img);
    }

    private void buildUI() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BG);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JPanel verticalContent = new JPanel();
        verticalContent.setLayout(new BoxLayout(verticalContent, BoxLayout.Y_AXIS));
        verticalContent.setBackground(BG);

        verticalContent.add(createHeader());
        verticalContent.add(Box.createRigidArea(new Dimension(0, 15)));
        verticalContent.add(createStatsPanel());
        verticalContent.add(Box.createRigidArea(new Dimension(0, 15)));
        verticalContent.add(createFilterPanel());
        verticalContent.add(Box.createRigidArea(new Dimension(0, 15)));
        verticalContent.add(createTableWrapper());

        mainPanel.add(verticalContent, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 240)),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        header.setPreferredSize(new Dimension(0, 70));

        // Left: title with small coffee icon
        JLabel title = new JLabel();
        title.setIcon(loadIcon("coffee-to-go.png", 28, 28));
        title.setText(" Order Management");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(new Color(50, 50, 70));
        title.setIconTextGap(10);
        header.add(title, BorderLayout.WEST);

        // Right panel: search + actions
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        rightPanel.setBackground(WHITE);

        // Search field - FIX: removed leading space in placeholder
        searchField = new JTextField("Search orders...", 20);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setForeground(new Color(160, 160, 180));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 210), 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        searchField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (searchField.getText().equals("Search orders...")) {
                    searchField.setText("");
                    searchField.setForeground(Color.BLACK);
                }
            }
            public void focusLost(FocusEvent e) {
                if (searchField.getText().isEmpty()) {
                    searchField.setText("Search orders...");
                    searchField.setForeground(new Color(160, 160, 180));
                }
            }
        });
        searchField.addActionListener(e -> applyFilters());

        JButton searchBtn = new JButton(loadIcon("search.png", 20, 20));
        searchBtn.setBackground(WHITE);
        searchBtn.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        searchBtn.setFocusPainted(false);
        searchBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        searchBtn.addActionListener(e -> applyFilters());

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        searchPanel.setBackground(WHITE);
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);

        // Action buttons with icons
        JButton newOrderBtn = new JButton(" New Order");
        newOrderBtn.setIcon(loadIcon("add--v1.png", 20, 20));
        newOrderBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        newOrderBtn.setBackground(ACCENT);
        newOrderBtn.setForeground(WHITE);
        newOrderBtn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        newOrderBtn.setFocusPainted(false);
        newOrderBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        newOrderBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "Opening New Order form..."));

        JButton exportBtn = new JButton(" Export");
        exportBtn.setIcon(loadIcon("export.png", 20, 20));
        exportBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        exportBtn.setBackground(new Color(240, 240, 245));
        exportBtn.setForeground(new Color(80, 80, 100));
        exportBtn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        exportBtn.setFocusPainted(false);
        exportBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        exportBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "Exporting orders data..."));

        JButton printBtn = new JButton(" Print");
        printBtn.setIcon(loadIcon("print.png", 20, 20));
        printBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        printBtn.setBackground(new Color(240, 240, 245));
        printBtn.setForeground(new Color(80, 80, 100));
        printBtn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        printBtn.setFocusPainted(false);
        printBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        printBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "Printing orders..."));

        rightPanel.add(searchPanel);
        rightPanel.add(newOrderBtn);
        rightPanel.add(exportBtn);
        rightPanel.add(printBtn);

        header.add(rightPanel, BorderLayout.EAST);
        return header;
    }

    private JPanel createStatsPanel() {
        JPanel stats = new JPanel(new GridLayout(1, 5, 15, 0));
        stats.setBackground(BG);

        totalOrdersLabel = new JLabel("0");
        pendingLabel = new JLabel("0");
        preparingLabel = new JLabel("0");
        completedLabel = new JLabel("0");
        cancelledLabel = new JLabel("0");

        totalPercentLabel = new JLabel("0%");
        pendingPercentLabel = new JLabel("0%");
        preparingPercentLabel = new JLabel("0%");
        completedPercentLabel = new JLabel("0%");
        cancelledPercentLabel = new JLabel("0%");

        stats.add(createStatCard("Total Orders", totalOrdersLabel, totalPercentLabel, new Color(99, 102, 241), "statistics.png"));
        stats.add(createStatCard("Pending", pendingLabel, pendingPercentLabel, new Color(251, 191, 36), "clock.png"));
        stats.add(createStatCard("Preparing", preparingLabel, preparingPercentLabel, new Color(59, 130, 246), "in-progress.png"));
        stats.add(createStatCard("Completed", completedLabel, completedPercentLabel, new Color(34, 197, 94), "checkmark.png"));
        stats.add(createStatCard("Cancelled", cancelledLabel, cancelledPercentLabel, new Color(239, 68, 68), "cancel.png"));

        return stats;
    }

    private JPanel createStatCard(String title, JLabel valueLabel, JLabel percentLabel, Color color, String iconName) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 240), 1),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(WHITE);
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        titleLbl.setForeground(new Color(130, 130, 150));
        // Add icon to the left of title
        JLabel iconLbl = new JLabel(loadIcon(iconName, 20, 20));
        top.add(iconLbl, BorderLayout.WEST);
        top.add(titleLbl, BorderLayout.CENTER);
        card.add(top, BorderLayout.NORTH);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueLabel.setForeground(color);
        percentLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        percentLabel.setForeground(new Color(160, 160, 180));

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bottom.setBackground(WHITE);
        bottom.add(valueLabel);
        bottom.add(Box.createHorizontalStrut(10));
        bottom.add(percentLabel);
        card.add(bottom, BorderLayout.SOUTH);

        return card;
    }

    private void updateStats() {
        int total = filteredOrders.size();
        int pending = 0, preparing = 0, completed = 0, cancelled = 0;
        for (Order o : filteredOrders) {
            switch (o.status) {
                case "Pending": pending++; break;
                case "Preparing": preparing++; break;
                case "Completed": completed++; break;
                case "Cancelled": cancelled++; break;
            }
        }

        totalOrdersLabel.setText(String.valueOf(total));
        pendingLabel.setText(String.valueOf(pending));
        preparingLabel.setText(String.valueOf(preparing));
        completedLabel.setText(String.valueOf(completed));
        cancelledLabel.setText(String.valueOf(cancelled));

        totalPercentLabel.setText("100%");
        pendingPercentLabel.setText(total > 0 ? String.format("%.1f%%", pending * 100.0 / total) : "0%");
        preparingPercentLabel.setText(total > 0 ? String.format("%.1f%%", preparing * 100.0 / total) : "0%");
        completedPercentLabel.setText(total > 0 ? String.format("%.1f%%", completed * 100.0 / total) : "0%");
        cancelledPercentLabel.setText(total > 0 ? String.format("%.1f%%", cancelled * 100.0 / total) : "0%");
    }

    private JPanel createFilterPanel() {
        JPanel filterPanel = new JPanel(new BorderLayout());
        filterPanel.setBackground(WHITE);
        filterPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(230, 230, 240)),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        left.setBackground(WHITE);

        // Orders label with icon
        JLabel ordersLabel = new JLabel(" Orders");
        ordersLabel.setIcon(loadIcon("order-history.png", 20, 20));
        ordersLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        ordersLabel.setForeground(new Color(50, 50, 70));
        ordersLabel.setIconTextGap(8);
        left.add(ordersLabel);

        // Filter dropdowns with icons
        left.add(createFilterLabel("Status", "status-available.png"));
        statusFilter = new JComboBox<>(new String[]{"All Status", "Pending", "Preparing", "Completed", "Cancelled"});
        styleComboBox(statusFilter);
        statusFilter.addActionListener(e -> applyFilters());
        left.add(statusFilter);

        left.add(createFilterLabel("Date", "calendar.png"));
        dateFilter = new JComboBox<>(new String[]{"Today", "Yesterday", "This Week", "This Month"});
        styleComboBox(dateFilter);
        dateFilter.addActionListener(e -> applyFilters());
        left.add(dateFilter);

        left.add(createFilterLabel("Payment", "money.png"));
        paymentFilter = new JComboBox<>(new String[]{"All Payment", "Paid", "Cash", "Card"});
        styleComboBox(paymentFilter);
        paymentFilter.addActionListener(e -> applyFilters());
        left.add(paymentFilter);

        left.add(createFilterLabel("Table", "table.png"));
        tableFilter = new JComboBox<>(new String[]{"All Tables", "T01", "T02", "T03", "T04", "T05", "T06", "TakeAway"});
        styleComboBox(tableFilter);
        tableFilter.addActionListener(e -> applyFilters());
        left.add(tableFilter);

        // Reset button with icon
        JButton resetBtn = new JButton(" Reset");
        resetBtn.setIcon(loadIcon("reset.png", 18, 18));
        resetBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        resetBtn.setBackground(new Color(240, 240, 245));
        resetBtn.setForeground(new Color(80, 80, 100));
        resetBtn.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        resetBtn.setFocusPainted(false);
        resetBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        resetBtn.addActionListener(e -> {
            statusFilter.setSelectedIndex(0);
            dateFilter.setSelectedIndex(0);
            paymentFilter.setSelectedIndex(0);
            tableFilter.setSelectedIndex(0);
            searchField.setText("Search orders...");
            searchField.setForeground(new Color(160, 160, 180));
            applyFilters();
        });
        left.add(resetBtn);

        filterPanel.add(left, BorderLayout.WEST);
        return filterPanel;
    }

    private JLabel createFilterLabel(String text, String iconName) {
        JLabel label = new JLabel(" " + text);
        label.setIcon(loadIcon(iconName, 16, 16));
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(new Color(130, 130, 150));
        label.setIconTextGap(4);
        return label;
    }

    private void styleComboBox(JComboBox<?> combo) {
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        combo.setBackground(WHITE);
        combo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 210), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        combo.setPreferredSize(new Dimension(120, 30));
    }

    private JPanel createTableWrapper() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(WHITE);
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 240), 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        // Table
        String[] columns = {"OrderID", "Customer", "Table/Type", "Items", "Total", "Payment", "Status", "Date & Time", "Actions"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 8;
            }
        };

        ordersTable = new JTable(tableModel);
        ordersTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        ordersTable.setRowHeight(50);
        ordersTable.setShowGrid(false);
        ordersTable.setIntercellSpacing(new Dimension(10, 5));
        ordersTable.setSelectionBackground(new Color(245, 158, 11, 30));

        // Column widths
        ordersTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        ordersTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        ordersTable.getColumnModel().getColumn(2).setPreferredWidth(90);
        ordersTable.getColumnModel().getColumn(3).setPreferredWidth(60);
        ordersTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        ordersTable.getColumnModel().getColumn(5).setPreferredWidth(80);
        ordersTable.getColumnModel().getColumn(6).setPreferredWidth(100);
        ordersTable.getColumnModel().getColumn(7).setPreferredWidth(150);
        ordersTable.getColumnModel().getColumn(8).setPreferredWidth(160);

        // Custom renderers
        ordersTable.getColumnModel().getColumn(6).setCellRenderer(new StatusCellRenderer());
        ordersTable.getColumnModel().getColumn(5).setCellRenderer(new PaymentCellRenderer());
        ordersTable.getColumnModel().getColumn(8).setCellRenderer(new ButtonRenderer());
        ordersTable.getColumnModel().getColumn(8).setCellEditor(new ButtonEditor(new JCheckBox()));

        JScrollPane scroll = new JScrollPane(ordersTable);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(WHITE);
        wrapper.add(scroll, BorderLayout.CENTER);

        wrapper.add(createPaginationPanel(), BorderLayout.SOUTH);

        return wrapper;
    }

    private JPanel createPaginationPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 0, 5, 0));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setBackground(WHITE);
        infoLabel = new JLabel("Showing 0 to 0 of 0 orders");
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        infoLabel.setForeground(new Color(130, 130, 150));
        left.add(infoLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        right.setBackground(WHITE);

        JButton prevBtn = createPaginationButton("Previous");
        prevBtn.setIcon(loadIcon("back.png", 16, 16));
        prevBtn.setHorizontalTextPosition(SwingConstants.RIGHT);
        prevBtn.addActionListener(e -> {
            if (currentPage > 1) {
                currentPage--;
                applyFilters();
            }
        });

        JButton nextBtn = createPaginationButton("Next");
        nextBtn.setIcon(loadIcon("forward.png", 16, 16));
        nextBtn.setHorizontalTextPosition(SwingConstants.LEFT);
        nextBtn.addActionListener(e -> {
            if (currentPage < totalPages) {
                currentPage++;
                applyFilters();
            }
        });

        pageButtonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
        pageButtonsPanel.setBackground(WHITE);

        right.add(prevBtn);
        right.add(pageButtonsPanel);
        right.add(nextBtn);

        panel.add(left, BorderLayout.WEST);
        panel.add(right, BorderLayout.EAST);
        return panel;
    }

    private JButton createPaginationButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setBackground(WHITE);
        btn.setForeground(new Color(80, 80, 100));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 210), 1),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void applyFilters() {
        String searchText = searchField.getText().trim().toLowerCase();
        // FIX: check against "search orders..." (without leading space)
        if (searchText.equals("search orders...") || searchText.isEmpty()) {
            searchText = "";
        }

        String status = (String) statusFilter.getSelectedItem();
        String date = (String) dateFilter.getSelectedItem();
        String payment = (String) paymentFilter.getSelectedItem();
        String table = (String) tableFilter.getSelectedItem();

        filteredOrders.clear();
        for (Order order : orders) {
            boolean match = true;

            if (!searchText.isEmpty()) {
                if (!order.id.toLowerCase().contains(searchText) &&
                        !order.customer.toLowerCase().contains(searchText)) {
                    match = false;
                }
            }

            if (match && !status.equals("All Status") && !order.status.equals(status)) match = false;
            if (match && !payment.equals("All Payment") && !order.payment.equals(payment)) match = false;
            if (match && !table.equals("All Tables") && !order.tableType.equals(table)) match = false;

            // Simplified date filtering (demo)
            if (match && !date.equals("Today")) {
                if (date.equals("Yesterday") && !order.dateTime.contains("May 24")) match = false;
                if (date.equals("This Week") && !order.dateTime.contains("May")) match = false;
                if (date.equals("This Month") && !order.dateTime.contains("May")) match = false;
            }

            if (match) filteredOrders.add(order);
        }

        totalPages = (int) Math.ceil(filteredOrders.size() / (double) rowsPerPage);
        if (totalPages == 0) totalPages = 1;
        if (currentPage > totalPages) currentPage = totalPages;

        updateTable();
        updateStats();
        updatePagination();
    }

    private void updateTable() {
        tableModel.setRowCount(0);
        int start = (currentPage - 1) * rowsPerPage;
        int end = Math.min(start + rowsPerPage, filteredOrders.size());

        for (int i = start; i < end; i++) {
            Order o = filteredOrders.get(i);
            Object[] row = {
                    o.id,
                    o.customer,
                    o.tableType,
                    o.items,
                    "$" + String.format("%.2f", o.total),
                    o.payment,
                    o.status,
                    o.dateTime,
                    "Actions"
            };
            tableModel.addRow(row);
        }
    }

    private void updatePagination() {
        int start = (currentPage - 1) * rowsPerPage + 1;
        int end = Math.min(currentPage * rowsPerPage, filteredOrders.size());
        infoLabel.setText("Showing " + start + " to " + end + " of " + filteredOrders.size() + " orders");

        pageButtonsPanel.removeAll();
        int startPage = Math.max(1, currentPage - 2);
        int endPage = Math.min(totalPages, currentPage + 2);

        for (int i = startPage; i <= endPage; i++) {
            JButton pageBtn = new JButton(String.valueOf(i));
            pageBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            pageBtn.setBackground(i == currentPage ? ACCENT : WHITE);
            pageBtn.setForeground(i == currentPage ? WHITE : new Color(80, 80, 100));
            pageBtn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(i == currentPage ? ACCENT : new Color(200, 200, 210), 1),
                    BorderFactory.createEmptyBorder(5, 12, 5, 12)
            ));
            pageBtn.setFocusPainted(false);
            pageBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            int page = i;
            pageBtn.addActionListener(e -> {
                currentPage = page;
                applyFilters();
            });
            pageButtonsPanel.add(pageBtn);
        }
        pageButtonsPanel.revalidate();
        pageButtonsPanel.repaint();
    }

    // ---------- Inner Classes ----------

    private static class Order {
        String id, customer, tableType, payment, status, dateTime;
        int items;
        double total;

        Order(String id, String customer, String tableType, int items, double total,
              String payment, String status, String dateTime) {
            this.id = id;
            this.customer = customer;
            this.tableType = tableType;
            this.items = items;
            this.total = total;
            this.payment = payment;
            this.status = status;
            this.dateTime = dateTime;
        }
    }

    private class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String status = (String) value;
            JLabel label = (JLabel) c;
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(new Font("Segoe UI", Font.BOLD, 12));

            Color bg, fg;
            switch (status) {
                case "Pending": bg = new Color(251, 191, 36, 40); fg = new Color(180, 120, 20); break;
                case "Preparing": bg = new Color(59, 130, 246, 40); fg = new Color(30, 80, 200); break;
                case "Completed": bg = new Color(34, 197, 94, 40); fg = new Color(20, 150, 60); break;
                case "Cancelled": bg = new Color(239, 68, 68, 40); fg = new Color(190, 40, 40); break;
                default: bg = WHITE; fg = Color.BLACK;
            }
            label.setBackground(bg);
            label.setForeground(fg);
            label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(bg.darker(), 1),
                    BorderFactory.createEmptyBorder(4, 12, 4, 12)
            ));
            return label;
        }
    }

    private class PaymentCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String payment = (String) value;
            JLabel label = (JLabel) c;
            label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            // Add small icons for payment methods
            if ("Paid".equals(payment)) {
                label.setForeground(new Color(34, 197, 94));
                label.setText("✅ " + payment);
            } else if ("Cash".equals(payment)) {
                label.setForeground(new Color(59, 130, 246));
                label.setText("💵 " + payment);
            } else if ("Card".equals(payment)) {
                label.setForeground(new Color(139, 92, 246));
                label.setText("💳 " + payment);
            }
            return label;
        }
    }

    private class ButtonRenderer extends JPanel implements TableCellRenderer {
        private JButton viewBtn, editBtn;

        ButtonRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 3, 5));
            setBackground(WHITE);
            viewBtn = new JButton("View");
            viewBtn.setIcon(loadIcon("view.png", 16, 16));
            viewBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            viewBtn.setBackground(new Color(240, 240, 245));
            viewBtn.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
            viewBtn.setFocusPainted(false);
            viewBtn.setToolTipText("View order details");
            editBtn = new JButton("Edit");
            editBtn.setIcon(loadIcon("edit.png", 16, 16));
            editBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            editBtn.setBackground(new Color(240, 240, 245));
            editBtn.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
            editBtn.setFocusPainted(false);
            editBtn.setToolTipText("Edit order");
            add(viewBtn);
            add(editBtn);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            return this;
        }
    }

    private class ButtonEditor extends DefaultCellEditor {
        private JPanel panel;
        private JButton viewBtn, editBtn;
        private int currentRow;

        ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 5));
            panel.setBackground(WHITE);

            viewBtn = new JButton("View");
            viewBtn.setIcon(loadIcon("view.png", 16, 16));
            viewBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            viewBtn.setBackground(new Color(240, 240, 245));
            viewBtn.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
            viewBtn.setFocusPainted(false);
            viewBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            viewBtn.addActionListener(e -> {
                String orderId = (String) tableModel.getValueAt(currentRow, 0);
                JOptionPane.showMessageDialog(BenCafeOrderPage.this, "Viewing order: " + orderId);
                fireEditingStopped();
            });

            editBtn = new JButton("Edit");
            editBtn.setIcon(loadIcon("edit.png", 16, 16));
            editBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            editBtn.setBackground(new Color(240, 240, 245));
            editBtn.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
            editBtn.setFocusPainted(false);
            editBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            editBtn.addActionListener(e -> {
                String orderId = (String) tableModel.getValueAt(currentRow, 0);
                JOptionPane.showMessageDialog(BenCafeOrderPage.this, "Editing order: " + orderId);
                fireEditingStopped();
            });

            panel.add(viewBtn);
            panel.add(editBtn);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            currentRow = row;
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return "Actions";
        }
    }

    // ---------- Main ----------
   
    
}