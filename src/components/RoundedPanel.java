package components;

import java.awt.*;
import javax.swing.JPanel;

/** JavaBean with a public no-argument constructor for the NetBeans Palette. */
public class RoundedPanel extends JPanel {
    private int cornerRadius = 16;
    private Color gradientEnd;
    private boolean shadow = true;
    public RoundedPanel() {
        setOpaque(false); setBackground(Color.WHITE); setBorder(new ShadowBorder());
    }
    public int getCornerRadius() { return cornerRadius; }
    public void setCornerRadius(int value) { cornerRadius = Math.max(0, value); repaint(); }
    public Color getGradientEnd() { return gradientEnd; }
    public void setGradientEnd(Color color) { gradientEnd = color; repaint(); }
    public boolean isShadow() { return shadow; }
    public void setShadow(boolean value) {
        shadow = value;
        // A designer-specified input outline must survive switching off the shadow.
        if (value && (getBorder() == null || getBorder() instanceof ShadowBorder)) setBorder(new ShadowBorder());
        else if (!value && getBorder() instanceof ShadowBorder) setBorder(null);
        repaint();
    }
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Insets in = getInsets();
        g.setPaint(gradientEnd == null ? getBackground()
                : new GradientPaint(0, 0, getBackground(), getWidth(), getHeight(), gradientEnd));
        g.fillRoundRect(in.left, in.top, getWidth() - in.left - in.right,
                getHeight() - in.top - in.bottom, cornerRadius, cornerRadius);
        g.dispose();
    }
}
