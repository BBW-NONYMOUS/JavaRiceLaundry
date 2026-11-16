package components;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.Icon;

/** Original vector navigation marks, crisp at any UI scale. */
public final class BrandIcon implements Icon {
    private final String kind;
    private final Color color;
    public BrandIcon(String kind, Color color) { this.kind = kind; this.color = color; }
    @Override public int getIconWidth() { return 20; }
    @Override public int getIconHeight() { return 20; }
    @Override public void paintIcon(Component c, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create(); g.translate(x, y);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color); g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (kind) {
            case "home" -> { g.drawPolyline(new int[]{2,10,18}, new int[]{9,2,9},3); g.drawRect(5,9,10,9); g.drawRect(8,12,4,6); }
            case "order" -> { g.drawRoundRect(3,5,14,13,3,3); g.drawArc(6,1,8,9,0,180); g.drawLine(7,11,13,11); g.drawLine(10,8,10,14); }
            case "track" -> { g.drawOval(3,2,14,14); g.drawOval(8,7,4,4); g.drawLine(6,14,10,19); g.drawLine(14,14,10,19); }
            case "payment" -> { g.drawRoundRect(2,4,16,13,3,3); g.drawLine(2,8,18,8); g.drawLine(5,13,9,13); }
            case "history" -> { g.drawArc(3,3,14,14,40,300); g.drawLine(3,3,3,8); g.drawLine(3,8,8,8); g.drawLine(10,6,10,10); g.drawLine(10,10,13,12); }
            case "refresh" -> { g.drawArc(3,3,14,14,45,285); g.drawLine(17,3,17,8); g.drawLine(17,8,12,8); }
            case "logout" -> { g.drawPolyline(new int[]{8,3,3,8},new int[]{3,3,17,17},4); g.drawLine(8,10,18,10); g.drawLine(14,6,18,10); g.drawLine(14,14,18,10); }
            case "profile" -> { g.drawOval(7,2,6,6); g.drawArc(3,10,14,14,0,180); }
            case "heart" -> { Path2D p = new Path2D.Double(); p.moveTo(10,18); p.curveTo(-7,7,5,-3,10,5); p.curveTo(15,-3,27,7,10,18); g.fill(p); }
            default -> { g.drawOval(2,2,16,16); g.drawLine(10,9,10,14); g.fillOval(9,5,2,2); }
        }
        g.dispose();
    }
}
