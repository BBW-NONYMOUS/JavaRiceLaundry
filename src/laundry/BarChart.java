package laundry;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JPanel;

/**
 * A simple bar chart for the GUI builder. Set {@code title}, {@code subtitle} and {@code horizontal}
 * in the Properties window; supply the numbers at runtime with {@link #setData}.
 */
public class BarChart extends components.RoundedPanel {

    private static final long serialVersionUID = 1L;
    private static final Color DARK = new Color(0x1B1B1B);
    private static final Color GRID = new Color(0xE3EBE4);
    private static final Color AXIS_TEXT = new Color(0x6B6B6B);

    private String title = "Chart";
    private String subtitle = "";
    private boolean horizontal;
    private boolean money;
    private String[] labels = {"A", "B", "C", "D", "E"};
    private double[] values = {3, 5, 2, 6, 4};   // sample bars shown in the designer
    private Color[] colors = {Ui.GREEN};
    private int highlight = -1;

    public BarChart() {
        setBackground(Color.WHITE);
    }

    /**
     * @param money show values as pesos
     * @param highlight index of a bar to emphasize (for example "today"), or -1
     */
    public void setData(String[] labels, double[] values, boolean money, int highlight) {
        this.labels = labels.clone();
        this.values = values.clone();
        this.money = money;
        this.highlight = highlight;
        repaint();
    }

    /** One color for all bars, or one per bar. */
    public void setColors(Color... colors) {
        this.colors = colors.clone();
        repaint();
    }

    public double[] getValues() {
        return values.clone();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        repaint();
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
        repaint();
    }

    public boolean isHorizontal() {
        return horizontal;
    }

    public void setHorizontal(boolean horizontal) {
        this.horizontal = horizontal;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Insets in = getInsets();
        int left = in.left + 18, top = in.top + 14;
        int right = getWidth() - in.right - 18, bottom = getHeight() - in.bottom - 12;

        g.setFont(new Font("Segoe UI", Font.BOLD, 15));
        g.setColor(DARK);
        g.drawString(title, left, top + g.getFontMetrics().getAscent());
        if (!subtitle.isEmpty()) {
            g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g.setColor(AXIS_TEXT);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(subtitle, right - fm.stringWidth(subtitle), top + 15);
        }
        top += 34;

        double max = 0;
        for (double v : values) {
            max = Math.max(max, v);
        }
        if (horizontal) {
            paintHorizontal(g, left, top, right, bottom, max);
        } else {
            paintVertical(g, left, top, right, bottom, niceMax(max));
        }
        if (max == 0) {
            g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g.setColor(AXIS_TEXT);
            String empty = "No data yet";
            FontMetrics fm = g.getFontMetrics();
            g.drawString(empty, (left + right - fm.stringWidth(empty)) / 2, (top + bottom) / 2);
        }
        g.dispose();
    }

