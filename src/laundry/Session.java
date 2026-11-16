package laundry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads and writes the system's records in MySQL (see {@code database.sql}) and remembers who is
 * logged in and the order being placed. Every screen gets its data from here.
 */
public final class Session {

    private static User currentUser;

    // the order being placed (kept in memory until it is confirmed)
    private static String draftServiceCode = Service.WASH_AND_FOLD;
    private static double draftWeight = 0;

    private Session() {
    }

    private static Database.DataException failed(String action, SQLException e) {
        return new Database.DataException("Could not " + action + " in the database: " + e.getMessage(), e);
    }

    // ---- accounts (users table) ----

    private static User toUser(ResultSet rs) throws SQLException {
        return new User(rs.getInt("user_id"), rs.getString("full_name"), rs.getString("email"),
                rs.getString("phone"), rs.getString("role"));
    }

    private static User findUser(String email) {
        try (Connection c = Database.connect();
                PreparedStatement ps = c.prepareStatement("SELECT * FROM users WHERE email = ?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? toUser(rs) : null;
            }
        } catch (SQLException e) {
            throw failed("read the account", e);
        }
    }

    public static User login(String email, String password) {
        try (Connection c = Database.connect();
                PreparedStatement ps = c.prepareStatement("SELECT * FROM users WHERE email = ?")) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && Passwords.matches(password, rs.getString("password_hash"))) {
                    currentUser = toUser(rs);
                    return currentUser;
                }
                return null;
            }
        } catch (SQLException e) {
            throw failed("check the login", e);
        }
    }

    public static boolean emailExists(String email) {
        return findUser(email.trim()) != null;
    }

    /** Creates a customer account. */
    public static void register(String name, String email, String phone, String password) {
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO users (full_name, email, phone, password_hash, role) VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, name);
            ps.setString(2, email.trim());
            ps.setString(3, phone);
            ps.setString(4, Passwords.hash(password));
            ps.setString(5, User.CUSTOMER);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw failed("create the account", e);
        }
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    /** Staff or admin. */
    public static boolean isStaff() {
        return currentUser != null && currentUser.isStaff();
    }

    public static boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    public static void updateProfile(String name, String phone, String newPassword) {
        String sql = newPassword.isEmpty()
                ? "UPDATE users SET full_name = ?, phone = ? WHERE user_id = ?"
                : "UPDATE users SET full_name = ?, phone = ?, password_hash = ? WHERE user_id = ?";
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, name);
            ps.setString(i++, phone);
            if (!newPassword.isEmpty()) {
                ps.setString(i++, Passwords.hash(newPassword));
            }
            ps.setInt(i, currentUser.getId());
            ps.executeUpdate();
            currentUser.setName(name);
            currentUser.setPhone(phone);
        } catch (SQLException e) {
            throw failed("save the profile", e);
        }
    }

    public static void logout() {
        currentUser = null;
        draftServiceCode = Service.WASH_AND_FOLD;
        draftWeight = 0;
    }

    /** Re-reads the logged-in account. Orders are always read fresh from the database. */
    public static void reload() {
        if (currentUser != null) {
            currentUser = findUser(currentUser.getEmail());
        }
    }

    /** Signs in the sample customer when a customer screen is run directly from NetBeans (Shift+F6). */
    public static void ensurePreviewUser() {
        if (currentUser == null) {
            currentUser = firstUser(User.CUSTOMER);
        }
    }

    /** Signs in the admin account when a staff screen is run directly from NetBeans (all staff screens work). */
    public static void ensurePreviewStaff() {
        if (currentUser == null || !currentUser.isAdmin()) {
            currentUser = firstUser(User.ADMIN);
        }
    }

    private static User firstUser(String role) {
        try (Connection c = Database.connect(); PreparedStatement ps =
                c.prepareStatement("SELECT * FROM users WHERE role = ? ORDER BY user_id LIMIT 1")) {
            ps.setString(1, role);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? toUser(rs) : null;
            }
        } catch (SQLException e) {
            throw failed("read the accounts", e);
        }
    }

    // ---- service and pricing list (services table) ----

    private static Service toService(ResultSet rs) throws SQLException {
        return new Service(rs.getString("service_code"), rs.getString("service_name"), rs.getString("description"),
                rs.getString("icon"), rs.getBigDecimal("rate_per_kg").doubleValue(), rs.getInt("turnaround_days"),
                rs.getBoolean("is_available"));
    }

    public static Service getService(String code) {
        try (Connection c = Database.connect();
                PreparedStatement ps = c.prepareStatement("SELECT * FROM services WHERE service_code = ?")) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return toService(rs);
                }
                throw new IllegalArgumentException("Unknown service " + code);
            }
        } catch (SQLException e) {
            throw failed("read the service list", e);
        }
    }

    public static List<Service> getServices() {
        List<Service> services = new ArrayList<>();
        try (Connection c = Database.connect(); Statement st = c.createStatement();
                ResultSet rs = st.executeQuery("SELECT * FROM services ORDER BY sort_order")) {
            while (rs.next()) {
                services.add(toService(rs));
            }
            return services;
        } catch (SQLException e) {
            throw failed("read the service list", e);
        }
    }

    /** Admin only. */
    public static void updateService(String code, double ratePerKg, int turnaroundDays, boolean available) {
        if (!isAdmin()) {
            throw new IllegalStateException("Only the admin can change service rates.");
        }
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(
                "UPDATE services SET rate_per_kg = ?, turnaround_days = ?, is_available = ? WHERE service_code = ?")) {
            ps.setBigDecimal(1, money(ratePerKg));
            ps.setInt(2, turnaroundDays);
            ps.setBoolean(3, available);
            ps.setString(4, code);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw failed("save the service rates", e);
        }
    }

    // ---- the order being placed ----

    public static void setDraftService(String code) {
        draftServiceCode = code;
    }

    public static Service getDraftService() {
        return getService(draftServiceCode);
    }

    public static double getDraftWeight() {
        return draftWeight;
    }

    public static void setDraftWeight(double weight) {
        draftWeight = weight;
    }

    public static double getDraftTotal() {
        return Order.charge(draftWeight, getDraftService().getRatePerKg());
    }

    public static LocalDate getDraftCompletion() {
        return LocalDate.now().plusDays(getDraftService().getTurnaroundDays());
    }

    // ---- orders (orders, payments and order_status_history tables) ----

    /** Saves the confirmed order, its payment record and its first status in one transaction. */
    public static Order placeOrder(String paymentMethod, String paymentReference) {
        Service service = getDraftService();
        LocalDateTime now = LocalDateTime.now().withNano(0);
        double total = Order.charge(draftWeight, service.getRatePerKg());
        try (Connection c = Database.connect()) {
            c.setAutoCommit(false);
            try {
                int orderId;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO orders (customer_id, service_code, service_name, weight_kg, rate_per_kg, total_amount,"
                        + " order_date, expected_completion, order_status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, currentUser.getId());
                    ps.setString(2, service.getCode());
                    ps.setString(3, service.getName());
                    ps.setBigDecimal(4, money(draftWeight));
                    ps.setBigDecimal(5, money(service.getRatePerKg()));
                    ps.setBigDecimal(6, money(total));
                    ps.setTimestamp(7, Timestamp.valueOf(now));
                    ps.setObject(8, java.sql.Date.valueOf(now.toLocalDate().plusDays(service.getTurnaroundDays())));
                    ps.setString(9, Order.STATUSES[Order.RECEIVED]);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        orderId = keys.getInt(1);
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO payments (order_id, payment_method, payment_reference, amount, payment_status)"
                        + " VALUES (?, ?, ?, ?, ?)")) {
                    ps.setInt(1, orderId);
                    ps.setString(2, paymentMethod);
                    ps.setString(3, paymentReference.isEmpty() ? null : paymentReference);
                    ps.setBigDecimal(4, money(total));
                    ps.setString(5, Order.CASH.equals(paymentMethod) ? Order.UNPAID : Order.PENDING_VERIFICATION);
                    ps.executeUpdate();
                }
                addHistory(c, orderId, Order.RECEIVED, now, null);
                c.commit();
                draftServiceCode = Service.WASH_AND_FOLD;
                draftWeight = 0;
                return loadOrders(c, "o.order_id = ?", orderId).get(0);
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw failed("save the order", e);
        }
    }

    /** Orders of the logged-in customer, newest first. */
    public static List<Order> getMyOrders() {
        try (Connection c = Database.connect()) {
            return loadOrders(c, "o.customer_id = ?", currentUser.getId());
        } catch (SQLException e) {
            throw failed("read your orders", e);
        }
    }

    /** All orders, newest first (staff). */
    public static List<Order> getAllOrders() {
        try (Connection c = Database.connect()) {
            return loadOrders(c, "1 = 1");
        } catch (SQLException e) {
            throw failed("read the orders", e);
        }
    }

    private static List<Order> loadOrders(Connection c, String where, Object... args) throws SQLException {
        Map<Integer, Order> orders = new LinkedHashMap<>();
        String sql = "SELECT o.*, u.full_name, u.email, u.phone, s.icon, p.payment_method, p.payment_reference,"
                + " p.payment_status, p.paid_at FROM orders o"
                + " JOIN users u ON u.user_id = o.customer_id"
                + " JOIN services s ON s.service_code = o.service_code"
                + " JOIN payments p ON p.order_id = o.order_id"
                + " WHERE " + where + " ORDER BY o.order_id DESC";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Order o = new Order();
                    o.id = rs.getInt("order_id");
                    o.customerName = rs.getString("full_name");
                    o.customerEmail = rs.getString("email");
                    o.customerPhone = rs.getString("phone");
                    o.service = rs.getString("service_name");
                    o.icon = rs.getString("icon");
                    o.weightKg = rs.getBigDecimal("weight_kg").doubleValue();
                    o.pricePerKg = rs.getBigDecimal("rate_per_kg").doubleValue();
                    o.total = rs.getBigDecimal("total_amount").doubleValue();
                    o.createdAt = rs.getTimestamp("order_date").toLocalDateTime();
                    o.expectedCompletion = rs.getDate("expected_completion").toLocalDate();
                    o.status = Order.statusIndex(rs.getString("order_status"));
                    o.staffNote = rs.getString("staff_note");
                    o.paymentMethod = rs.getString("payment_method");
                    o.paymentReference = rs.getString("payment_reference");
                    o.paymentStatus = rs.getString("payment_status");
                    Timestamp paid = rs.getTimestamp("paid_at");
                    o.paidAt = paid == null ? null : paid.toLocalDateTime();
                    orders.put(o.id, o);
                }
            }
        }
        if (!orders.isEmpty()) {
            // latest time each status was recorded
            String ids = orders.keySet().toString().replaceAll("[\\[\\] ]", "");
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(
                    "SELECT order_id, status, MAX(changed_at) AS changed_at FROM order_status_history"
                    + " WHERE order_id IN (" + ids + ") GROUP BY order_id, status")) {
                while (rs.next()) {
                    Order o = orders.get(rs.getInt("order_id"));
                    int step = Order.statusIndex(rs.getString("status"));
                    if (step <= o.status) {
                        o.statusTimes[step] = rs.getTimestamp("changed_at").toLocalDateTime();
                    }
                }
            }
        }
        return new ArrayList<>(orders.values());
    }

    private static void addHistory(Connection c, int orderId, int status, LocalDateTime when, Integer staffId)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO order_status_history (order_id, status, changed_at, changed_by) VALUES (?, ?, ?, ?)")) {
            ps.setInt(1, orderId);
            ps.setString(2, Order.STATUSES[status]);
            ps.setTimestamp(3, Timestamp.valueOf(when));
            ps.setObject(4, staffId);
            ps.executeUpdate();
        }
    }

    /**
     * Staff update of an order's status, payment status and note. Each new status is logged in
     * order_status_history (skipped steps are logged too), and verified payments record the staff member.
     */
    public static void updateOrder(Order order, int status, String paymentStatus, String staffNote) {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        try (Connection c = Database.connect()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE orders SET order_status = ?, staff_note = ? WHERE order_id = ?")) {
                    ps.setString(1, Order.STATUSES[status]);
                    ps.setString(2, staffNote);
                    ps.setInt(3, order.getId());
                    ps.executeUpdate();
                }
                if (status > order.getStatus()) {
                    for (int step = order.getStatus() + 1; step <= status; step++) {
                        addHistory(c, order.getId(), step, now, currentUser.getId());
                    }
                } else if (status < order.getStatus()) {
                    addHistory(c, order.getId(), status, now, currentUser.getId());
                }
                if (!paymentStatus.equals(order.getPaymentStatus())) {
                    boolean paid = Order.PAID.equals(paymentStatus);
                    try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE payments SET payment_status = ?, paid_at = ?, verified_by = ? WHERE order_id = ?")) {
                        ps.setString(1, paymentStatus);
                        ps.setTimestamp(2, paid ? Timestamp.valueOf(now) : null);
                        ps.setObject(3, paid ? currentUser.getId() : null);
                        ps.setInt(4, order.getId());
                        ps.executeUpdate();
                    }
                }
                c.commit();
                order.copyFrom(loadOrders(c, "o.order_id = ?", order.getId()).get(0));
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw failed("update the order", e);
        }
    }

    private static BigDecimal money(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    public static String paymentAccount(String method) {
        return Order.GCASH.equals(method)
                ? "GCash 0917 123 4567 (Metric Laundry)"
                : "BDO Savings 0012 3456 7890 (Metric Laundry)";
    }
}
