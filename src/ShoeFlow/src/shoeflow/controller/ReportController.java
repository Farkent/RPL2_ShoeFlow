package shoeflow.controller;

import java.math.BigDecimal;
import java.sql.*;
import java.text.DateFormatSymbols;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import shoeflow.config.Koneksi;
import shoeflow.model.User;
import shoeflow.util.Roles;

/** Laporan (admin/manajer): Rekap Penjualan, Nilai Stok, dan Laba Rugi sederhana. */
public class ReportController extends BaseController {

    public static final String HARIAN = "Harian", MINGGUAN = "Mingguan", BULANAN = "Bulanan";

    /** Subquery HPP: rata-rata harga beli per varian dari PO yang SUDAH diterima. */
    private static final String COST_SUBQUERY =
            "(SELECT poi.variant_id, SUM(poi.subtotal) / SUM(poi.qty) AS avg_cost "
          + "FROM purchase_order_items poi JOIN purchase_orders po ON po.id = poi.po_id AND po.status = 'received' "
          + "GROUP BY poi.variant_id)";

    public ReportController(User user) { super(user, Roles.ADMIN, Roles.MANAJER); }

    private static void checkRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) throw new IllegalArgumentException("Rentang tanggal wajib diisi.");
        if (from.isAfter(to)) throw new IllegalArgumentException("Tanggal 'Dari' tidak boleh setelah tanggal 'Sampai'.");
    }

    /** Rekap penjualan per periode. Baris: [label, jumlah transaksi, item terjual, total penjualan]. */
    public List<Object[]> salesRecap(LocalDate from, LocalDate to, String period) throws SQLException {
        checkRange(from, to);
        String groupBy;
        if (MINGGUAN.equals(period)) groupBy = "YEARWEEK(s.created_at, 1)";
        else if (BULANAN.equals(period)) groupBy = "YEAR(s.created_at), MONTH(s.created_at)";
        else groupBy = "DATE(s.created_at)";

        String sql = "SELECT MIN(DATE(s.created_at)), COUNT(*), COALESCE(SUM(i.q),0), COALESCE(SUM(s.total),0) "
                   + "FROM sales s LEFT JOIN (SELECT sale_id, SUM(qty) AS q FROM sale_items GROUP BY sale_id) i ON i.sale_id = s.id "
                   + "WHERE s.created_at >= ? AND s.created_at < ? "
                   + "GROUP BY " + groupBy + " ORDER BY 1";
        List<Object[]> rows = new ArrayList<>();
        try (Connection c = Koneksi.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(from.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDate d = rs.getDate(1).toLocalDate();
                    String label;
                    if (MINGGUAN.equals(period)) label = "Minggu mulai " + d;
                    else if (BULANAN.equals(period))
                        label = new DateFormatSymbols(new Locale("id", "ID")).getMonths()[d.getMonthValue() - 1] + " " + d.getYear();
                    else label = d.toString();
                    rows.add(new Object[]{label, rs.getInt(2), rs.getInt(3), rs.getBigDecimal(4)});
                }
            }
        }
        return rows;
    }

    /**
     * Nilai stok saat ini (snapshot, tidak terikat rentang tanggal).
     * Baris: [SKU, produk, ukuran, warna, stok, harga jual, nilai (harga jual), HPP rata-rata, nilai (HPP)].
     */
    public List<Object[]> stockValue() throws SQLException {
        String sql = "SELECT v.sku, CONCAT(p.brand,' ',p.model), v.size, v.color, v.stock, p.price, "
                   + "v.stock * p.price, COALESCE(c.avg_cost,0), v.stock * COALESCE(c.avg_cost,0) "
                   + "FROM variants v JOIN products p ON p.id = v.product_id "
                   + "LEFT JOIN " + COST_SUBQUERY + " c ON c.variant_id = v.id "
                   + "ORDER BY p.brand, p.model, v.size, v.color";
        List<Object[]> rows = new ArrayList<>();
        try (Connection c = Koneksi.getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                rows.add(new Object[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getInt(5),
                        rs.getBigDecimal(6), rs.getBigDecimal(7), rs.getBigDecimal(8).setScale(0, java.math.RoundingMode.HALF_UP),
                        rs.getBigDecimal(9).setScale(0, java.math.RoundingMode.HALF_UP)});
            }
        }
        return rows;
    }

    /**
     * Laba rugi sederhana per model sepatu = pendapatan - HPP (laba kotor).
     * HPP = qty terjual x rata-rata harga beli PO yang sudah diterima (varian tanpa PO dianggap HPP 0).
     * Baris: [produk, qty terjual, pendapatan, HPP, laba kotor, margin %].
     */
    public List<Object[]> profitLoss(LocalDate from, LocalDate to) throws SQLException {
        checkRange(from, to);
        String sql = "SELECT CONCAT(p.brand,' ',p.model), SUM(si.qty), SUM(si.subtotal), "
                   + "SUM(si.qty * COALESCE(c.avg_cost,0)) "
                   + "FROM sale_items si JOIN sales s ON s.id = si.sale_id "
                   + "JOIN variants v ON v.id = si.variant_id JOIN products p ON p.id = v.product_id "
                   + "LEFT JOIN " + COST_SUBQUERY + " c ON c.variant_id = si.variant_id "
                   + "WHERE s.created_at >= ? AND s.created_at < ? "
                   + "GROUP BY p.id, p.brand, p.model ORDER BY 3 DESC";
        List<Object[]> rows = new ArrayList<>();
        try (Connection c = Koneksi.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(from.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BigDecimal revenue = rs.getBigDecimal(3);
                    BigDecimal hpp = rs.getBigDecimal(4).setScale(0, java.math.RoundingMode.HALF_UP);
                    BigDecimal profit = revenue.subtract(hpp);
                    String margin = revenue.signum() == 0 ? "0,0%"
                            : String.format("%.1f%%", profit.multiply(BigDecimal.valueOf(100))
                                    .divide(revenue, 4, java.math.RoundingMode.HALF_UP).doubleValue()).replace('.', ',');
                    rows.add(new Object[]{rs.getString(1), rs.getInt(2), revenue, hpp, profit, margin});
                }
            }
        }
        return rows;
    }
}
