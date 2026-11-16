package components;

import java.awt.*;
import javax.swing.border.AbstractBorder;

/** Outline for selected cards, using the same radius as the card's fill. */
public final class RoundedOutlineBorder extends AbstractBorder {
    private final Color color;
    private final int width;
    private final int radius;
    public RoundedOutlineBorder(Color color, int width, int radius) {
        this.color = java.util.Objects.requireNonNull(color);
        this.width = width;
        this.radius = radius;
    }
    @Override public Insets getBorderInsets(Component c) { return new Insets(width,width,width,width); }
    @Override public Insets getBorderInsets(Component c, Insets insets) { insets.set(width,width,width,width); return insets; }
    @Override public void paintBorder(Component c, Graphics graphics, int x, int y, int w, int h) {
        Graphics2D g=(Graphics2D)graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);g.setStroke(new BasicStroke(width));
        float inset=width/2f;
        g.draw(new java.awt.geom.RoundRectangle2D.Float(x+inset,y+inset,w-width,h-width,radius,radius));
        g.dispose();
    }
}
