package shoeflow.controller;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import shoeflow.config.Koneksi;
import shoeflow.model.User;
import shoeflow.util.Roles;

/** Data Dashboard (admin/manajer): ringkasan harian, grafik bulanan, dan alert stok minimum. */
public class DashboardController extends BaseController {

    /** Seluruh data dashboard dimuat dalam satu koneksi agar cepat (target < 3 detik). */
    public static class Data {
        public BigDecimal todayTotal = BigDecimal.ZERO;
        public int todayTrx;
        public int todayItems;
        public List<Object[]> lowStock = new ArrayList<>();   // SKU, produk, ukuran, warna, stok, min
        public Map<YearMonth, BigDecimal> monthly = new LinkedHashMap<>();   // 12 bulan terakhir
    }

    public DashboardController(User user) { super(user, Roles.ADMIN, Roles.MANAJER); }

    public Data load() throws SQLException {
        Data d = new Data();
        LocalDate today = LocalDate.now();
        try (Connection c = Koneksi.getConnection()) {
            // --- ringkasan penjualan hari ini
            String sum = "SELECT COUNT(*), COALESCE(SUM(total),0) FROM sales WHERE created_at >= ? AND created_at < ?";
            try (PreparedStatement ps = c.prepareStatement(sum)) {
                ps.setTimestamp(1, Timestamp.valueOf(today.atStartOfDay()));
                ps.setTimestamp(2, Timestamp.valueOf(today.plusDays(1).atStartOfDay()));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) { d.todayTrx = rs.getInt(1); d.todayTotal = rs.getBigDecimal(2); }
                }
            }
            String items = "SELECT COALESCE(SUM(si.qty),0) FROM sale_items si JOIN sales s ON s.id = si.sale_id "
                         + "WHERE s.created_at >= ? AND s.created_at < ?";
            try (PreparedStatement ps = c.prepareStatement(items)) {
                ps.setTimestamp(1, Timestamp.valueOf(today.atStartOfDay()));
                ps.setTimestamp(2, Timestamp.valueOf(today.plusDays(1).atStartOfDay()));
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) d.todayItems = rs.getInt(1); }
            }

            // --- grafik 12 bulan terakhir (bulan tanpa penjualan diisi 0)
            YearMonth start = YearMonth.from(today).minusMonths(11);
            for (int i = 0; i < 12; i++) d.monthly.put(start.plusMonths(i), BigDecimal.ZERO);
            String mon = "SELECT YEAR(created_at), MONTH(created_at), SUM(total) FROM sales WHERE created_at >= ? "
                       + "GROUP BY YEAR(created_at), MONTH(created_at)";
            try (PreparedStatement ps = c.prepareStatement(mon)) {
                ps.setTimestamp(1, Timestamp.valueOf(start.atDay(1).atStartOfDay()));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) d.monthly.put(YearMonth.of(rs.getInt(1), rs.getInt(2)), rs.getBigDecimal(3));
                }
            }

            // --- alert stok: stok <= batas minimum varian
            String low = "SELECT v.sku, CONCAT(p.brand,' ',p.model), v.size, v.color, v.stock, v.min_stock "
                       + "FROM variants v JOIN products p ON p.id = v.product_id "
                       + "WHERE v.stock <= v.min_stock ORDER BY v.stock ASC, p.brand, p.model";
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(low)) {
                while (rs.next()) {
                    d.lowStock.add(new Object[]{rs.getString(1), rs.getString(2), rs.getString(3),
                            rs.getString(4), rs.getInt(5), rs.getInt(6)});
                }
            }
        }
        return d;
    }
}
