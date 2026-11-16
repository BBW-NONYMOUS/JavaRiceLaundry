import dao.CustomerDashboardDAOImpl;
import model.DashboardData;

/** Optional live read-only verification. Never initializes or modifies the database. */
public final class DashboardDatabaseChecks {
    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        DashboardData data = new CustomerDashboardDAOImpl().load(1);
        if (data.services().size() > 3 || data.orders().size() > 5)
            throw new AssertionError("Dashboard query limits were exceeded");
        for (var service : data.services()) {
            if (service.price() == null || service.code() == null)
                throw new AssertionError("Service data missing");
        }
        System.out.println("PASS: live read-only dashboard DAO returned " + data.services().size()
                + " services and " + data.orders().size() + " orders for customer 1.");
    }
}
