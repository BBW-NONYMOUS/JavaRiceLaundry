package laundry;

/**
 * A row of the {@code services} table (the service and pricing list). Staff maintain the rate,
 * turnaround and availability.
 */
public class Service {

    public static final String WASH_AND_FOLD = "WASH_FOLD";
    public static final String DRY_CLEANING = "DRY_CLEAN";
    public static final String IRONING = "IRONING";

    private final String code;
    private final String name;
    private final String description;
    private final String icon;
    private double ratePerKg;
    private int turnaroundDays;
    private boolean available;

    public Service(String code, String name, String description, String icon, double ratePerKg,
            int turnaroundDays, boolean available) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.ratePerKg = ratePerKg;
        this.turnaroundDays = turnaroundDays;
        this.available = available;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getIcon() {
        return icon;
    }

    public double getRatePerKg() {
        return ratePerKg;
    }

    void setRatePerKg(double ratePerKg) {
        this.ratePerKg = ratePerKg;
    }

    public int getTurnaroundDays() {
        return turnaroundDays;
    }

    void setTurnaroundDays(int turnaroundDays) {
        this.turnaroundDays = turnaroundDays;
    }

    public boolean isAvailable() {
        return available;
    }

    void setAvailable(boolean available) {
        this.available = available;
    }
}
