package components;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import javax.swing.UIManager;

/** Shared design tokens. Install once, before creating any Swing windows. */
public final class Theme {
    public static final Color GREEN = Color.decode("#1E8E3E");
    public static final Color DARK_GREEN = Color.decode("#145A32");
    public static final Color LIGHT_GREEN = Color.decode("#E8F5E9");
    public static final Color ACCENT = Color.decode("#C8E6C9");
    public static final Color BACKGROUND = Color.decode("#F5F8F5");
    public static final Color TEXT = Color.decode("#1B1B1B");
    public static final Color MUTED = Color.decode("#6B6B6B");
    public static final Color BORDER = Color.decode("#E3EBE4");
    public static final Color WARNING_BG = Color.decode("#FFF3B0");
    public static final Color WARNING_TEXT = Color.decode("#8A6D00");
    public static final Color SUCCESS_TEXT = Color.decode("#1E6B2F");
    private static final String FAMILY = Arrays.asList(GraphicsEnvironment
            .getLocalGraphicsEnvironment().getAvailableFontFamilyNames()).contains("Segoe UI")
            ? "Segoe UI" : Font.SANS_SERIF;

    private Theme() { }

    public static Font font(int size, boolean bold) {
        return new Font(FAMILY, bold ? Font.BOLD : Font.PLAIN, size);
    }

    public static void install() {
        FlatLightLaf.setup();
        UIManager.put("defaultFont", font(13, false));
        UIManager.put("Component.focusColor", GREEN);
        UIManager.put("Component.accentColor", GREEN);
        UIManager.put("Button.arc", 12);
        UIManager.put("TextComponent.arc", 12);
        UIManager.put("Table.selectionBackground", LIGHT_GREEN);
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("TableHeader.background", LIGHT_GREEN);
        UIManager.put("TableHeader.foreground", DARK_GREEN);
        UIManager.put("ScrollBar.width", 10);
    }
}
