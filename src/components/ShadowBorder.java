package components;

import java.awt.*;
import javax.swing.border.AbstractBorder;

/** Soft, reserved outside shadow; painting never overlaps the card contents. */
public class ShadowBorder extends AbstractBorder {
    @Override public Insets getBorderInsets(Component c) { return new Insets(3, 3, 8, 3); }
    @Override public Insets getBorderInsets(Component c, Insets in) {
        in.set(3, 3, 8, 3); return in;
    }
    @Override public void paintBorder(Component c, Graphics graphics, int x, int y, int w, int h) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int i = 6; i > 0; i--) {
            g.setColor(new Color(20, 70, 32, 3 + (6 - i) * 2));
            g.drawRoundRect(x + 3 - i / 3, y + 4 + i / 2, w - 7 + i / 2, h - 12 + i / 2, 16, 16);
        }
        g.dispose();
    }
}
