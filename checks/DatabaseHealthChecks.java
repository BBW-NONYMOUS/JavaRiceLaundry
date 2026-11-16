package laundry;

import dao.CustomerDashboardDAOImpl;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Live mode is read-only. Write tests use a newly created, uniquely named database. */
public final class DatabaseHealthChecks {
    private static int passed;
    private static final List<String> issues = new ArrayList<>();
    private static void check(boolean ok, String description) {
        if (!ok) throw new AssertionError(description);
        passed++; System.out.println("PASS: " + description);
    }
    private static long count(String sql, Object... args) throws SQLException {
        try (Connection c=Database.connect(); PreparedStatement ps=c.prepareStatement(sql)) {
            for(int i=0;i<args.length;i++) ps.setObject(i+1,args[i]);
            try(ResultSet rs=ps.executeQuery()) { rs.next();return rs.getLong(1); }
        }
    }
    private static void login(String email,String password,String role) {
        Session.logout();User user=Session.login(email,password);
        check(user!=null && role.equals(user.getRole()), role+" sample credentials authenticate");
    }
    private static void live() throws Exception {
        check(Database.NAME.equals("metric_laundry"),"Live check targets metric_laundry");
        try(Connection c=Database.connect()) {
            check(c.isValid(5),"Application JDBC connection is valid");
            System.out.println("SERVER: " + c.getMetaData().getDatabaseProductName()+" "+c.getMetaData().getDatabaseProductVersion());
        }
        check(count("SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=? AND TABLE_NAME IN"
                +" ('users','services','orders','payments','order_status_history') AND ENGINE='InnoDB'",Database.NAME)==5,
                "All five expected InnoDB tables exist");
        check(count("SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=?",Database.NAME)==6,
                "All six foreign-key constraints exist");
        for(String table:List.of("users","services","orders","payments","order_status_history")) {
            // Names are selected from a fixed application-owned list, never from user input.
            try(Connection c=Database.connect();PreparedStatement ps=c.prepareStatement("CHECK TABLE "+table);ResultSet rs=ps.executeQuery()) {
                boolean ok=false;while(rs.next()) if("status".equals(rs.getString("Msg_type")))ok="OK".equals(rs.getString("Msg_text"));
                check(ok,table+" integrity check");
            }
        }
        login("juan.dcruz@email.com","password123",User.CUSTOMER);
        int customer=Session.getCurrentUser().getId();
        check(new CustomerDashboardDAOImpl().load(customer).services().size()==3,"Customer dashboard reads the live service catalog");
        Session.logout();check(Session.login("juan.dcruz@email.com","incorrect-password")==null,"Wrong password is rejected");
        login("staff@metriclaundry.com","staff123",User.STAFF);
        check(Session.isStaff()&&!Session.isAdmin(),"Staff role is recognized correctly");
        login("admin@metriclaundry.com","admin123",User.ADMIN);
        check(Session.isAdmin(),"Admin role is recognized correctly");
        check(count("SELECT COUNT(*) FROM orders o LEFT JOIN users u ON u.user_id=o.customer_id LEFT JOIN services s"
                +" ON s.service_code=o.service_code WHERE u.user_id IS NULL OR s.service_code IS NULL")==0,"No orphaned orders");
        check(count("SELECT COUNT(*) FROM orders o LEFT JOIN payments p ON p.order_id=o.order_id WHERE p.payment_id IS NULL")==0,
                "Every existing order has a payment record");
        check(count("SELECT COUNT(*) FROM orders o LEFT JOIN order_status_history h ON h.order_id=o.order_id"
                +" AND h.status='Received' WHERE h.history_id IS NULL")==0,"Every existing order has initial status history");
        check(count("SELECT COUNT(*) FROM payments WHERE payment_status='Paid' AND (paid_at IS NULL OR verified_by IS NULL)")==0,
                "Paid records contain payment time and verifier");
        check(count("SELECT COUNT(*) FROM orders o JOIN payments p ON p.order_id=o.order_id WHERE o.total_amount<>p.amount")==0,
                "Existing order and payment amounts agree");
        check(count("SELECT COUNT(*) FROM orders WHERE ABS(total_amount-ROUND(weight_kg*rate_per_kg,2))>0.005")==0,
                "Existing order totals match their saved weight and rate");
        System.out.println("LIVE_COUNTS: users="+count("SELECT COUNT(*) FROM users")+", services="+count("SELECT COUNT(*) FROM services")
                +", orders="+count("SELECT COUNT(*) FROM orders")+", payments="+count("SELECT COUNT(*) FROM payments")
                +", history="+count("SELECT COUNT(*) FROM order_status_history"));
        Session.logout();
    }
    private static void isolated() throws Exception {
        String name="metric_laundry_verify_"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))+"_"+UUID.randomUUID().toString().substring(0,8);
        if(!name.matches("metric_laundry_verify_[0-9]{14}_[a-f0-9]{8}"))throw new IllegalStateException("Unsafe test database name");
        System.setProperty("laundry.db.name",name);
        System.out.println("ISOLATED_DATABASE: "+name);
        boolean created=false;
        try {
            check(Database.NAME.equals(name),"Application connection is isolated from the live database");
            // Check uniqueness before setup; never adopt or delete an existing database.
            try(Connection c=DriverManager.getConnection("jdbc:mysql://"+Database.HOST+"/",Database.USER,Database.PASSWORD);
                    PreparedStatement ps=c.prepareStatement("SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME=?")) {
                ps.setString(1,name);try(ResultSet rs=ps.executeQuery()){rs.next();check(rs.getInt(1)==0,"Test database name is unused");}
            }
            Database.setup();created=true;
            check(count("SELECT COUNT(*) FROM users")==3 && count("SELECT COUNT(*) FROM services")==3,"Fresh schema and sample data initialize correctly");
            java.lang.reflect.Field ready=Database.class.getDeclaredField("ready");ready.setAccessible(true);ready.setBoolean(null,false);
            Database.setup();check(count("SELECT COUNT(*) FROM users")==3,"Repeated startup does not duplicate accounts");
            Session.register("Database Test Customer","db-check@example.com","09123456789","TestPass123");
            check(Session.emailExists("db-check@example.com"),"Registration persists a new customer");
            login("db-check@example.com","TestPass123",User.CUSTOMER);
            check(count("SELECT COUNT(*) FROM users WHERE email=? AND password_hash LIKE 'pbkdf2_sha256$%'","db-check@example.com")==1,
                    "Registration stores a password hash");
            boolean duplicateBlocked=false;try{Session.register("Duplicate","db-check@example.com","09123456789","TestPass123");}
            catch(Database.DataException expected){duplicateBlocked=true;}
            check(duplicateBlocked && count("SELECT COUNT(*) FROM users")==4,"Duplicate email is rejected without adding a user");
            Session.updateProfile("Updated Test Customer","09987654321","ChangedPass123");Session.reload();
            check(Session.getCurrentUser().getName().equals("Updated Test Customer")&&Session.getCurrentUser().getPhone().equals("09987654321"),
                    "Profile changes persist and reload");
            login("db-check@example.com","ChangedPass123",User.CUSTOMER);
            Session.logout();check(Session.login("db-check@example.com","TestPass123")==null,"Old password no longer authenticates after password change");
            login("admin@metriclaundry.com","admin123",User.ADMIN);
            Session.updateService(Service.WASH_AND_FOLD,30,2,true);
            check(Session.getService(Service.WASH_AND_FOLD).getRatePerKg()==30,"Admin service-rate changes persist");
            login("db-check@example.com","ChangedPass123",User.CUSTOMER);
            Session.setDraftService(Service.WASH_AND_FOLD);Session.setDraftWeight(5);Order cash=Session.placeOrder(Order.CASH,"");
            check(cash.getTotal()==150&&cash.getStatus()==Order.RECEIVED&&cash.getPaymentStatus().equals(Order.UNPAID),"Cash order saves with correct total and initial states");
            check(count("SELECT COUNT(*) FROM payments WHERE order_id=?",cash.getId())==1
                    &&count("SELECT COUNT(*) FROM order_status_history WHERE order_id=?",cash.getId())==1,"Order, payment and initial history are saved together");
            Session.setDraftService(Service.DRY_CLEANING);Session.setDraftWeight(2);Order gcash=Session.placeOrder(Order.GCASH,"VERIFY-GCASH-1234");
            check(gcash.getPaymentStatus().equals(Order.PENDING_VERIFICATION)&&gcash.getPaymentReference().equals("VERIFY-GCASH-1234"),
                    "GCash reference and pending verification persist");
            Session.setDraftService(Service.IRONING);Session.setDraftWeight(1);Order bank=Session.placeOrder(Order.BANK_TRANSFER,"VERIFY-BANK-1234");
            check(bank.getPaymentStatus().equals(Order.PENDING_VERIFICATION)&&bank.getPaymentReference().equals("VERIFY-BANK-1234"),
                    "Bank transfer reference and pending verification persist");
            check(Session.getMyOrders().size()==3,"Customer can reload all of their orders");
            login("juan.dcruz@email.com","password123",User.CUSTOMER);
            check(Session.getMyOrders().isEmpty(),"Other customers do not see the test customer's orders");
            login("staff@metriclaundry.com","staff123",User.STAFF);
            check(Session.getAllOrders().size()==3,"Staff can retrieve saved orders");
            Session.updateOrder(cash,Order.WASHING,Order.UNPAID,"Verification note");
            Session.updateOrder(cash,Order.READY,Order.PAID,"Ready for collection");
            check(cash.isPaid()&&cash.getAmountDue()==0&&cash.getPaidAt()!=null,"Payment verification saves paid state and timestamp");
            check(cash.getStatus()==Order.READY&&count("SELECT COUNT(*) FROM order_status_history WHERE order_id=?",cash.getId())==4,
                    "Status updates and skipped-stage history persist");
            check(count("SELECT COUNT(*) FROM payments WHERE order_id=? AND verified_by=?",cash.getId(),Session.getCurrentUser().getId())==1,
                    "Payment records the verifying staff member");
            Session.updateOrder(cash,Order.CLAIMED,Order.PAID,"Collected");
            check(cash.getStatus()==Order.CLAIMED&&cash.getStatusTime(Order.CLAIMED)!=null,"Completed order persists its final status and time");
            boolean rateBlocked=false;try{Session.updateService(Service.WASH_AND_FOLD,99,1,true);}catch(IllegalStateException expected){rateBlocked=true;}
            check(rateBlocked&&Session.getService(Service.WASH_AND_FOLD).getRatePerKg()==30,"Staff cannot change service rates");
            login("admin@metriclaundry.com","admin123",User.ADMIN);
            Session.updateService(Service.WASH_AND_FOLD,35,1,true);
            login("db-check@example.com","ChangedPass123",User.CUSTOMER);
            Order fresh=Session.getMyOrders().stream().filter(o->o.getId()==cash.getId()).findFirst().orElseThrow();
            check(fresh.getTotal()==150&&fresh.getPricePerKg()==30&&fresh.getStaffNote().equals("Collected"),"Existing order retains its original rate, total and saved staff note");
            long before=count("SELECT COUNT(*) FROM orders");long payments=count("SELECT COUNT(*) FROM payments");
            Session.setDraftService(Service.WASH_AND_FOLD);Session.setDraftWeight(1);
            boolean rolledBack=false;try{Session.placeOrder(null,"");}catch(Database.DataException expected){rolledBack=true;}
            check(rolledBack&&count("SELECT COUNT(*) FROM orders")==before&&count("SELECT COUNT(*) FROM payments")==payments,
                    "Failure inserting payment rolls back the order insertion");
            boolean fkBlocked=false;
            try(Connection c=Database.connect();PreparedStatement ps=c.prepareStatement("DELETE FROM services WHERE service_code=?")) {
                ps.setString(1,Service.WASH_AND_FOLD);ps.executeUpdate();
            }catch(SQLException expected){fkBlocked=true;}
            check(fkBlocked,"Foreign keys prevent deleting a service referenced by orders");

            // Probe known application boundaries separately from database/server health.
            Session.setDraftWeight(2.333);Order precision=Session.placeOrder(Order.CASH,"");
            double recomputed=Order.charge(precision.getWeightKg(),precision.getPricePerKg());
            if(Math.abs(precision.getTotal()-recomputed)>0.005)issues.add("Weight precision: saved "+precision.getWeightKg()+" kg at "+precision.getPricePerKg()
                    +" produces "+recomputed+", but the order total is "+precision.getTotal()+".");
            login("admin@metriclaundry.com","admin123",User.ADMIN);Session.updateService(Service.IRONING,30,1,false);
            login("db-check@example.com","ChangedPass123",User.CUSTOMER);Session.setDraftService(Service.IRONING);Session.setDraftWeight(1);
            try{Session.placeOrder(Order.CASH,"");issues.add("Service availability: the save operation accepts an order for a disabled service.");}
            catch(IllegalStateException|IllegalArgumentException expected){check(true,"Disabled service rejected at save");}
        } finally {
            Session.logout();
            if(created)try(Connection c=DriverManager.getConnection("jdbc:mysql://"+Database.HOST+"/",Database.USER,Database.PASSWORD);
                    PreparedStatement ps=c.prepareStatement("DROP DATABASE `"+name+"`")){ps.executeUpdate();System.out.println("CLEANUP: removed only "+name);}
        }
    }
    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        if(args.length!=1||!(args[0].equals("live")||args[0].equals("isolated")))throw new IllegalArgumentException("Use live or isolated");
        if(args[0].equals("live"))live();else isolated();
        System.out.println("RESULT: "+passed+" checks passed; "+issues.size()+" application issues found.");
        for(String issue:issues)System.out.println("APPLICATION ISSUE: "+issue);
    }
}
