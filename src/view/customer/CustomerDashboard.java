package view.customer;

/** Customer home. Layout is paired with CustomerDashboard.form for NetBeans Design. */
public class CustomerDashboard extends javax.swing.JFrame {

    private final controller.CustomerDashboardController controller;
    private final int customerId;
    private model.DashboardData data = new model.DashboardData(java.util.List.of(), java.util.List.of());
    private final javax.swing.table.DefaultTableModel tableModel = new javax.swing.table.DefaultTableModel(
            new String[]{"Order No.", "Service", "Quantity", "Total", "Status", "Date"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };

    public CustomerDashboard() {
        this(requireCustomer(), new dao.CustomerDashboardDAOImpl());
    }

    /** Dependency injection supports offline rendering and controlled DAO verification. */
    public CustomerDashboard(int customerId, dao.CustomerDashboardDAO dao) {
        if (customerId <= 0) throw new IllegalArgumentException("Please log in as a customer first.");
        this.customerId = customerId;
        this.controller = new controller.CustomerDashboardController(dao);
        initComponents();
        configure();
        refreshData();
    }

    private static int requireCustomer() {
        laundry.User user = laundry.Session.getCurrentUser();
        if (user == null || user.isStaff()) throw new IllegalStateException("Please log in as a customer first.");
        return user.getId();
    }

    private void configure() {
        setIconImage(new javax.swing.ImageIcon(getClass().getResource("/resources/images/logo.png")).getImage());
        javax.swing.JButton[] nav = {homeButton, placeOrderButton, trackButton, paymentButton, historyButton, aboutButton, profileButton, logoutButton};
        String[] icons = {"home", "order", "track", "payment", "history", "about", "profile", "logout"};
        for (int i = 0; i < nav.length; i++) {
            nav[i].setIcon(new components.BrandIcon(icons[i], i == 0 ? java.awt.Color.WHITE : components.Theme.DARK_GREEN));
            nav[i].setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
            nav[i].getAccessibleContext().setAccessibleDescription(nav[i].getText());
        }
        homeButton.getAccessibleContext().setAccessibleDescription("Home, current page");
        refreshButton.setIcon(new components.BrandIcon("refresh", components.Theme.DARK_GREEN));
        thanksLabel.setIcon(new components.BrandIcon("heart", java.awt.Color.WHITE));
        ordersTable.setModel(tableModel);
        ordersTable.setRowHeight(36);
        ordersTable.setFont(components.Theme.font(12, false));
        ordersTable.setBackground(java.awt.Color.WHITE);
        ordersTable.setForeground(components.Theme.TEXT);
        ordersTable.setShowGrid(false);
        ordersTable.setIntercellSpacing(new java.awt.Dimension(0, 0));
        ordersTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        ordersTable.getTableHeader().setFont(components.Theme.font(12, true));
        ordersTable.getTableHeader().setReorderingAllowed(false);
        ordersTable.getColumnModel().getColumn(4).setCellRenderer(new components.StatusBadgeRenderer());
        int[] widths = {66, 142, 68, 78, 145, 106};
        for (int i = 0; i < widths.length; i++) ordersTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        ordersScroll.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        ordersScroll.getViewport().setBackground(java.awt.Color.WHITE);
        ordersTable.setFillsViewportHeight(true);
        ordersTable.getAccessibleContext().setAccessibleName("Your five most recent laundry orders");
        progress.getAccessibleContext().setAccessibleName("No order selected");
        javax.swing.KeyStroke refreshKey = javax.swing.KeyStroke.getKeyStroke("F5");
        mainPanel.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(refreshKey, "refresh");
        mainPanel.getActionMap().put("refresh", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { refreshData(); }
        });
    }

    private void refreshData() {
        refreshButton.setEnabled(false);
        setOrderButtonsEnabled(false);
        loadStatus.setForeground(components.Theme.MUTED);
        loadStatus.setText("Loading your services and orders...");
        controller.load(customerId, this::display, message -> {
            refreshButton.setEnabled(true);
            loadStatus.setForeground(new java.awt.Color(0xA12622));
            loadStatus.setText(message);
        });
    }

    private void setOrderButtonsEnabled(boolean value) {
        for (javax.swing.JButton button : new javax.swing.JButton[]{orderButton0, orderButton1, orderButton2})
            button.setEnabled(value);
    }

