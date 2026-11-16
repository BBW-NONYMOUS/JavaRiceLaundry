package components;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.TableCellRenderer;

/** Renders a text-labelled badge, so status is not communicated by color alone. */
public class StatusBadgeRenderer extends JPanel implements TableCellRenderer {
    private String status = "";
    public StatusBadgeRenderer() { setOpaque(true); setFont(Theme.font(12, true)); }
    @Override public Component getTableCellRendererComponent(JTable table, Object value,
            boolean selected, boolean focused, int row, int column) {
        status = value == null ? "" : value.toString();
        if (status.equals("Claimed")) status = "Completed";
        setBackground(selected ? table.getSelectionBackground() : table.getBackground());
        getAccessibleContext().setAccessibleName(status);
        return this;
    }
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        boolean complete = status.equals("Completed") || status.equals("Ready for Pickup") || status.equals("Paid");
        g.setFont(getFont());
        int w = Math.min(getWidth() - 8, g.getFontMetrics().stringWidth(status) + 20);
        int x = (getWidth() - w) / 2, y = (getHeight() - 26) / 2;
        g.setColor(complete ? Theme.ACCENT : Theme.WARNING_BG); g.fillRoundRect(x, y, w, 26, 12, 12);
        g.setColor(complete ? Theme.SUCCESS_TEXT : Theme.WARNING_TEXT);
        Shape clip = g.getClip(); g.clipRect(x + 6, y, Math.max(0, w - 12), 26);
        g.drawString(status, x + 10, y + 17); g.setClip(clip); g.dispose();
    }
}
