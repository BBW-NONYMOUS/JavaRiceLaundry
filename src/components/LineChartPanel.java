package components;

import java.awt.*;
import java.awt.geom.Path2D;
import javax.swing.JPanel;

/** Small dependency-free chart with an honest empty state and scaled value labels. */
public class LineChartPanel extends JPanel {
    private double[] values = new double[0];
    private String[] labels = new String[0];
    public LineChartPanel() { setBackground(Color.WHITE); setFont(Theme.font(12, false)); }
    public void setData(String[] labels, double[] values) {
        if (labels == null || values == null || labels.length != values.length)
            throw new IllegalArgumentException("Labels and values must have equal lengths");
        for (double value : values) if (!Double.isFinite(value) || value < 0)
            throw new IllegalArgumentException("Chart values must be finite and nonnegative");
        this.labels = labels.clone(); this.values = values.clone(); repaint();
    }
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(getFont());
        if (values.length == 0) { g.setColor(Theme.MUTED); g.drawString("No sales recorded", 24, 40); g.dispose(); return; }
        double max = Math.max(1, java.util.Arrays.stream(values).max().orElse(1));
        int left = 65, right = getWidth() - 30, top = 25, bottom = getHeight() - 36;
        for (int i = 0; i <= 4; i++) {
            int y = bottom - (bottom - top) * i / 4;
            g.setColor(Theme.BORDER); g.drawLine(left, y, right, y);
            g.setColor(Theme.MUTED); g.drawString(String.format(java.util.Locale.US, "%.0f", max * i / 4), 8, y + 4);
        }
        Path2D line = new Path2D.Double();
        int[] xs = new int[values.length], ys = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            xs[i] = values.length == 1 ? (left + right) / 2 : left + (right - left) * i / (values.length - 1);
            ys[i] = bottom - (int) ((bottom - top) * values[i] / max);
            if (i == 0) line.moveTo(xs[i], ys[i]); else line.lineTo(xs[i], ys[i]);
        }
        Path2D area = (Path2D) line.clone();
        area.lineTo(xs[xs.length - 1], bottom); area.lineTo(xs[0], bottom); area.closePath();
        g.setPaint(new GradientPaint(0, top, Theme.ACCENT, 0, bottom, Color.WHITE)); g.fill(area);
        g.setColor(Theme.GREEN); g.setStroke(new BasicStroke(2.5f)); g.draw(line);
        for (int i = 0; i < xs.length; i++) {
            g.setColor(Theme.GREEN); g.fillOval(xs[i] - 4, ys[i] - 4, 8, 8);
            g.setColor(Theme.MUTED); g.drawString(labels[i], xs[i] - g.getFontMetrics().stringWidth(labels[i]) / 2, bottom + 22);
        }
        g.dispose();
    }
}