    private void display(model.DashboardData loaded) {
        data = loaded;
        javax.swing.JLabel[] names = {serviceName0, serviceName1, serviceName2};
        javax.swing.JLabel[] prices = {servicePrice0, servicePrice1, servicePrice2};
        javax.swing.JLabel[] images = {serviceImage0, serviceImage1, serviceImage2};
        javax.swing.JButton[] buttons = {orderButton0, orderButton1, orderButton2};
        components.RoundedPanel[] cards = {serviceCard0, serviceCard1, serviceCard2};
        for (int i = 0; i < cards.length; i++) {
            boolean exists = i < data.services().size();
            cards[i].setVisible(exists);
            if (!exists) continue;
            model.DashboardData.ServiceCard service = data.services().get(i);
            names[i].setText(service.name());
            names[i].setToolTipText(service.description());
            prices[i].setText(util.CurrencyFormat.peso(service.price()) + " / kg");
            String image = switch (service.code()) {
                case "DRY_CLEAN" -> "dry-cleaning.png";
                case "IRONING", "FOLDING" -> "folding.png";
                default -> "washing.png";
            };
            images[i].setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/images/" + image)));
            images[i].getAccessibleContext().setAccessibleName(service.name());
            buttons[i].setEnabled(service.available());
            buttons[i].setText(service.available() ? "Order Now  >" : "Unavailable");
        }
        tableModel.setRowCount(0);
        for (model.DashboardData.RecentOrder order : data.orders()) {
            tableModel.addRow(new Object[]{order.number(), order.service(), order.quantity().stripTrailingZeros().toPlainString() + " kg",
                util.CurrencyFormat.peso(order.total()), order.displayStatus(), order.placedAt().format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy"))});
        }
        progress.setTimestamps(new String[6]);
        completionTitle.setText("Estimated Completion");
        if (data.orders().isEmpty()) {
            progress.setCurrentStep(-1);
            trackingOrder.setText("Your next fresh start begins here");
            completionValue.setText("Place an order to get started");
        } else {
            model.DashboardData.RecentOrder order = data.orders().get(0);
            trackingOrder.setText("Order " + order.number() + "  |  " + order.service());
            trackingOrder.setToolTipText(trackingOrder.getText());
            progress.setCurrentStep(order.step());
            String[] times = new String[6];
            java.time.format.DateTimeFormatter format = java.time.format.DateTimeFormatter.ofPattern("MMM d, h:mm a");
            times[0] = order.placedAt().format(format);
            String[] legacy = {"Received", "Washing", "Drying", "Ready for Pickup", "Claimed"};
            for (int i = 0; i < legacy.length; i++) {
                java.time.LocalDateTime time = order.history().get(legacy[i]);
                if (time == null && i == 4) time = order.history().get("Completed");
                if (time != null && i + 1 <= order.step()) times[i + 1] = time.format(format);
            }
            progress.setTimestamps(times);
            completionTitle.setText(order.step() == 5 ? "Laundry completed" : "Estimated Completion");
            completionValue.setText(order.step() == 5 ? "Thank you. See you again!"
                    : order.expectedCompletion().format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")));
        }
        refreshButton.setEnabled(true);
        loadStatus.setForeground(components.Theme.MUTED);
        loadStatus.setText(data.orders().isEmpty() ? "No orders yet. Choose a service to place your first order."
                : "Showing your latest " + data.orders().size() + " orders. Refresh or press F5 for the latest status.");
        if (data.services().isEmpty()) loadStatus.setText("No services are listed. Please contact Metric Laundry.");
    }

    private void navigate(String destination) {
        // Existing screens remain reachable until their individual redesigns are approved.
        switch (destination) {
            case "home" -> refreshData();
            case "place" -> laundry.Nav.go(this, laundry.Nav.SERVICES);
            case "track", "history" -> laundry.Nav.go(this, laundry.Nav.ORDERS);
            case "profile" -> laundry.Nav.go(this, laundry.Nav.PROFILE);
            case "payment" -> {
                laundry.Ui.info(this, "Payment is selected when placing an order. Your payment status and receipt are available under My Orders.");
                laundry.Nav.go(this, laundry.Nav.ORDERS);
            }
            case "about" -> laundry.Ui.info(this, "Metric Laundry\nClean Clothes, Happy You!\n\n"
                    + "Careful laundry service for your everyday essentials.\n"
                    + "Kalawag 1, back of Water District\n8:00 AM - 5:00 PM (Daily)");
            case "logout" -> laundry.Nav.logout(this);
            default -> {
                if (!destination.startsWith("order")) throw new IllegalArgumentException("Unknown destination");
                int index = Integer.parseInt(destination.substring(5));
                if (index >= data.services().size()) return;
                model.DashboardData.ServiceCard service = data.services().get(index);
                if (!service.available()) { laundry.Ui.warn(this, "This service is currently unavailable."); return; }
                laundry.Session.setDraftService(service.code());
                laundry.Nav.open(this, new laundry.WeightForm());
            }
        }
    }

    @Override public void dispose() { controller.cancel(); super.dispose(); }

    /** Run this form through the login screen; it never auto-signs in a sample account. */
    public static void main(String[] args) { laundry.LoginForm.main(args); }


    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Metric Laundry - Customer Dashboard");
        setResizable(false);
        mainPanel = new javax.swing.JPanel();
        mainPanel.setLayout(null);
        mainPanel.setBackground(new java.awt.Color(0xF5F8F5));
        sidebar = new javax.swing.JPanel();
        sidebar.setLayout(null);
        sidebar.setBackground(new java.awt.Color(0xFFFFFF));
        logo = new javax.swing.JLabel();
        logo.setForeground(new java.awt.Color(0x1B1B1B));
        logo.setFont(new java.awt.Font("Segoe UI", 0, 13));
        logo.setText("");
        logo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/images/logo.png")));
        brand = new javax.swing.JLabel();
        brand.setForeground(new java.awt.Color(0x145A32));
        brand.setFont(new java.awt.Font("Segoe UI", 1, 17));
        brand.setText("METRIC LAUNDRY");
        brand.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        brandTagline = new javax.swing.JLabel();
        brandTagline.setForeground(new java.awt.Color(0x6B6B6B));
        brandTagline.setFont(new java.awt.Font("Segoe UI", 0, 11));
        brandTagline.setText("A little care. A lot of fresh.");
        brandTagline.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        homeButton = new components.RoundedButton();
        homeButton.setBackground(new java.awt.Color(0x1E8E3E));
        homeButton.setForeground(new java.awt.Color(0xFFFFFF));
        homeButton.setFont(new java.awt.Font("Segoe UI", 1, 13));
        homeButton.setText("Home");
        homeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                homeButtonActionPerformed(evt);
            }
        });
        placeOrderButton = new components.RoundedButton();
        placeOrderButton.setBackground(new java.awt.Color(0xFFFFFF));
        placeOrderButton.setForeground(new java.awt.Color(0x145A32));
        placeOrderButton.setFont(new java.awt.Font("Segoe UI", 1, 13));
        placeOrderButton.setText("Place Order");
        placeOrderButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                placeOrderButtonActionPerformed(evt);
            }
        });
        trackButton = new components.RoundedButton();
        trackButton.setBackground(new java.awt.Color(0xFFFFFF));
        trackButton.setForeground(new java.awt.Color(0x145A32));
        trackButton.setFont(new java.awt.Font("Segoe UI", 1, 13));
        trackButton.setText("Track Order");
        trackButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                trackButtonActionPerformed(evt);
            }
        });
        paymentButton = new components.RoundedButton();
        paymentButton.setBackground(new java.awt.Color(0xFFFFFF));
        paymentButton.setForeground(new java.awt.Color(0x145A32));
        paymentButton.setFont(new java.awt.Font("Segoe UI", 1, 13));
        paymentButton.setText("Payment");
        paymentButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                paymentButtonActionPerformed(evt);
            }
        });
        historyButton = new components.RoundedButton();
        historyButton.setBackground(new java.awt.Color(0xFFFFFF));
        historyButton.setForeground(new java.awt.Color(0x145A32));
        historyButton.setFont(new java.awt.Font("Segoe UI", 1, 13));
        historyButton.setText("Order History");
        historyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                historyButtonActionPerformed(evt);
            }
        });
        aboutButton = new components.RoundedButton();
        aboutButton.setBackground(new java.awt.Color(0xFFFFFF));
        aboutButton.setForeground(new java.awt.Color(0x145A32));
        aboutButton.setFont(new java.awt.Font("Segoe UI", 1, 13));
        aboutButton.setText("About Us");
        aboutButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutButtonActionPerformed(evt);
            }
        });
        profileButton = new components.RoundedButton();
        profileButton.setBackground(new java.awt.Color(0xFFFFFF));
        profileButton.setForeground(new java.awt.Color(0x145A32));
        profileButton.setFont(new java.awt.Font("Segoe UI", 1, 13));
        profileButton.setText("My Profile");
        profileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                profileButtonActionPerformed(evt);
            }
        });
        logoutButton = new components.RoundedButton();
        logoutButton.setBackground(new java.awt.Color(0xFFFFFF));
        logoutButton.setForeground(new java.awt.Color(0x145A32));
        logoutButton.setFont(new java.awt.Font("Segoe UI", 1, 13));
        logoutButton.setText("Log out");
        logoutButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                logoutButtonActionPerformed(evt);
            }
        });
        sidebarLeaves = new javax.swing.JLabel();
        sidebarLeaves.setForeground(new java.awt.Color(0x1B1B1B));
        sidebarLeaves.setFont(new java.awt.Font("Segoe UI", 0, 13));
        sidebarLeaves.setText("");
        sidebarLeaves.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/images/leaves.png")));
        welcomeBanner = new components.RoundedPanel();
        welcomeBanner.setLayout(null);
        welcomeBanner.setBackground(new java.awt.Color(0xE8F5E9));
        welcomeBanner.setGradientEnd(new java.awt.Color(0xC8E6C9));
        eyebrow = new javax.swing.JLabel();
        eyebrow.setForeground(new java.awt.Color(0x145A32));
        eyebrow.setFont(new java.awt.Font("Segoe UI", 1, 11));
        eyebrow.setText("FRESH LAUNDRY, LESS EFFORT");
        welcomeTitle = new javax.swing.JLabel();
        welcomeTitle.setForeground(new java.awt.Color(0x145A32));
        welcomeTitle.setFont(new java.awt.Font("Segoe UI", 1, 30));
        welcomeTitle.setText("Welcome to Metric Laundry!");
        welcomeTagline = new javax.swing.JLabel();
        welcomeTagline.setForeground(new java.awt.Color(0x145A32));
        welcomeTagline.setFont(new java.awt.Font("Segoe UI", 0, 16));
        welcomeTagline.setText("Clean Clothes, Happy You!");
        basketImage = new javax.swing.JLabel();
        basketImage.setForeground(new java.awt.Color(0x1B1B1B));
        basketImage.setFont(new java.awt.Font("Segoe UI", 0, 13));
        basketImage.setText("");
        basketImage.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/images/basket.png")));
        servicesPanel = new components.RoundedPanel();
        servicesPanel.setLayout(null);
        servicesTitle = new javax.swing.JLabel();
        servicesTitle.setForeground(new java.awt.Color(0x1B1B1B));
        servicesTitle.setFont(new java.awt.Font("Segoe UI", 1, 18));
        servicesTitle.setText("Our Services");
        viewServicesButton = new components.RoundedButton();
        viewServicesButton.setBackground(new java.awt.Color(0xFFFFFF));
        viewServicesButton.setForeground(new java.awt.Color(0x145A32));
        viewServicesButton.setFont(new java.awt.Font("Segoe UI", 1, 13));
        viewServicesButton.setText("View All \u2192");
        viewServicesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewServicesButtonActionPerformed(evt);
            }
        });
        serviceCard0 = new components.RoundedPanel();
        serviceCard0.setLayout(null);
        serviceCard0.setBackground(new java.awt.Color(0xF4F9F4));
        serviceCard0.setShadow(false);
        serviceImage0 = new javax.swing.JLabel();
        serviceImage0.setForeground(new java.awt.Color(0x1B1B1B));
        serviceImage0.setFont(new java.awt.Font("Segoe UI", 0, 13));
        serviceImage0.setText("");
        serviceImage0.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/images/washing.png")));
        serviceName0 = new javax.swing.JLabel();
        serviceName0.setForeground(new java.awt.Color(0x1B1B1B));
        serviceName0.setFont(new java.awt.Font("Segoe UI", 1, 15));
        serviceName0.setText("Washing");
        serviceName0.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        servicePrice0 = new javax.swing.JLabel();
        servicePrice0.setForeground(new java.awt.Color(0x145A32));
        servicePrice0.setFont(new java.awt.Font("Segoe UI", 0, 13));
        servicePrice0.setText("Loading rate...");
        servicePrice0.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        orderButton0 = new components.RoundedButton();
        orderButton0.setBackground(new java.awt.Color(0x1E8E3E));
        orderButton0.setForeground(new java.awt.Color(0xFFFFFF));
        orderButton0.setFont(new java.awt.Font("Segoe UI", 1, 13));
        orderButton0.setText("Order Now  >");
        orderButton0.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                orderButton0ActionPerformed(evt);
            }
        });
        serviceCard1 = new components.RoundedPanel();
        serviceCard1.setLayout(null);
        serviceCard1.setBackground(new java.awt.Color(0xF4F9F4));
        serviceCard1.setShadow(false);
        serviceImage1 = new javax.swing.JLabel();
        serviceImage1.setForeground(new java.awt.Color(0x1B1B1B));
        serviceImage1.setFont(new java.awt.Font("Segoe UI", 0, 13));
        serviceImage1.setText("");
        serviceImage1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/images/dry-cleaning.png")));
        serviceName1 = new javax.swing.JLabel();
        serviceName1.setForeground(new java.awt.Color(0x1B1B1B));
        serviceName1.setFont(new java.awt.Font("Segoe UI", 1, 15));
        serviceName1.setText("Dry Cleaning");
        serviceName1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        servicePrice1 = new javax.swing.JLabel();
        servicePrice1.setForeground(new java.awt.Color(0x145A32));
        servicePrice1.setFont(new java.awt.Font("Segoe UI", 0, 13));
        servicePrice1.setText("Loading rate...");
        servicePrice1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        orderButton1 = new components.RoundedButton();
        orderButton1.setBackground(new java.awt.Color(0x1E8E3E));
        orderButton1.setForeground(new java.awt.Color(0xFFFFFF));
        orderButton1.setFont(new java.awt.Font("Segoe UI", 1, 13));
        orderButton1.setText("Order Now  >");
        orderButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                orderButton1ActionPerformed(evt);
            }
        });
        serviceCard2 = new components.RoundedPanel();
        serviceCard2.setLayout(null);
        serviceCard2.setBackground(new java.awt.Color(0xF4F9F4));
        serviceCard2.setShadow(false);
        serviceImage2 = new javax.swing.JLabel();
        serviceImage2.setForeground(new java.awt.Color(0x1B1B1B));
        serviceImage2.setFont(new java.awt.Font("Segoe UI", 0, 13));
        serviceImage2.setText("");
        serviceImage2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/images/folding.png")));
        serviceName2 = new javax.swing.JLabel();
        serviceName2.setForeground(new java.awt.Color(0x1B1B1B));
        serviceName2.setFont(new java.awt.Font("Segoe UI", 1, 15));
        serviceName2.setText("Folding");
        serviceName2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        servicePrice2 = new javax.swing.JLabel();
        servicePrice2.setForeground(new java.awt.Color(0x145A32));
        servicePrice2.setFont(new java.awt.Font("Segoe UI", 0, 13));
        servicePrice2.setText("Loading rate...");
        servicePrice2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        orderButton2 = new components.RoundedButton();
        orderButton2.setBackground(new java.awt.Color(0x1E8E3E));
        orderButton2.setForeground(new java.awt.Color(0xFFFFFF));
        orderButton2.setFont(new java.awt.Font("Segoe UI", 1, 13));
        orderButton2.setText("Order Now  >");
        orderButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                orderButton2ActionPerformed(evt);
            }
        });
        trackingPanel = new components.RoundedPanel();
        trackingPanel.setLayout(null);
        trackingTitle = new javax.swing.JLabel();
        trackingTitle.setForeground(new java.awt.Color(0x1B1B1B));
        trackingTitle.setFont(new java.awt.Font("Segoe UI", 1, 18));
        trackingTitle.setText("Track Your Laundry");
        trackingOrder = new javax.swing.JLabel();
        trackingOrder.setForeground(new java.awt.Color(0x6B6B6B));
        trackingOrder.setFont(new java.awt.Font("Segoe UI", 0, 12));
        trackingOrder.setText("Your latest order at a glance");
        progress = new components.StepProgressBar();
        progress.setVertical(true);
        completionPanel = new components.RoundedPanel();
        completionPanel.setLayout(null);
        completionPanel.setBackground(new java.awt.Color(0xE8F5E9));
        completionPanel.setShadow(false);
        completionTitle = new javax.swing.JLabel();
        completionTitle.setForeground(new java.awt.Color(0x145A32));
        completionTitle.setFont(new java.awt.Font("Segoe UI", 1, 12));
        completionTitle.setText("Estimated Completion");
        completionValue = new javax.swing.JLabel();
        completionValue.setForeground(new java.awt.Color(0x145A32));
        completionValue.setFont(new java.awt.Font("Segoe UI", 0, 13));
        completionValue.setText("Place an order to get started");
        recentPanel = new components.RoundedPanel();
        recentPanel.setLayout(null);
        recentTitle = new javax.swing.JLabel();
        recentTitle.setForeground(new java.awt.Color(0x1B1B1B));
        recentTitle.setFont(new java.awt.Font("Segoe UI", 1, 18));
        recentTitle.setText("Your Recent Orders");
        viewOrdersButton = new components.RoundedButton();
        viewOrdersButton.setBackground(new java.awt.Color(0xFFFFFF));
        viewOrdersButton.setForeground(new java.awt.Color(0x145A32));
        viewOrdersButton.setFont(new java.awt.Font("Segoe UI", 1, 13));
        viewOrdersButton.setText("View All \u2192");
        viewOrdersButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewOrdersButtonActionPerformed(evt);
            }
        });
        ordersScroll = new javax.swing.JScrollPane();
        ordersTable = new javax.swing.JTable();
        thanksPanel = new components.RoundedPanel();
        thanksPanel.setLayout(null);
        thanksPanel.setBackground(new java.awt.Color(0x1E8E3E));
        thanksPanel.setShadow(false);
        thanksLabel = new javax.swing.JLabel();
        thanksLabel.setForeground(new java.awt.Color(0xFFFFFF));
        thanksLabel.setFont(new java.awt.Font("Segoe UI", 1, 12));
        thanksLabel.setText("Thank you for choosing Metric Laundry!");
        loadStatus = new javax.swing.JLabel();
        loadStatus.setForeground(new java.awt.Color(0x6B6B6B));
        loadStatus.setFont(new java.awt.Font("Segoe UI", 0, 12));
        loadStatus.setText("Loading your services and orders...");
        refreshButton = new components.RoundedButton();
        refreshButton.setBackground(new java.awt.Color(0xFFFFFF));
        refreshButton.setForeground(new java.awt.Color(0x145A32));
        refreshButton.setFont(new java.awt.Font("Segoe UI", 1, 13));
        refreshButton.setText("Refresh");
        refreshButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refreshButtonActionPerformed(evt);
            }
        });
        sidebar.add(logo);
        logo.setBounds(79, 30, 64, 64);
        sidebar.add(brand);
        brand.setBounds(20, 103, 184, 24);
        sidebar.add(brandTagline);
        brandTagline.setBounds(20, 132, 184, 20);
        sidebar.add(homeButton);
        homeButton.setBounds(20, 196, 184, 46);
        sidebar.add(placeOrderButton);
        placeOrderButton.setBounds(20, 252, 184, 46);
        sidebar.add(trackButton);
        trackButton.setBounds(20, 308, 184, 46);
        sidebar.add(paymentButton);
        paymentButton.setBounds(20, 364, 184, 46);
        sidebar.add(historyButton);
        historyButton.setBounds(20, 420, 184, 46);
        sidebar.add(aboutButton);
        aboutButton.setBounds(20, 476, 184, 46);
        sidebar.add(profileButton);
        profileButton.setBounds(20, 532, 184, 46);
        sidebar.add(logoutButton);
        logoutButton.setBounds(20, 695, 184, 42);
        sidebar.add(sidebarLeaves);
        sidebarLeaves.setBounds(0, 578, 170, 170);
        mainPanel.add(sidebar);
        sidebar.setBounds(0, 0, 224, 760);
        welcomeBanner.add(eyebrow);
        eyebrow.setBounds(26, 18, 380, 18);
        welcomeBanner.add(welcomeTitle);
        welcomeTitle.setBounds(26, 43, 725, 44);
        welcomeBanner.add(welcomeTagline);
        welcomeTagline.setBounds(26, 94, 480, 25);
        welcomeBanner.add(basketImage);
        basketImage.setBounds(754, -3, 240, 160);
        mainPanel.add(welcomeBanner);
        welcomeBanner.setBounds(248, 26, 1008, 150);
        servicesPanel.add(servicesTitle);
        servicesTitle.setBounds(23, 18, 400, 26);
        servicesPanel.add(viewServicesButton);
        viewServicesButton.setBounds(512, 13, 119, 36);
        serviceCard0.add(serviceImage0);
        serviceImage0.setBounds(28, 5, 140, 92);
        serviceCard0.add(serviceName0);
        serviceName0.setBounds(8, 99, 181, 25);
        serviceCard0.add(servicePrice0);
        servicePrice0.setBounds(8, 127, 181, 22);
        serviceCard0.add(orderButton0);
        orderButton0.setBounds(26, 158, 145, 34);
        servicesPanel.add(serviceCard0);
        serviceCard0.setBounds(20, 59, 197, 200);
        serviceCard1.add(serviceImage1);
        serviceImage1.setBounds(28, 5, 140, 92);
        serviceCard1.add(serviceName1);
        serviceName1.setBounds(8, 99, 181, 25);
        serviceCard1.add(servicePrice1);
        servicePrice1.setBounds(8, 127, 181, 22);
        serviceCard1.add(orderButton1);
        orderButton1.setBounds(26, 158, 145, 34);
        servicesPanel.add(serviceCard1);
        serviceCard1.setBounds(225, 59, 197, 200);
        serviceCard2.add(serviceImage2);
        serviceImage2.setBounds(28, 5, 140, 92);
        serviceCard2.add(serviceName2);
        serviceName2.setBounds(8, 99, 181, 25);
        serviceCard2.add(servicePrice2);
        servicePrice2.setBounds(8, 127, 181, 22);
        serviceCard2.add(orderButton2);
        orderButton2.setBounds(26, 158, 145, 34);
        servicesPanel.add(serviceCard2);
        serviceCard2.setBounds(430, 59, 197, 200);
        mainPanel.add(servicesPanel);
        servicesPanel.setBounds(248, 196, 656, 278);
        trackingPanel.add(trackingTitle);
        trackingTitle.setBounds(23, 18, 286, 26);
        trackingPanel.add(trackingOrder);
        trackingOrder.setBounds(23, 50, 286, 20);
        trackingPanel.add(progress);
        progress.setBounds(24, 84, 282, 292);
        completionPanel.add(completionTitle);
        completionTitle.setBounds(16, 10, 253, 20);
        completionPanel.add(completionValue);
        completionValue.setBounds(16, 34, 253, 21);
        trackingPanel.add(completionPanel);
        completionPanel.setBounds(22, 386, 287, 68);
        mainPanel.add(trackingPanel);
        trackingPanel.setBounds(924, 196, 332, 478);
        recentPanel.add(recentTitle);
        recentTitle.setBounds(23, 17, 340, 26);
        recentPanel.add(viewOrdersButton);
        viewOrdersButton.setBounds(512, 12, 119, 36);
        ordersScroll.setViewportView(ordersTable);
        recentPanel.add(ordersScroll);
        ordersScroll.setBounds(20, 58, 616, 140);
        mainPanel.add(recentPanel);
        recentPanel.setBounds(248, 494, 656, 220);
        thanksPanel.add(thanksLabel);
        thanksLabel.setBounds(15, 9, 308, 26);
        mainPanel.add(thanksPanel);
        thanksPanel.setBounds(924, 690, 332, 44);
        mainPanel.add(loadStatus);
        loadStatus.setBounds(252, 725, 855, 22);
        mainPanel.add(refreshButton);
        refreshButton.setBounds(1135, 736, 116, 24);
        mainPanel.setPreferredSize(new java.awt.Dimension(1280, 760));
        getContentPane().add(mainPanel, java.awt.BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void homeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_homeButtonActionPerformed
        navigate("home");
    }//GEN-LAST:event_homeButtonActionPerformed

    private void placeOrderButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_placeOrderButtonActionPerformed
        navigate("place");
    }//GEN-LAST:event_placeOrderButtonActionPerformed

    private void trackButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_trackButtonActionPerformed
        navigate("track");
    }//GEN-LAST:event_trackButtonActionPerformed

    private void paymentButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_paymentButtonActionPerformed
        navigate("payment");
    }//GEN-LAST:event_paymentButtonActionPerformed

    private void historyButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_historyButtonActionPerformed
        navigate("history");
    }//GEN-LAST:event_historyButtonActionPerformed

    private void aboutButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutButtonActionPerformed
        navigate("about");
    }//GEN-LAST:event_aboutButtonActionPerformed

    private void profileButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_profileButtonActionPerformed
        navigate("profile");
    }//GEN-LAST:event_profileButtonActionPerformed

    private void logoutButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_logoutButtonActionPerformed
        navigate("logout");
    }//GEN-LAST:event_logoutButtonActionPerformed

    private void viewServicesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewServicesButtonActionPerformed
        navigate("place");
    }//GEN-LAST:event_viewServicesButtonActionPerformed

    private void orderButton0ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_orderButton0ActionPerformed
        navigate("order0");
    }//GEN-LAST:event_orderButton0ActionPerformed

    private void orderButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_orderButton1ActionPerformed
        navigate("order1");
    }//GEN-LAST:event_orderButton1ActionPerformed

    private void orderButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_orderButton2ActionPerformed
        navigate("order2");
    }//GEN-LAST:event_orderButton2ActionPerformed

    private void viewOrdersButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewOrdersButtonActionPerformed
        navigate("history");
    }//GEN-LAST:event_viewOrdersButtonActionPerformed

    private void refreshButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshButtonActionPerformed
        navigate("home");
    }//GEN-LAST:event_refreshButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel mainPanel;
    private javax.swing.JPanel sidebar;
    private javax.swing.JLabel logo;
    private javax.swing.JLabel brand;
    private javax.swing.JLabel brandTagline;
    private components.RoundedButton homeButton;
    private components.RoundedButton placeOrderButton;
    private components.RoundedButton trackButton;
    private components.RoundedButton paymentButton;
    private components.RoundedButton historyButton;
    private components.RoundedButton aboutButton;
    private components.RoundedButton profileButton;
    private components.RoundedButton logoutButton;
    private javax.swing.JLabel sidebarLeaves;
    private components.RoundedPanel welcomeBanner;
    private javax.swing.JLabel eyebrow;
    private javax.swing.JLabel welcomeTitle;
    private javax.swing.JLabel welcomeTagline;
    private javax.swing.JLabel basketImage;
    private components.RoundedPanel servicesPanel;
    private javax.swing.JLabel servicesTitle;
    private components.RoundedButton viewServicesButton;
    private components.RoundedPanel serviceCard0;
    private javax.swing.JLabel serviceImage0;
    private javax.swing.JLabel serviceName0;
    private javax.swing.JLabel servicePrice0;
    private components.RoundedButton orderButton0;
    private components.RoundedPanel serviceCard1;
    private javax.swing.JLabel serviceImage1;
    private javax.swing.JLabel serviceName1;
    private javax.swing.JLabel servicePrice1;
    private components.RoundedButton orderButton1;
    private components.RoundedPanel serviceCard2;
    private javax.swing.JLabel serviceImage2;
    private javax.swing.JLabel serviceName2;
    private javax.swing.JLabel servicePrice2;
    private components.RoundedButton orderButton2;
    private components.RoundedPanel trackingPanel;
    private javax.swing.JLabel trackingTitle;
    private javax.swing.JLabel trackingOrder;
    private components.StepProgressBar progress;
    private components.RoundedPanel completionPanel;
    private javax.swing.JLabel completionTitle;
    private javax.swing.JLabel completionValue;
    private components.RoundedPanel recentPanel;
    private javax.swing.JLabel recentTitle;
    private components.RoundedButton viewOrdersButton;
    private javax.swing.JScrollPane ordersScroll;
    private javax.swing.JTable ordersTable;
    private components.RoundedPanel thanksPanel;
    private javax.swing.JLabel thanksLabel;
    private javax.swing.JLabel loadStatus;
    private components.RoundedButton refreshButton;
    // End of variables declaration//GEN-END:variables
}
