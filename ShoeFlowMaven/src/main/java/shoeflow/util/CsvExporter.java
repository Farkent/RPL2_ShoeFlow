package shoeflow.util;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableModel;

/** Export isi tabel ke CSV (pemisah titik-koma agar langsung terbaca rapi di Excel Indonesia). */
public final class CsvExporter {
    private CsvExporter() { }

    /** @return file yang disimpan, atau null jika dibatalkan. */
    public static File export(Component parent, TableModel m, String defaultName) throws IOException {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Export data ke CSV");
        fc.setFileFilter(new FileNameExtensionFilter("File CSV (*.csv)", "csv"));
        fc.setSelectedFile(new File(defaultName + ".csv"));
        if (fc.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return null;
        File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".csv")) f = new File(f.getParentFile(), f.getName() + ".csv");

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write('\uFEFF');   // BOM agar Excel membaca UTF-8
            for (int c = 0; c < m.getColumnCount(); c++) w.write((c > 0 ? ";" : "") + cell(m.getColumnName(c)));
            w.write("\r\n");
            for (int r = 0; r < m.getRowCount(); r++) {
                for (int c = 0; c < m.getColumnCount(); c++) {
                    Object v = m.getValueAt(r, c);
                    String s;
                    if (v == null) s = "";
                    else if (v instanceof BigDecimal) s = ((BigDecimal) v).toPlainString();
                    else if (v instanceof java.util.Date) s = df.format((java.util.Date) v);
                    else s = v.toString();
                    w.write((c > 0 ? ";" : "") + cell(s));
                }
                w.write("\r\n");
            }
        }
        return f;
    }

    private static String cell(String s) {
        if (s.contains(";") || s.contains("\"") || s.contains("\n")) return "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }
}
