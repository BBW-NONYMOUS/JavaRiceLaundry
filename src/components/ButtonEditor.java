package components;

import java.awt.Component;
import java.util.function.IntConsumer;
import javax.swing.*;
import javax.swing.table.TableCellEditor;

/** Supplies the model row to the action, including when a JTable is sorted. */
public class ButtonEditor extends AbstractCellEditor implements TableCellEditor {
    private final RoundedButton button = new RoundedButton();
    private int modelRow;
    private final IntConsumer action;
    public ButtonEditor(IntConsumer action) {
        this.action = java.util.Objects.requireNonNull(action);
        button.addActionListener(e -> { fireEditingStopped(); this.action.accept(modelRow); });
    }
    @Override public Component getTableCellEditorComponent(JTable table, Object value,
            boolean selected, int row, int column) {
        modelRow = table.convertRowIndexToModel(row);
        button.setText(value == null ? "View Details" : value.toString()); return button;
    }
    @Override public Object getCellEditorValue() { return button.getText(); }
}
