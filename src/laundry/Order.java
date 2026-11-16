package laundry;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A laundry order as stored in MySQL: the {@code orders} row, its {@code payments} row and the
 * time of each status from {@code order_status_history}. Instances are loaded by {@link Session}.
 */
public class Order {

    public static final String CASH = "Cash";
    public static final String GCASH = "GCash";
    public static final String BANK_TRANSFER = "Bank Transfer";

    public static final String UNPAID = "Unpaid";
    public static final String PENDING_VERIFICATION = "Pending Verification";
    public static final String PAID = "Paid";
    public static final String[] PAYMENT_STATUSES = {UNPAID, PENDING_VERIFICATION, PAID};

    public static final int RECEIVED = 0;
    public static final int WASHING = 1;
    public static final int DRYING = 2;
    public static final int READY = 3;
    public static final int CLAIMED = 4;
    public static final String[] STATUSES = {"Received", "Washing", "Drying", "Ready for Pickup", "Claimed"};

    // filled in by Session from the database
    int id;
    String customerName;
    String customerEmail;
    String customerPhone;
    String service;
    String icon;
    double weightKg;
    double pricePerKg;
    double total;
    LocalDateTime createdAt;
    LocalDate expectedCompletion;
    String paymentMethod;
    String paymentReference;
    String paymentStatus;
    LocalDateTime paidAt;
    int status;
    LocalDateTime[] statusTimes = new LocalDateTime[STATUSES.length];
    String staffNote;

    Order() {
    }

    /** The service charge: weight x rate, rounded to centavos. */
    public static double charge(double weightKg, double ratePerKg) {
        return Math.round(weightKg * ratePerKg * 100) / 100.0;
    }

    public static int statusIndex(String name) {
        return java.util.Arrays.asList(STATUSES).indexOf(name);
    }

    /** Copies a freshly loaded version of this order (after an update). */
    void copyFrom(Order fresh) {
        customerName = fresh.customerName;
        customerPhone = fresh.customerPhone;
        paymentStatus = fresh.paymentStatus;
        paidAt = fresh.paidAt;
        status = fresh.status;
        statusTimes = fresh.statusTimes;
        staffNote = fresh.staffNote;
    }

    public int getId() {
        return id;
    }

    public String getNumber() {
        return String.format("#%03d", id);
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public String getService() {
        return service;
    }

    public String getIcon() {
        return icon;
    }

    public double getWeightKg() {
        return weightKg;
    }

    public double getPricePerKg() {
        return pricePerKg;
    }

    public double getTotal() {
        return total;
    }

    public double getAmountDue() {
        return isPaid() ? 0 : total;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDate getExpectedCompletion() {
        return expectedCompletion;
    }

    // ---- payment ----

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getPaymentReference() {
        return paymentReference == null ? "" : paymentReference;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public boolean isPaid() {
        return PAID.equals(paymentStatus);
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    // ---- status recorded by staff ----

    public int getStatus() {
        return status;
    }

    public String getStatusName() {
        return STATUSES[status];
    }

    public LocalDateTime getStatusTime(int step) {
        return statusTimes[step];
    }

    /** Washing and Drying are shown as "In Progress" while they are the current status. */
    public boolean isStepInProgress(int step) {
        return step == status && (step == WASHING || step == DRYING);
    }

    public boolean isStepDone(int step) {
        return step <= status && !isStepInProgress(step);
    }

    public String getStaffNote() {
        return staffNote == null ? "" : staffNote;
    }
}
