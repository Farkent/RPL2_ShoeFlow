package shoeflow.controller;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import shoeflow.config.Koneksi;
import shoeflow.model.Product;
import shoeflow.model.User;
import shoeflow.model.Variant;
import shoeflow.util.Roles;

/** Manajemen Produk: Master Model -> Varian & Stok. Hanya admin/manajer. */
public class ProductController extends BaseController {

    public ProductController(User user) { super(user, Roles.ADMIN, Roles.MANAJER); }

    // ------------------------------------------------------------ Master Model
    public List<Product> listProducts(String keyword) throws SQLException {
        return queryProducts(keyword, 1000);
    }

    /** Insert (id = 0) atau update model sepatu. Validasi: tidak boleh kosong / harga harus positif. */
    public int saveProduct(Product p) throws SQLException {
        if (p.getBrand() == null || p.getBrand().trim().isEmpty()) throw new IllegalArgumentException("Merek wajib diisi.");
        if (p.getModel() == null || p.getModel().trim().isEmpty()) throw new IllegalArgumentException("Model wajib diisi.");
        if (p.getPrice() == null || p.getPrice().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Harga jual harus lebih dari 0 (tidak boleh negatif).");

        try (Connection c = Koneksi.getConnection()) {
            if (p.getId() == 0) {
                String sql = "INSERT INTO products (brand, model, price, image) VALUES (?,?,?,?)";
                try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, p.getBrand().trim());
                    ps.setString(2, p.getModel().trim());
                    ps.setBigDecimal(3, p.getPrice());
                    ps.setString(4, p.getImage());
                    ps.executeUpdate();
                    try (ResultSet k = ps.getGeneratedKeys()) { k.next(); p.setId(k.getInt(1)); }
                }
            } else {
                String sql = "UPDATE products SET brand=?, model=?, price=?, image=? WHERE id=?";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setString(1, p.getBrand().trim());
                    ps.setString(2, p.getModel().trim());
                    ps.setBigDecimal(3, p.getPrice());
                    ps.setString(4, p.getImage());
                    ps.setInt(5, p.getId());
                    ps.executeUpdate();
                }
            }
        }
        return p.getId();
    }

    /** Hapus model beserta variannya (ON DELETE CASCADE). Ditolak bila varian sudah dipakai transaksi/PO. */
    public void deleteProduct(int id) throws SQLException {
        try (Connection c = Koneksi.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM products WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (isInUse(e)) throw new IllegalStateException(
                    "Produk tidak bisa dihapus karena variannya sudah dipakai pada transaksi penjualan atau PO.");
            throw e;
        }
    }

    // ------------------------------------------------------------ Varian & Stok
    public List<Variant> listVariants(int productId) throws SQLException {
        List<Variant> list = new ArrayList<>();
        try (Connection c = Koneksi.getConnection();
             PreparedStatement ps = c.prepareStatement(VARIANT_SELECT + "WHERE v.product_id = ? ORDER BY v.size, v.color")) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapVariant(rs)); }
        }
        return list;
    }

    /** Seluruh varian semua model (untuk tabel Daftar Inventaris). */
    public List<Variant> listInventory(String keyword) throws SQLException {
        String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        String sql = VARIANT_SELECT + "WHERE p.brand LIKE ? OR p.model LIKE ? OR v.sku LIKE ? OR v.color LIKE ? "
                   + "ORDER BY p.brand, p.model, v.size, v.color";
        List<Variant> list = new ArrayList<>();
        try (Connection c = Koneksi.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 1; i <= 4; i++) ps.setString(i, like);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapVariant(rs)); }
        }
        return list;
    }

    /** Insert (id = 0) atau update varian. Validasi anti angka negatif juga dilakukan di sini (lapis kedua). */
    public void saveVariant(Variant v) throws SQLException {
        if (v.getProductId() <= 0) throw new IllegalArgumentException("Pilih model sepatu terlebih dahulu.");
        if (blank(v.getSize())) throw new IllegalArgumentException("Ukuran wajib diisi.");
        if (blank(v.getColor())) throw new IllegalArgumentException("Warna wajib diisi.");
        if (blank(v.getSku())) throw new IllegalArgumentException("SKU wajib diisi.");
        if (v.getStock() < 0) throw new IllegalArgumentException("Stok tidak boleh negatif.");
        if (v.getMinStock() < 0) throw new IllegalArgumentException("Stok minimum tidak boleh negatif.");

        try (Connection c = Koneksi.getConnection()) {
            if (v.getId() == 0) {
                String sql = "INSERT INTO variants (product_id, size, color, sku, stock, min_stock) VALUES (?,?,?,?,?,?)";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    bind(ps, v);
                    ps.executeUpdate();
                }
            } else {
                String sql = "UPDATE variants SET product_id=?, size=?, color=?, sku=?, stock=?, min_stock=? WHERE id=?";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    bind(ps, v);
                    ps.setInt(7, v.getId());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            if (isDuplicate(e)) throw new IllegalStateException("SKU '" + v.getSku() + "' sudah dipakai varian lain.");
            throw e;
        }
    }

    public void deleteVariant(int id) throws SQLException {
        try (Connection c = Koneksi.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM variants WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (isInUse(e)) throw new IllegalStateException(
                    "Varian tidak bisa dihapus karena sudah dipakai pada transaksi penjualan atau PO.");
            throw e;
        }
    }

    /** Saran SKU otomatis, contoh: NIK-AIRMAX-42-HITAM. */
    public static String suggestSku(Product p, String size, String color) {
        return (clean(p.getBrand(), 3) + "-" + clean(p.getModel(), 8) + "-" + clean(size, 5) + "-" + clean(color, 5));
    }

    private static String clean(String s, int max) {
        String x = s == null ? "" : s.toUpperCase().replaceAll("[^A-Z0-9]", "");
        return x.length() > max ? x.substring(0, max) : x;
    }

    private static boolean blank(String s) { return s == null || s.trim().isEmpty(); }

    private static void bind(PreparedStatement ps, Variant v) throws SQLException {
        ps.setInt(1, v.getProductId());
        ps.setString(2, v.getSize().trim());
        ps.setString(3, v.getColor().trim());
        ps.setString(4, v.getSku().trim());
        ps.setInt(5, v.getStock());
        ps.setInt(6, v.getMinStock());
    }
}
