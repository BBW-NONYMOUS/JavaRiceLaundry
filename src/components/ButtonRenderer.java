package components;

import java.awt.Component;
import javax.swing.*;
import javax.swing.table.TableCellRenderer;

/** Pair with ButtonEditor to make a table action both visible and actionable. */
public class ButtonRenderer extends RoundedButton implements TableCellRenderer {
    @Override public Component getTableCellRendererComponent(JTable table, Object value,
            boolean selected, boolean focused, int row, int column) {
        setText(value == null ? "View Details" : value.toString()); return this;
    }
}
