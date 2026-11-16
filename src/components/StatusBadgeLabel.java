package components;

import java.awt.*;
import javax.swing.JLabel;

/** Standalone status badge for order details, editable in the GUI Builder. */
public class StatusBadgeLabel extends JLabel {
    public StatusBadgeLabel() {
        setOpaque(false);setFont(Theme.font(13,true));setHorizontalAlignment(CENTER);
        setBackground(Theme.WARNING_BG);setForeground(Theme.WARNING_TEXT);
    }
    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g=(Graphics2D)graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(getBackground());g.fillRoundRect(0,0,getWidth(),getHeight(),12,12);g.dispose();
        super.paintComponent(graphics);
    }
}
