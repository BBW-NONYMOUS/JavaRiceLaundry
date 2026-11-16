package components;

import java.awt.*;
import javax.swing.JComponent;

/** Six-stage progress indicator; -1 means that no order has been placed. */
public class StepProgressBar extends JComponent {
    public static final String[] STEPS = {"Order Placed", "Received", "Washing", "Drying", "Ready for Pickup", "Completed"};
    private int currentStep = -1;
    private boolean vertical;
    private String[] timestamps = new String[STEPS.length];
    public StepProgressBar() { setFont(Theme.font(13, false)); setPreferredSize(new Dimension(900, 100)); }
    public int getCurrentStep() { return currentStep; }
    public void setCurrentStep(int step) {
        if (step < -1 || step >= STEPS.length) throw new IllegalArgumentException("Step must be -1 to 5");
        currentStep = step;
        getAccessibleContext().setAccessibleName(step < 0 ? "No order selected" : "Current status: " + STEPS[step]);
        repaint();
    }
    public boolean isVertical() { return vertical; }
    public void setVertical(boolean value) { vertical = value; repaint(); }
    public void setTimestamps(String[] times) {
        if (times == null || times.length != STEPS.length) throw new IllegalArgumentException("Six timestamps required");
        timestamps = times.clone(); repaint();
    }
    @Override public javax.accessibility.AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) accessibleContext = new AccessibleJComponent() { };
        return accessibleContext;
    }
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(getFont());
        int n = STEPS.length;
        for (int i = 0; i < n; i++) {
            int x = vertical ? 20 : 70 + i * Math.max(1, (getWidth() - 140) / (n - 1));
            int y = vertical ? 22 + i * Math.max(1, (getHeight() - 60) / (n - 1)) : 26;
            int next = vertical ? 22 + (i + 1) * Math.max(1, (getHeight() - 60) / (n - 1)) : 0;
            if (i < n - 1) {
                g.setStroke(new BasicStroke(2)); g.setColor(i < currentStep ? Theme.GREEN : Theme.BORDER);
                if (vertical) g.drawLine(x, y + 13, x, next - 13);
                else g.drawLine(x + 13, y, 70 + (i + 1) * Math.max(1, (getWidth() - 140) / (n - 1)) - 13, y);
            }
            boolean done = i < currentStep || (i == currentStep && i == n - 1);
            g.setColor(i <= currentStep ? Theme.GREEN : Theme.BORDER);
            g.fillOval(x - 11, y - 11, 22, 22);
            if (i == currentStep) { g.setStroke(new BasicStroke(2)); g.drawOval(x - 15, y - 15, 30, 30); }
            g.setColor(i <= currentStep ? Color.WHITE : Theme.MUTED);
            if (done) { g.setStroke(new BasicStroke(2)); g.drawLine(x - 4, y, x - 1, y + 3); g.drawLine(x - 1, y + 3, x + 5, y - 4); }
            else g.drawString(Integer.toString(i + 1), x - 4, y + 4);
            String label = STEPS[i];
            g.setFont(Theme.font(13, i == currentStep));
            g.setColor(i == currentStep ? Theme.GREEN : Theme.TEXT);
            int tx = vertical ? x + 28 : x - g.getFontMetrics().stringWidth(label) / 2;
            g.drawString(label, tx, vertical ? y - 2 : y + 35);
            String time = i == currentStep && !done ? "In Progress"
                    : timestamps[i] == null ? (done ? "Time not recorded" : "Pending") : timestamps[i];
            g.setFont(Theme.font(11, false)); g.setColor(Theme.MUTED);
            g.drawString(time, vertical ? tx : x - g.getFontMetrics().stringWidth(time) / 2, vertical ? y + 15 : y + 53);
        }
        g.dispose();
    }
}
