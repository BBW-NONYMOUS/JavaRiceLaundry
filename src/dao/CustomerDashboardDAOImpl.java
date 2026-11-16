package dao;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import laundry.Database;
import model.DashboardData;
import model.DashboardData.RecentOrder;
import model.DashboardData.ServiceCard;

/** Read-only adapter for the existing schema; every query uses a PreparedStatement. */
public final class CustomerDashboardDAOImpl implements CustomerDashboardDAO {
    @Override public DashboardData load(int customerId) throws SQLException {
        if (customerId <= 0) throw new IllegalArgumentException("A signed-in customer is required");
        try (Connection c = Database.connect()) {
            c.setReadOnly(true);
            c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            c.setAutoCommit(false);
            try {
                List<ServiceCard> services = new ArrayList<>();
                try (PreparedStatement ps = c.prepareStatement("SELECT service_code, service_name, description,"
                        + " rate_per_kg, is_available FROM services ORDER BY sort_order LIMIT 3");
                        ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) services.add(new ServiceCard(rs.getString(1), rs.getString(2), rs.getString(3),
                            rs.getBigDecimal(4), rs.getBoolean(5)));
                }
                List<RecentOrder> orders = new ArrayList<>();
                String sql = "SELECT order_id, service_name, weight_kg, total_amount, order_status, order_date,"
                        + " expected_completion FROM orders WHERE customer_id = ? ORDER BY order_id DESC LIMIT 5";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setInt(1, customerId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            int id = rs.getInt("order_id");
                            // Legacy numbers stay unchanged until persistent daily sequences are introduced.
                            orders.add(new RecentOrder(id, String.format(Locale.US, "#%03d", id),
                                    rs.getString("service_name"), rs.getBigDecimal("weight_kg"),
                                    rs.getBigDecimal("total_amount"), rs.getString("order_status"),
                                    rs.getTimestamp("order_date").toLocalDateTime(),
                                    rs.getDate("expected_completion").toLocalDate(), Map.of()));
                        }
                    }
                }
                if (!orders.isEmpty()) {
                    RecentOrder latest = orders.get(0);
                    Map<String, LocalDateTime> history = new HashMap<>();
                    try (PreparedStatement ps = c.prepareStatement("SELECT status, MAX(changed_at) AS changed_at"
                            + " FROM order_status_history WHERE order_id = ? GROUP BY status")) {
                        ps.setInt(1, latest.id());
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) history.put(rs.getString(1), rs.getTimestamp(2).toLocalDateTime());
                        }
                    }
                    orders.set(0, new RecentOrder(latest.id(), latest.number(), latest.service(), latest.quantity(),
                            latest.total(), latest.status(), latest.placedAt(), latest.expectedCompletion(), history));
                }
                c.commit(); return new DashboardData(services, orders);
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }
}
