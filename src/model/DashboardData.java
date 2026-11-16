package model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Immutable read models keep JDBC and mutable session state out of the view. */
public record DashboardData(List<ServiceCard> services, List<RecentOrder> orders) {
    public DashboardData { services = List.copyOf(services); orders = List.copyOf(orders); }
    public record ServiceCard(String code, String name, String description, BigDecimal price, boolean available) { }
    public record RecentOrder(int id, String number, String service, BigDecimal quantity, BigDecimal total,
            String status, LocalDateTime placedAt, LocalDate expectedCompletion, Map<String, LocalDateTime> history) {
        public RecentOrder { history = Map.copyOf(history); }
        public int step() {
            return switch (status) {
                case "Order Placed" -> 0;
                case "Received" -> 1;
                case "Washing" -> 2;
                case "Drying" -> 3;
                case "Ready for Pickup" -> 4;
                case "Claimed", "Completed" -> 5;
                default -> -1;
            };
        }
        public String displayStatus() { return status.equals("Claimed") ? "Completed" : status; }
    }
}
