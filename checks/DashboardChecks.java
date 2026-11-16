import components.*;
import controller.CustomerDashboardController;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.SQLException;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import model.DashboardData;
import model.DashboardData.*;
import view.customer.CustomerDashboard;

/** Standalone assertions and offscreen render checks; never connects to the live database. */
public final class DashboardChecks {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static DashboardData example() {
        List<ServiceCard> services = List.of(
            new ServiceCard("WASH_FOLD", "Washing", "Everyday essentials, freshly cleaned.", new BigDecimal("30"), true),
            new ServiceCard("DRY_CLEAN", "Dry Cleaning", "Gentle care for special fabrics.", new BigDecimal("50"), true),
            new ServiceCard("FOLDING", "Folding", "Neat, folded and ready to go.", new BigDecimal("20"), true));
        LocalDateTime placed = LocalDateTime.of(2026, 11, 15, 8, 30);
        List<RecentOrder> orders = List.of(
            new RecentOrder(12, "#012", "Washing", new BigDecimal("5"), new BigDecimal("150"), "Washing", placed,
                placed.toLocalDate().plusDays(1), Map.of("Received", placed.plusMinutes(10), "Washing", placed.plusHours(1))),
            new RecentOrder(11, "#011", "Dry Cleaning", new BigDecimal("2"), new BigDecimal("100"), "Ready for Pickup",
                placed.minusDays(1), placed.toLocalDate(), Map.of()),
            new RecentOrder(10, "#010", "Folding", new BigDecimal("3"), new BigDecimal("60"), "Claimed",
                placed.minusDays(2), placed.toLocalDate().minusDays(1), Map.of()));
        return new DashboardData(services, orders);
    }
    private static <T extends Component> T find(Container parent, Class<T> type, java.util.function.Predicate<T> predicate) {
        for (Component child : parent.getComponents()) {
            if (type.isInstance(child) && predicate.test(type.cast(child))) return type.cast(child);
            if (child instanceof Container nested) {
                T result = find(nested, type, predicate);
                if (result != null) return result;
            }
        }
        return null;
    }
    private static void waitFor(java.util.function.BooleanSupplier ready) throws Exception {
        for (int i = 0; i < 100; i++) {
            AtomicBoolean done = new AtomicBoolean();
            SwingUtilities.invokeAndWait(() -> done.set(ready.getAsBoolean()));
            if (done.get()) return;
            Thread.sleep(30);
        }
        throw new AssertionError("Timed out waiting for asynchronous view update");
    }
    private static void render(CustomerDashboard frame, String file) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            frame.addNotify(); frame.validate();
            BufferedImage image = new BufferedImage(1280, 760, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics(); frame.getContentPane().printAll(g); g.dispose();
            try { ImageIO.write(image, "png", Path.of(file).toFile()); }
            catch (java.io.IOException e) { throw new RuntimeException(e); }
        });
    }
    public static void main(String[] args) throws Exception {
        Theme.install();
        Files.createDirectories(Path.of("target/review"));
        DashboardData sample = example();
        check(sample.orders().get(0).step() == 2, "Washing maps to step 2");
        check(sample.orders().get(2).displayStatus().equals("Completed"), "Legacy claimed mapping");
        check(sample.orders().get(2).step() == 5, "Completed maps to step 5");
        check(util.CurrencyFormat.peso(new BigDecimal("1234.50")).equals("\u20b11,234.50"), "Peso formatting");
        AtomicBoolean background = new AtomicBoolean();
        AtomicReference<CustomerDashboard> ref = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> ref.set(new CustomerDashboard(1, id -> {
            background.set(!SwingUtilities.isEventDispatchThread()); return sample;
        })));
        CustomerDashboard frame = ref.get();
        waitFor(() -> find(frame, JTable.class, t -> true).getRowCount() == 3);
        check(background.get(), "DAO must run outside EDT");
        check(!frame.isResizable(), "Fixed frame size");
        render(frame, "target/review/customer-dashboard.png");
        SwingUtilities.invokeAndWait(frame::dispose);

        SwingUtilities.invokeAndWait(() -> ref.set(new CustomerDashboard(1, id -> new DashboardData(List.of(), List.of()))));
        CustomerDashboard empty = ref.get();
        waitFor(() -> find(empty, JLabel.class, l -> l.getText() != null && l.getText().startsWith("No services")) != null);
        render(empty, "target/review/customer-dashboard-empty.png");
        SwingUtilities.invokeAndWait(empty::dispose);

        SwingUtilities.invokeAndWait(() -> ref.set(new CustomerDashboard(1, id -> { throw new SQLException("Simulated outage"); })));
        CustomerDashboard failure = ref.get();
        waitFor(() -> find(failure, JLabel.class, l -> l.getText() != null && l.getText().startsWith("We couldn't")) != null);
        SwingUtilities.invokeAndWait(() -> {
            JButton refresh = find(failure, JButton.class, b -> b.getText().equals("Refresh"));
            check(refresh.isEnabled(), "Refresh recovers from errors");
            check(!find(failure, JButton.class, b -> b.getText().equals("Order Now  >")).isEnabled(), "Stale checkout disabled on error");
            failure.dispose();
            StepProgressBar progress = new StepProgressBar();
            try { progress.setCurrentStep(6); throw new AssertionError("Invalid progress accepted"); }
            catch (IllegalArgumentException expected) { }
            LineChartPanel chart = new LineChartPanel();
            try { chart.setData(new String[]{"Mon"}, new double[]{Double.NaN}); throw new AssertionError("NaN accepted"); }
            catch (IllegalArgumentException expected) { }
            try { chart.setData(new String[]{"Mon"}, new double[0]); throw new AssertionError("Mismatched data accepted"); }
            catch (IllegalArgumentException expected) { }
        });

        CountDownLatch callback = new CountDownLatch(1);
        AtomicBoolean callbackOnEdt = new AtomicBoolean();
        SwingUtilities.invokeAndWait(() -> new CustomerDashboardController(id -> sample).load(1, data -> {
            callbackOnEdt.set(SwingUtilities.isEventDispatchThread()); callback.countDown();
        }, message -> { throw new AssertionError(message); }));
        check(callback.await(3, TimeUnit.SECONDS) && callbackOnEdt.get(), "Controller must update Swing on EDT");
        System.out.println("PASS: dashboard data, status mapping, currency, background reads, EDT callbacks, populated/empty/error states, refresh, progress and chart validation.");
        System.exit(0);
    }
}
