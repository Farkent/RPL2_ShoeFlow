package shoeflow.view;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import javax.swing.*;
import shoeflow.controller.ReportController;
import shoeflow.model.User;
import shoeflow.util.CsvExporter;
import shoeflow.util.Rupiah;
import shoeflow.view.components.*;

/** Laporan (admin/manajer): Rekap Penjualan, Nilai Stok, Laba Rugi. Ada filter rentang tanggal & export CSV. */
public class ReportPanel extends JPanel implements Refreshable {

    private final ReportController ctl;
    private final JSpinner spFrom = UiUtil.dateSpinner(LocalDate.now().withDayOfMonth(1));
    private final JSpinner spTo = UiUtil.dateSpinner(LocalDate.now());
    private final JComboBox<String> cmbPeriod = new JComboBox<>(new String[]{
            ReportController.HARIAN, ReportController.MINGGUAN, ReportController.BULANAN});
    private final JTabbedPane tabs = new JTabbedPane();

    private final ReadOnlyModel recapModel = new ReadOnlyModel("Periode", "Jumlah Transaksi", "Item Terjual", "Total Penjualan");
    private final ReadOnlyModel stockModel = new ReadOnlyModel("SKU", "Produk", "Ukuran", "Warna", "Stok", "Harga Jual",
            "Nilai Stok (Harga Jual)", "HPP Rata-rata", "Nilai Stok (HPP)");
    private final ReadOnlyModel plModel = new ReadOnlyModel("Produk", "Qty Terjual", "Pendapatan", "HPP", "Laba Kotor", "Margin");
    private final JTable recapTable = UiUtil.style(new JTable(recapModel));
    private final JTable stockTable = UiUtil.style(new JTable(stockModel));
    private final JTable plTable = UiUtil.style(new JTable(plModel));
    private final JLabel lblRecap = new JLabel(" "), lblStock = new JLabel(" "), lblPl = new JLabel(" ");

    public ReportPanel(User user) {
        ctl = new ReportController(user);   // SecurityException untuk kasir
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(18, 20, 20, 20));
        setBackground(UiUtil.BG);
        add(UiUtil.title("Laporan Bisnis & Analisis Keuangan"), BorderLayout.NORTH);

        JButton bShow = UiUtil.button("Tampilkan", UiUtil.ACCENT);
        JButton bExport = UiUtil.button("Export CSV (Tab Aktif)", UiUtil.SUCCESS);
        bShow.addActionListener(e -> refresh());
        bExport.addActionListener(e -> export());

        UiUtil.styleCombo(cmbPeriod);

        UiUtil.NeoPanel tb = new UiUtil.NeoPanel(new FlowLayout(FlowLayout.LEFT, 10, 8), Color.WHITE, 3, 0);
        tb.setPadding(6, 12, 6, 12);
        JLabel lFrom = new JLabel("Dari:"); lFrom.setFont(UiUtil.FONT_BOLD);
        JLabel lTo = new JLabel("Sampai:"); lTo.setFont(UiUtil.FONT_BOLD);
        JLabel lPeriod = new JLabel("Periode Rekap:"); lPeriod.setFont(UiUtil.FONT_BOLD);

        tb.add(lFrom); tb.add(spFrom);
        tb.add(lTo); tb.add(spTo);
        tb.add(lPeriod); tb.add(cmbPeriod);
        tb.add(bShow); tb.add(bExport);

        tabs.addTab("Rekap Penjualan", tab(recapTable, lblRecap));
        tabs.addTab("Nilai Stok Barang", tab(stockTable, lblStock));
        tabs.addTab("Laba Rugi", tab(plTable, lblPl));

        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        center.add(tb, BorderLayout.NORTH);
        center.add(tabs, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    private JPanel tab(JTable t, JLabel summary) {
        summary.setFont(UiUtil.FONT_BOLD.deriveFont(13f));
        summary.setForeground(UiUtil.BORDER_DARK);

        UiUtil.NeoPanel summaryBox = new UiUtil.NeoPanel(new BorderLayout(), UiUtil.ACCENT, 3, 0);
        summaryBox.setPadding(8, 12, 8, 12);
        summaryBox.add(summary, BorderLayout.CENTER);

        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        p.add(UiUtil.horizontalScroll(t), BorderLayout.CENTER);
        p.add(summaryBox, BorderLayout.SOUTH);
        return p;
    }

    @Override public void refresh() {
        LocalDate from = UiUtil.toLocalDate(spFrom), to = UiUtil.toLocalDate(spTo);
        if (from.isAfter(to)) { UiUtil.warn(this, "Tanggal 'Dari' tidak boleh setelah tanggal 'Sampai'."); return; }
        try {
            // --- Rekap Penjualan
            List<Object[]> recap = ctl.salesRecap(from, to, (String) cmbPeriod.getSelectedItem());
            recapModel.setRows(recap);
            BigDecimal sumSales = BigDecimal.ZERO; int trx = 0, items = 0;
            for (Object[] r : recap) { trx += (Integer) r[1]; items += (Integer) r[2]; sumSales = sumSales.add((BigDecimal) r[3]); }
            lblRecap.setText("TOTAL: " + trx + " transaksi | " + items + " pasang | " + Rupiah.format(sumSales));

            // --- Nilai Stok (snapshot saat ini)
            List<Object[]> stock = ctl.stockValue();
            stockModel.setRows(stock);
            BigDecimal vSell = BigDecimal.ZERO, vCost = BigDecimal.ZERO; int qty = 0;
            for (Object[] r : stock) { qty += (Integer) r[4]; vSell = vSell.add((BigDecimal) r[6]); vCost = vCost.add((BigDecimal) r[8]); }
            lblStock.setText("TOTAL STOK: " + qty + " pasang | Nilai (harga jual): " + Rupiah.format(vSell)
                    + " | Nilai (HPP): " + Rupiah.format(vCost) + "   [posisi stok saat ini, tidak terikat rentang tanggal]");

            // --- Laba Rugi
            List<Object[]> pl = ctl.profitLoss(from, to);
            plModel.setRows(pl);
            BigDecimal rev = BigDecimal.ZERO, hpp = BigDecimal.ZERO;
            for (Object[] r : pl) { rev = rev.add((BigDecimal) r[2]); hpp = hpp.add((BigDecimal) r[3]); }
            lblPl.setText("Pendapatan " + Rupiah.format(rev) + " - HPP " + Rupiah.format(hpp) + " = LABA KOTOR "
                    + Rupiah.format(rev.subtract(hpp)) + "   [HPP = rata-rata harga beli PO yang sudah diterima]");

            UiUtil.packColumns(recapTable); UiUtil.packColumns(stockTable); UiUtil.packColumns(plTable);
        } catch (IllegalArgumentException ex) {
            UiUtil.warn(this, ex.getMessage());
        } catch (SQLException ex) { UiUtil.dbError(this, ex); }
    }

    private void export() {
        int i = tabs.getSelectedIndex();
        ReadOnlyModel m = i == 0 ? recapModel : i == 1 ? stockModel : plModel;
        String name = i == 0 ? "rekap_penjualan" : i == 1 ? "nilai_stok" : "laba_rugi";
        try {
            File f = CsvExporter.export(this, m, name + "_" + LocalDate.now());
            if (f != null) UiUtil.info(this, "Data berhasil diexport ke:\n" + f.getAbsolutePath());
        } catch (IOException ex) { UiUtil.warn(this, "Gagal export: " + ex.getMessage()); }
    }
}