    private void paintVertical(Graphics2D g, int left, int top, int right, int bottom, double max) {
        g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        FontMetrics fm = g.getFontMetrics();
        int axisWidth = fm.stringWidth(format(max, true)) + 10;
        int plotLeft = left + axisWidth, plotBottom = bottom - fm.getHeight() - 4;
        int plotTop = top + fm.getHeight();   // room for value labels above the tallest bar
        int steps = 4;
        for (int i = 0; i <= steps; i++) {
            int y = plotBottom - (plotBottom - plotTop) * i / steps;
            g.setColor(GRID);
            g.setStroke(i == 0 ? new BasicStroke(1.2f) : new BasicStroke(1f, BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER, 10, new float[] {3, 3}, 0));
            g.drawLine(plotLeft, y, right, y);
            g.setColor(AXIS_TEXT);
            String text = format(max * i / steps, true);
            g.drawString(text, plotLeft - 8 - fm.stringWidth(text), y + fm.getAscent() / 2 - 1);
        }
        g.setStroke(new BasicStroke(1f));
        int n = Math.max(1, values.length);
        double slot = (right - plotLeft) / (double) n;
        int barWidth = (int) Math.min(38, slot * 0.56);
        for (int i = 0; i < values.length; i++) {
            int cx = (int) (plotLeft + slot * i + slot / 2);
            int h = max == 0 ? 0 : (int) Math.round((plotBottom - plotTop) * values[i] / max);
            if (h > 0) {
                g.setColor(barColor(i));
                g.fill(new RoundRectangle2D.Double(cx - barWidth / 2.0, plotBottom - h, barWidth, h, 8, 8));
                int square = Math.min(h, 4);   // flat bottom on the baseline
                g.fillRect(cx - barWidth / 2, plotBottom - square, barWidth, square);
                g.setColor(DARK);
                String text = format(values[i], false);
                g.drawString(text, cx - fm.stringWidth(text) / 2, plotBottom - h - 4);
            }
            g.setColor(i == highlight ? DARK : AXIS_TEXT);
            g.drawString(labels[i], cx - fm.stringWidth(labels[i]) / 2, bottom - 2);
        }
    }

    private void paintHorizontal(Graphics2D g, int left, int top, int right, int bottom, double max) {
        g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        FontMetrics fm = g.getFontMetrics();
        int labelWidth = 0, valueWidth = 0;
        for (int i = 0; i < labels.length; i++) {
            labelWidth = Math.max(labelWidth, fm.stringWidth(labels[i]));
            valueWidth = Math.max(valueWidth, fm.stringWidth(format(values[i], false)));
        }
        int barLeft = left + labelWidth + 12, barRight = right - valueWidth - 8;
        int n = Math.max(1, values.length);
        double row = (bottom - top) / (double) n;
        int barHeight = (int) Math.min(18, row * 0.6);
        for (int i = 0; i < values.length; i++) {
            int cy = (int) (top + row * i + row / 2);
            g.setColor(AXIS_TEXT);
            g.drawString(labels[i], left, cy + fm.getAscent() / 2 - 1);
            g.setColor(new Color(0xE8F5E9));
            g.fill(new RoundRectangle2D.Double(barLeft, cy - barHeight / 2.0, barRight - barLeft, barHeight, 8, 8));
            int w = max == 0 ? 0 : (int) Math.round((barRight - barLeft) * values[i] / max);
            if (w > 0) {
                g.setColor(barColor(i));
                g.fill(new RoundRectangle2D.Double(barLeft, cy - barHeight / 2.0, Math.max(w, 8), barHeight, 8, 8));
            }
            g.setColor(DARK);
            g.drawString(format(values[i], false), barRight + 8, cy + fm.getAscent() / 2 - 1);
        }
    }

    /** With a highlighted bar, the other bars are drawn lighter. */
    private Color barColor(int i) {
        Color c = colors[i % colors.length];
        if (highlight < 0 || i == highlight) {
            return c;
        }
        return new Color((c.getRed() + 2 * 255) / 3, (c.getGreen() + 2 * 255) / 3, (c.getBlue() + 2 * 255) / 3);
    }

    /** Rounds the axis maximum up to 1, 2 or 5 times a power of ten. */
    private static double niceMax(double max) {
        if (max <= 0) {
            return 4;
        }
        double magnitude = Math.pow(10, Math.floor(Math.log10(max)));
        for (double step : new double[] {1, 2, 5, 10}) {
            if (step * magnitude >= max) {
                return step * magnitude;
            }
        }
        return 10 * magnitude;
    }

    private String format(double value, boolean axis) {
        if (!money) {
            return value == Math.floor(value) ? String.valueOf((long) value) : String.format("%.1f", value);
        }
        if (axis && value >= 1000) {
            return "₱" + Ui.number(value / 1000) + "k";
        }
        return axis || value != Math.floor(value) ? "₱" + Ui.number(value) : "₱" + String.format("%,d", (long) value);
    }
}
