package laundry;

/**
 * A row of the {@code users} table: a customer, laundry staff (orders and payments), or the admin / owner
 * (everything staff can do, plus the service rates and sales reports).
 */
public class User {

    public static final String CUSTOMER = "Customer";
    public static final String STAFF = "Staff";
    public static final String ADMIN = "Admin";

    private final int id;
    private String name;
    private final String email;
    private String phone;
    private final String role;

    public User(int id, String name, String email, String phone, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    /** Staff and admin both use the staff panel. */
    public boolean isStaff() {
        return STAFF.equals(role) || isAdmin();
    }

    public boolean isAdmin() {
        return ADMIN.equals(role);
    }
}
