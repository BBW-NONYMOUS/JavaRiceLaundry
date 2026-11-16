package laundry;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;

/**
 * Shared styling and small helpers used by the screens.
 */
public final class Ui {

    public static final Color GREEN = components.Theme.GREEN;
    public static final Color TEXT = components.Theme.TEXT;
    public static final Color MUTED = components.Theme.MUTED;
    public static final Color HINT = components.Theme.MUTED;
    public static final Color BORDER = components.Theme.BORDER;
    private static final Color SELECTED_BG = components.Theme.LIGHT_GREEN;
    /** Chart colors for Received, Washing, Drying, Ready for Pickup, Claimed. */
    public static final Color[] STATUS_COLORS = {new Color(138, 109, 0), GREEN, new Color(86, 164, 104),
        components.Theme.DARK_GREEN, new Color(107, 107, 107)};

    private static final String HINT_TEXT = "laundry.hint";
    private static final String HINT_SHOWING = "laundry.hintShowing";
    private static final String ECHO_CHAR = "laundry.echoChar";
    private static final String REVEALED = "laundry.revealed";

    private Ui() {
    }

    public static void installLookAndFeel() {
        components.Theme.install();
    }

    /** Common window identity for every editable JFrame form. */
    public static void finishFrame(JFrame frame) {
        frame.setIconImage(new ImageIcon(Ui.class.getResource("/resources/images/logo.png")).getImage());
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
    }

    public static void showAbout(Component parent) {
        info(parent, "Metric Laundry\nClean Clothes, Happy You!\n\n"
                + "Careful laundry service for your everyday essentials.\n"
                + "Kalawag 1, back of Water District\n8:00 AM - 5:00 PM (Daily)");
    }

    // ---- formatting ----

    public static String peso(double amount) {
        return "₱" + String.format("%,.2f", amount);
    }

    public static String pesoHtml(double amount) {
        return "&#8369;" + String.format("%,.2f", amount);
    }

