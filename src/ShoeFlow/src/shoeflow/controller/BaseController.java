package shoeflow.controller;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import shoeflow.model.Product;
import shoeflow.model.User;
import shoeflow.model.Variant;

/**
 * Induk semua controller. Menerapkan Role-Based Access Control (RBAC) di sisi logika:
 * controller menolak dibuat jika role pengguna tidak berhak, jadi akses tidak hanya
 * disembunyikan di menu tetapi benar-benar diblokir.
 */
public abstract class BaseController {

    protected final User user;

    protected BaseController(User user, String... allowedRoles) {
        if (user == null) throw new SecurityException("Belum login.");
        boolean ok = false;
        for (String r : allowedRoles) if (r.equalsIgnoreCase(user.getRoleName())) ok = true;
        if (!ok) throw new SecurityException("Akses ditolak untuk role '" + user.getRoleName() + "'.");
        this.user = user;
    }

    // ------------------------------------------------------------ helper SQL bersama
    protected static final String VARIANT_SELECT =
            "SELECT v.id, v.product_id, v.size, v.color, v.sku, v.stock, v.min_stock, p.brand, p.model, p.price "
          + "FROM variants v JOIN products p ON p.id = v.product_id ";

    protected static Variant mapVariant(ResultSet rs) throws SQLException {
        Variant v = new Variant();
        v.setId(rs.getInt("id"));
        v.setProductId(rs.getInt("product_id"));
        v.setSize(rs.getString("size"));
        v.setColor(rs.getString("color"));
        v.setSku(rs.getString("sku"));
        v.setStock(rs.getInt("stock"));
        v.setMinStock(rs.getInt("min_stock"));
        v.setBrand(rs.getString("brand"));
        v.setModel(rs.getString("model"));
        v.setPrice(rs.getBigDecimal("price"));
        return v;
    }

    /** Pencarian produk (merek / model / SKU varian) beserta total stok & jumlah varian. */
    protected static List<Product> queryProducts(String keyword, int limit) throws SQLException {
        String sql = "SELECT p.id, p.brand, p.model, p.price, p.image, p.created_at, "
                   + "COALESCE(SUM(v.stock),0) AS ts, COUNT(v.id) AS vc "
                   + "FROM products p LEFT JOIN variants v ON v.product_id = p.id "
                   + "WHERE (p.brand LIKE ? OR p.model LIKE ? OR EXISTS "
                   + "(SELECT 1 FROM variants x WHERE x.product_id = p.id AND x.sku LIKE ?)) "
                   + "GROUP BY p.id, p.brand, p.model, p.price, p.image, p.created_at "
                   + "ORDER BY p.brand, p.model LIMIT " + limit;
        String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        List<Product> list = new ArrayList<>();
        try (Connection c = shoeflow.config.Koneksi.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, like); ps.setString(2, like); ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Product p = new Product(rs.getInt("id"), rs.getString("brand"), rs.getString("model"),
                            rs.getBigDecimal("price"), rs.getString("image"), rs.getTimestamp("created_at"));
                    p.setTotalStock(rs.getInt("ts"));
                    p.setVariantCount(rs.getInt("vc"));
                    list.add(p);
                }
            }
        }
        return list;
    }

    protected static void rollbackQuietly(Connection c) {
        if (c != null) try { c.rollback(); } catch (SQLException ignore) { }
    }

    protected static void closeQuietly(Connection c) {
        if (c != null) try { c.setAutoCommit(true); c.close(); } catch (SQLException ignore) { }
    }

    /** Kode MySQL 1062 = duplicate entry (mis. SKU kembar). */
    protected static boolean isDuplicate(SQLException e) { return e.getErrorCode() == 1062; }

    /** Kode 1451 = baris masih direferensikan tabel lain (foreign key). */
    protected static boolean isInUse(SQLException e) { return e.getErrorCode() == 1451; }
}
