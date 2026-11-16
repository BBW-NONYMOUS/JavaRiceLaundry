package controller;

import dao.CustomerDashboardDAO;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.SwingWorker;
import model.DashboardData;

/** One background read per refresh. Results and failures are delivered on the EDT. */
public final class CustomerDashboardController {
    private final CustomerDashboardDAO dao;
    private SwingWorker<DashboardData, Void> worker;
    public CustomerDashboardController(CustomerDashboardDAO dao) { this.dao = java.util.Objects.requireNonNull(dao); }
    public void load(int customerId, Consumer<DashboardData> success, Consumer<String> failure) {
        cancel();
        worker = new SwingWorker<>() {
            @Override protected DashboardData doInBackground() throws Exception { return dao.load(customerId); }
            @Override protected void done() {
                if (isCancelled()) return;
                try { success.accept(get()); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                catch (ExecutionException e) {
                    java.util.logging.Logger.getLogger(CustomerDashboardController.class.getName())
                            .log(java.util.logging.Level.WARNING, "Dashboard load failed", e.getCause());
                    failure.accept("We couldn't load your dashboard. Check that MySQL is running, then click Refresh.");
                }
            }
        };
        worker.execute();
    }
    public void cancel() { if (worker != null && !worker.isDone()) worker.cancel(true); }
}
