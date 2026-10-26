package shoeflow.view.components;

import javax.swing.table.DefaultTableModel;

/** Model tabel read-only; tipe kolom ditebak dari data sehingga BigDecimal/Integer/Timestamp dirender benar. */
public class ReadOnlyModel extends DefaultTableModel {
    public ReadOnlyModel(String... columns) { super(columns, 0); }

    @Override public boolean isCellEditable(int row, int col) { return false; }

    @Override public Class<?> getColumnClass(int col) {
        for (int r = 0; r < getRowCount(); r++) {
            Object v = getValueAt(r, col);
            if (v != null) return v.getClass();
        }
        return Object.class;
    }

    public void setRows(java.util.List<Object[]> rows) {
        setRowCount(0);
        for (Object[] r : rows) addRow(r);
    }
}