    public static String number(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    public static String kg(double value) {
        return number(value) + " kg";
    }

    public static ImageIcon icon(String file) {
        java.net.URL resource = Ui.class.getResource("/resources/images/ui/" + file);
        if (resource == null) resource = Ui.class.getResource("/resources/images/ui/info.png");
        return new ImageIcon(resource);
    }

    /** Parses a number typed by the user; returns -1 if it is not a valid number. */
    public static double parseNumber(String text) {
        try {
            double value = Double.parseDouble(text.trim());
            return Double.isFinite(value) ? value : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static String days(int days) {
        return days + (days == 1 ? " day" : " days");
    }

    /** Read-only table with the app's styling. */
    public static javax.swing.table.DefaultTableModel table(javax.swing.JTable table, String... columns) {
        javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table.setModel(model);
        for (int i = 0; i < columns.length; i++) {
            if ("Status".equals(columns[i]) || "Payment Status".equals(columns[i]))
                table.getColumnModel().getColumn(i).setCellRenderer(new components.StatusBadgeRenderer());
        }
        table.setRowHeight(30);
        table.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
        table.getTableHeader().setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 13));
        table.getTableHeader().setReorderingAllowed(false);
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionBackground(components.Theme.LIGHT_GREEN);
        table.setSelectionForeground(TEXT);
        table.setGridColor(BORDER);
        table.setShowVerticalLines(false);
        table.setFillsViewportHeight(true);
        if (table.getParent() != null) {
            table.getParent().setBackground(Color.WHITE);
        }
        return model;
    }

    // ---- validation ----

    public static boolean isEmail(String email) {
        return email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    }

    public static boolean isPhone(String phone) {
        return phone.matches("09\\d{9}");
    }

    // ---- placeholder text for text and password fields ----

    public static void placeholder(final JTextField field, String hint) {
        field.putClientProperty(HINT_TEXT, hint);
        if (field instanceof JPasswordField) {
            field.putClientProperty(ECHO_CHAR, ((JPasswordField) field).getEchoChar());
        }
        if (field.getText().isEmpty()) {
            showHint(field);
        }
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (isHintShowing(field)) {
                    field.setText("");
                    field.setForeground(TEXT);
                    field.putClientProperty(HINT_SHOWING, Boolean.FALSE);
                    updateEcho(field);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    showHint(field);
                }
            }
        });
    }

    /** The typed text, or "" while the placeholder is showing. */
    public static String value(JTextField field) {
        if (isHintShowing(field)) {
            return "";
        }
        if (field instanceof JPasswordField) {
            return new String(((JPasswordField) field).getPassword());
        }
        return field.getText().trim();
    }

    public static void setValue(JTextField field, String text) {
        if (text.isEmpty() && field.getClientProperty(HINT_TEXT) != null) {
            showHint(field);
        } else {
            field.putClientProperty(HINT_SHOWING, Boolean.FALSE);
            field.setForeground(TEXT);
            field.setText(text);
            updateEcho(field);
        }
    }

    public static void togglePassword(JPasswordField field) {
        field.putClientProperty(REVEALED, !Boolean.TRUE.equals(field.getClientProperty(REVEALED)));
        updateEcho(field);
    }

    private static boolean isHintShowing(JTextField field) {
        return Boolean.TRUE.equals(field.getClientProperty(HINT_SHOWING));
    }

    private static void showHint(JTextField field) {
        field.putClientProperty(HINT_SHOWING, Boolean.TRUE);
        field.setForeground(HINT);
        field.setText((String) field.getClientProperty(HINT_TEXT));
        updateEcho(field);
    }

    private static void updateEcho(JTextField field) {
        if (field instanceof JPasswordField) {
            Object echo = field.getClientProperty(ECHO_CHAR);
            if (echo == null) {
                echo = '•';
                field.putClientProperty(ECHO_CHAR, echo);
            }
            boolean plain = isHintShowing(field) || Boolean.TRUE.equals(field.getClientProperty(REVEALED));
            ((JPasswordField) field).setEchoChar(plain ? (char) 0 : (Character) echo);
        }
    }

    /** Gives focus to a component once the window opens, so field placeholders stay visible. */
    public static void focusOnOpen(JFrame frame, final JComponent component) {
        frame.getRootPane().setDefaultButton(component instanceof javax.swing.JButton ? (javax.swing.JButton) component : null);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                component.requestFocusInWindow();
            }
        });
    }

    // ---- cards and dialogs ----

    /** Highlights a selectable card (services, payment methods). */
    public static void select(JComponent card, boolean selected) {
        card.setBorder(new components.RoundedOutlineBorder(selected ? GREEN : BORDER, selected ? 2 : 1, 16));
        card.setBackground(selected ? SELECTED_BG : Color.WHITE);
    }

    /** Staff panel: shows "ADMIN PANEL" or "STAFF PANEL" and hides admin-only menu items from staff. */
    public static void applyRole(JLabel roleLabel, JComponent servicesNav, JComponent reportNav) {
        boolean admin = Session.isAdmin();
        roleLabel.setText(admin ? "ADMIN PANEL" : "STAFF PANEL");
        servicesNav.setVisible(admin);
        reportNav.setVisible(admin);
    }

    /** Colors the status badge: amber while processing, green when ready, grey once claimed. */
    public static void statusBadge(JLabel badge, Order order) {
        Color fg = components.Theme.WARNING_TEXT, bg = components.Theme.WARNING_BG, line = components.Theme.WARNING_BG;
        if (order.getStatus() == Order.READY) {
            fg = components.Theme.SUCCESS_TEXT;
            bg = components.Theme.ACCENT;
            line = components.Theme.ACCENT;
        } else if (order.getStatus() == Order.CLAIMED) {
            fg = components.Theme.SUCCESS_TEXT;
            bg = components.Theme.ACCENT;
            line = components.Theme.ACCENT;
        }
        badge.setText(order.getStatusName());
        badge.setForeground(fg);
        badge.setBackground(bg);
        badge.setBorder(new components.RoundedOutlineBorder(line, 1, 12));
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Metric Laundry", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void warn(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Metric Laundry", JOptionPane.WARNING_MESSAGE);
    }

    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, yyyy");
    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a");
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("MMM d, yyyy  h:mm a");

    /** "Payment Receipt" once staff have confirmed payment, otherwise a claim stub / order confirmation. */
    public static String documentName(Order order) {
        return order.isPaid() ? "Payment Receipt" : "Claim Stub";
    }

    public static void showReceipt(Component parent, Order order) {
        StringBuilder payment = new StringBuilder()
                .append(row("Method", order.getPaymentMethod()));
        if (!order.getPaymentReference().isEmpty()) {
            payment.append(row("Reference No.", escape(order.getPaymentReference())));
        }
        payment.append(row("Payment Status", order.getPaymentStatus()));
        if (order.isPaid()) {
            payment.append(row("Paid On", order.getPaidAt().format(DATE_TIME)));
        }
        payment.append("<tr><td><b>AMOUNT DUE</b></td><td align='right'><b>")
                .append(pesoHtml(order.getAmountDue())).append("</b></td></tr>");
        String footer = order.isPaid()
                ? "Thank you for choosing Metric Laundry!"
                : "Present this claim stub when you pick up your laundry.";
        String html = "<html><body style='width:290px; font-family:Segoe UI; font-size:11px'>"
                + "<div style='color:#1E8E3E; font-size:15px'><b>Metric Laundry</b></div>"
                + "<div style='color:#6B6B6B'>Kalawag 1, behind the Water District</div>"
                + "<div style='font-size:12px; margin-top:4px'><b>" + documentName(order).toUpperCase() + "</b></div><hr>"
                + "<table width='100%' cellpadding='2'>"
                + row("Order No.", order.getNumber())
                + row("Date Received", order.getCreatedAt().format(DATE_TIME))
                + row("Expected Completion", order.getExpectedCompletion().format(DATE))
                + row("Customer", escape(order.getCustomerName()))
                + row("Contact No.", escape(order.getCustomerPhone()))
                + "</table><hr><table width='100%' cellpadding='2'>"
                + row("Service", escape(order.getService()))
                + row("Weight", kg(order.getWeightKg()))
                + row("Rate", pesoHtml(order.getPricePerKg()) + " / kg")
                + "<tr><td><b>TOTAL CHARGE</b></td><td align='right' style='color:#1E8E3E; font-size:14px'><b>"
                + pesoHtml(order.getTotal()) + "</b></td></tr>"
                + "</table><hr><table width='100%' cellpadding='2'>" + payment
                + "</table><hr>" + row("Order Status", order.getStatusName()).replace("<tr>", "<table width='100%'><tr>")
                + "</table><br><div style='color:#6B6B6B'>" + footer + "</div></body></html>";
        JOptionPane.showMessageDialog(parent, new JLabel(html), documentName(order) + " " + order.getNumber(),
                JOptionPane.PLAIN_MESSAGE);
    }

    private static String row(String label, String value) {
        return "<tr><td style='color:#6B6B6B'>" + label + "</td><td align='right'>" + value + "</td></tr>";
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
