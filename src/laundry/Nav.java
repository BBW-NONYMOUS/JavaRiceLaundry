package laundry;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * Moves between screens: opens the next JFrame where the current one was, then closes it.
 */
public final class Nav {

    // customer screens
    public static final int HOME = 0;
    public static final int ORDERS = 1;
    public static final int SERVICES = 2;
    public static final int PROFILE = 3;
    // staff screens (Services & Rates and Sales Summary are admin only)
    public static final int STAFF_HOME = 10;
    public static final int STAFF_ORDERS = 11;
    public static final int STAFF_SERVICES = 12;
    public static final int STAFF_REPORT = 13;

    private Nav() {
    }

    /** Sidebar and dashboard shortcuts. */
    public static void go(JFrame from, int target) {
        if (Session.getCurrentUser() == null) {
            Ui.warn(from, "Please log in first.");
            return;
        }
        boolean staffTarget = target >= STAFF_HOME;
        boolean adminOnly = target == STAFF_SERVICES || target == STAFF_REPORT;
        if (staffTarget != Session.isStaff() || (adminOnly && !Session.isAdmin())) {
            Ui.warn(from, "This page is not available for your account.");
            return;
        }
        open(from, create(target));
    }

    /** The first screen after logging in. */
    public static void home(JFrame from) {
        open(from, create(Session.isStaff() ? STAFF_HOME : HOME));
    }

    private static JFrame create(int target) {
        switch (target) {
            case ORDERS:
                return new MyOrdersForm();
            case SERVICES:
                return new SelectServiceForm();
            case PROFILE:
                return new ProfileForm();
            case STAFF_HOME:
                return new StaffDashboardForm();
            case STAFF_ORDERS:
                return new StaffOrdersForm();
            case STAFF_SERVICES:
                return new StaffServicesForm();
            case STAFF_REPORT:
                return new StaffReportForm();
            default:
                return new view.customer.CustomerDashboard();
        }
    }

    public static void open(JFrame from, JFrame next) {
        next.setLocationRelativeTo(null);
        next.setVisible(true);
        from.dispose();
    }

    public static void logout(JFrame from) {
        int choice = JOptionPane.showConfirmDialog(from, "Do you want to log out?", "Logout",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            Session.logout();
            open(from, new LoginForm());
        }
    }
}
