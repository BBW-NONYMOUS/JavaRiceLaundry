package components;

import java.awt.*;
import javax.swing.JButton;
import javax.swing.BorderFactory;

/** Keyboard-accessible button with hover, press, disabled and focus treatments. */
public class RoundedButton extends JButton {
    private int cornerRadius = 12;
    public RoundedButton() {
        setBackground(Theme.GREEN); setForeground(Color.WHITE); setFont(Theme.font(13, true));
        setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false);
        setOpaque(false); setRolloverEnabled(true); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        setIconTextGap(10);
    }
    public int getCornerRadius() { return cornerRadius; }
    public void setCornerRadius(int value) { cornerRadius = Math.max(0, value); repaint(); }
    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color fill = getBackground();
        if (!isEnabled()) fill = Theme.BORDER;
        else if (getModel().isPressed()) fill = fill.darker();
        else if (getModel().isRollover()) fill = new Color(Math.max(0, fill.getRed() - 12),
                Math.max(0, fill.getGreen() - 12), Math.max(0, fill.getBlue() - 12));
        g.setColor(fill); g.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
        if (isFocusOwner()) {
            g.setColor(getForeground()); g.setStroke(new BasicStroke(2));
            g.drawRoundRect(3, 3, getWidth() - 7, getHeight() - 7, cornerRadius, cornerRadius);
        }
        g.dispose(); super.paintComponent(graphics);
    }
}
