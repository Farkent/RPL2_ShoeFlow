package shoeflow.controller;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import shoeflow.config.AppContext;
import shoeflow.dao.ProductDao;
import shoeflow.dao.VariantDao;
import shoeflow.model.Product;
import shoeflow.model.User;
import shoeflow.model.Variant;
import shoeflow.util.DaoExceptions;
import shoeflow.util.Roles;

/**
 * Manajemen Produk: Master Model -> Varian & Stok. Hanya admin/manajer.
 * Akses data memakai SPRING DAO: controller hanya berisi validasi dan aturan tampilan,
 * sedangkan query/transaksi dikerjakan oleh ProductDao dan VariantDao (Hibernate yang dikelola Spring).
 * Signature method sengaja tetap "throws SQLException" supaya ProductPanel tidak perlu diubah.
 */
public class ProductController extends BaseController {

    public ProductController(User user) { super(user, Roles.ADMIN, Roles.MANAJER); }

    // ============================================================ akses DAO (disuntikkan oleh Spring)
    private static ProductDao products() { return AppContext.getBean(ProductDao.class); }
    private static VariantDao variants() { return AppContext.getBean(VariantDao.class); }

    /**
     * Menjalankan pekerjaan DAO. DataAccessException milik Spring diubah menjadi SQLException
     * (lengkap dengan kode error MySQL) agar penanganan di layar tidak berubah.
     * IllegalStateException/IllegalArgumentException (aturan bisnis) diteruskan apa adanya.
     */
    private static <T> T dao(Supplier<T> work) throws SQLException {
        try {
            return work.get();
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (RuntimeException e) {
            throw DaoExceptions.toSql(e);
        }
    }

    /** Mengisi field bantuan tampilan (brand, model, price) dari relasi product. */
    private static void fill(Variant v) {
        Product p = v.getProduct();
        v.setBrand(p.getBrand());
        v.setModel(p.getModel());
        v.setPrice(p.getPrice());
    }

    private static boolean blank(String s) { return s == null || s.trim().isEmpty(); }

    // ============================================================ Master Model
    public List<Product> listProducts(String keyword) throws SQLException {
        List<Product> list = dao(() -> products().findByKeyword(keyword));
        for (Product p : list) {            // hitung total stok & jumlah varian dari relasi
            int total = 0;
            for (Variant v : p.getVariants()) total += v.getStock();
            p.setTotalStock(total);
            p.setVariantCount(p.getVariants().size());
        }
        return list;
    }

    /** Insert (id = 0) atau update model sepatu. Validasi: tidak boleh kosong / harga harus positif. */
    public int saveProduct(final Product p) throws SQLException {
        if (blank(p.getBrand())) throw new IllegalArgumentException("Merek wajib diisi.");
        if (blank(p.getModel())) throw new IllegalArgumentException("Model wajib diisi.");
        if (p.getPrice() == null || p.getPrice().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Harga jual harus lebih dari 0 (tidak boleh negatif).");
        p.setBrand(p.getBrand().trim());
        p.setModel(p.getModel().trim());

        return dao(() -> {
            if (p.getId() == 0) return products().insert(p);
            products().update(p);
            return p.getId();
        });
    }

    /** Hapus model beserta variannya. Ditolak bila varian sudah dipakai transaksi/PO (foreign key). */
    public void deleteProduct(final int id) throws SQLException {
        try {
            dao(() -> { products().deleteById(id); return null; });
        } catch (SQLException e) {
            if (isInUse(e)) throw new IllegalStateException(
                    "Produk tidak bisa dihapus karena variannya sudah dipakai pada transaksi penjualan atau PO.");
            throw e;
        }
    }

    // ============================================================ Varian & Stok
    public List<Variant> listVariants(final int productId) throws SQLException {
        List<Variant> list = dao(() -> variants().findByProduct(productId));
        for (Variant v : list) fill(v);
        list.sort(Comparator.comparing(Variant::getSize).thenComparing(Variant::getColor));
        return list;
    }

    /** Seluruh varian semua model (untuk tabel Daftar Inventaris). */
    public List<Variant> listInventory(String keyword) throws SQLException {
        List<Variant> list = dao(() -> variants().findByKeyword(keyword));
        for (Variant v : list) fill(v);
        list.sort(Comparator.comparing(Variant::getBrand).thenComparing(Variant::getModel)
                .thenComparing(Variant::getSize).thenComparing(Variant::getColor));
        return list;
    }

    /** Insert (id = 0) atau update varian. Validasi anti angka negatif juga dilakukan di sini (lapis kedua). */
    public void saveVariant(final Variant v) throws SQLException {
        if (v.getProductId() <= 0) throw new IllegalArgumentException("Pilih model sepatu terlebih dahulu.");
        if (blank(v.getSize())) throw new IllegalArgumentException("Ukuran wajib diisi.");
        if (blank(v.getColor())) throw new IllegalArgumentException("Warna wajib diisi.");
        if (blank(v.getSku())) throw new IllegalArgumentException("SKU wajib diisi.");
        if (v.getStock() < 0) throw new IllegalArgumentException("Stok tidak boleh negatif.");
        if (v.getMinStock() < 0) throw new IllegalArgumentException("Stok minimum tidak boleh negatif.");
        v.setSize(v.getSize().trim());
        v.setColor(v.getColor().trim());
        v.setSku(v.getSku().trim());

        try {
            dao(() -> {
                if (v.getId() == 0) variants().insert(v); else variants().update(v);
                return null;
            });
        } catch (SQLException e) {
            if (isDuplicate(e)) throw new IllegalStateException("SKU '" + v.getSku() + "' sudah dipakai varian lain.");
            throw e;
        }
    }

    public void deleteVariant(final int id) throws SQLException {
        try {
            dao(() -> { variants().deleteById(id); return null; });
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
}
