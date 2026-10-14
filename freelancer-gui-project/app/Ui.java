package app;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.Component;
import java.awt.Window;
import java.util.List;
import java.util.function.Function;

/** Small helpers shared by all screens. */
public final class Ui {

    private Ui() {
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Short, non-blocking message that fades out by itself. Falls back to a dialog if there is no main window. */
    public static void toast(Component parent, String message) {
        Window window = SwingUtilities.getWindowAncestor(parent);
        if (window instanceof MainFrame) {
            ((MainFrame) window).showToast(message);
        } else {
            info(parent, message);
        }
    }

    public static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            /** Reports the real type of each column so sorting works for numbers and dates too. */
            @Override
            public Class<?> getColumnClass(int column) {
                if (getRowCount() > 0 && getValueAt(0, column) != null) {
                    return getValueAt(0, column).getClass();
                }
                return Object.class;
            }
        };
    }

    /** Generic helper: clears a table and refills it from any list using a row-mapping function. */
    public static <T> void fill(DefaultTableModel model, List<T> items, Function<T, Object[]> rowMapper) {
        model.setRowCount(0);
        for (T item : items) {
            model.addRow(rowMapper.apply(item));
        }
    }

    public static String fmt(double value) {
        return String.format("%.2f", value);
    }
}
