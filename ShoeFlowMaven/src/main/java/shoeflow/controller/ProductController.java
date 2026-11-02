package shoeflow.controller;

import jakarta.persistence.PersistenceException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.Transaction;
import shoeflow.model.Product;
import shoeflow.model.User;
import shoeflow.model.Variant;
import shoeflow.util.HibernateUtil;
import shoeflow.util.Roles;

/**
 * Manajemen Produk: Master Model -> Varian & Stok. Hanya admin/manajer.
 * Akses data memakai HIBERNATE (ORM): tidak ada SQL manual, cukup mengelola objek Product dan Variant.
 * Signature method sengaja tetap "throws SQLException" supaya ProductPanel tidak perlu diubah.
 */
public class ProductController extends BaseController {

    public ProductController(User user) { super(user, Roles.ADMIN, Roles.MANAJER); }

    // ============================================================ infrastruktur transaksi Hibernate
    /** Satu unit pekerjaan yang dijalankan di dalam Session + Transaction. */
    private interface Work<T> { T run(Session s); }

    /**
     * Membuka Session, memulai transaksi, menjalankan pekerjaan, lalu commit.
     * Jika terjadi error -> rollback. Error Hibernate diubah menjadi SQLException
     * (lengkap dengan kode error MySQL) agar penanganan di layar tidak berubah.
     */
    private static <T> T inTx(Work<T> work) throws SQLException {
        try (Session s = HibernateUtil.openSession()) {
            Transaction tx = s.beginTransaction();
            try {
                T result = work.run(s);
                tx.commit();
                return result;
            } catch (RuntimeException e) {
                rollback(tx);
                throw e;
            }
        } catch (PersistenceException e) {
            throw toSql(e);
        }
    }

    private static void rollback(Transaction tx) {
        try { if (tx != null && tx.isActive()) tx.rollback(); } catch (RuntimeException ignore) { }
    }

    /** Mencari SQLException asli di dalam rantai penyebab, lalu membungkusnya dengan kode error yang sama. */
    private static SQLException toSql(PersistenceException e) {
        Throwable t = e;
        while (t != null) {
            if (t instanceof SQLException) {
                SQLException orig = (SQLException) t;
                return new SQLException(orig.getMessage(), orig.getSQLState(), orig.getErrorCode(), e);
            }
            t = t.getCause();
        }
        return new SQLException(e.getMessage(), e);
    }

    private static String like(String keyword) {
        return "%" + (keyword == null ? "" : keyword.trim().toLowerCase()) + "%";
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
    /** Setara SQL: SELECT ... FROM products LEFT JOIN variants ... (ditulis dengan HQL, bukan SQL). */
    public List<Product> listProducts(String keyword) throws SQLException {
        final String kw = like(keyword);
        return inTx(s -> {
            List<Product> list = s.createQuery(
                    "select distinct p from Product p left join fetch p.variants "
                  + "where lower(p.brand) like :kw or lower(p.model) like :kw "
                  + "or exists (select 1 from Variant x where x.product.id = p.id and lower(x.sku) like :kw) "
                  + "order by p.brand, p.model", Product.class)
                    .setParameter("kw", kw)
                    .getResultList();
            for (Product p : list) {            // hitung total stok & jumlah varian dari relasi
                int total = 0;
                for (Variant v : p.getVariants()) total += v.getStock();
                p.setTotalStock(total);
                p.setVariantCount(p.getVariants().size());
            }
            return list;
        });
    }

    /** Insert (id = 0) atau update model sepatu. Validasi: tidak boleh kosong / harga harus positif. */
    public int saveProduct(final Product p) throws SQLException {
        if (blank(p.getBrand())) throw new IllegalArgumentException("Merek wajib diisi.");
        if (blank(p.getModel())) throw new IllegalArgumentException("Model wajib diisi.");
        if (p.getPrice() == null || p.getPrice().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Harga jual harus lebih dari 0 (tidak boleh negatif).");
        final String brand = p.getBrand().trim(), model = p.getModel().trim();

        return inTx(s -> {
            if (p.getId() == 0) {
                p.setBrand(brand);
                p.setModel(model);
                s.persist(p);                                   // INSERT
            } else {
                Product m = s.find(Product.class, p.getId());   // SELECT berdasarkan primary key
                if (m == null) throw new IllegalStateException("Model sepatu tidak ditemukan.");
                m.setBrand(brand);                              // perubahan objek terdeteksi otomatis
                m.setModel(model);                              // (dirty checking) -> UPDATE saat commit
                m.setPrice(p.getPrice());
                m.setImage(p.getImage());
            }
            return p.getId();
        });
    }

    /** Hapus model beserta variannya. Ditolak bila varian sudah dipakai transaksi/PO (foreign key). */
    public void deleteProduct(final int id) throws SQLException {
        try {
            inTx(s -> {
                Product p = s.find(Product.class, id);
                if (p != null) s.remove(p);                     // DELETE (varian ikut terhapus via cascade)
                return null;
            });
        } catch (SQLException e) {
            if (isInUse(e)) throw new IllegalStateException(
                    "Produk tidak bisa dihapus karena variannya sudah dipakai pada transaksi penjualan atau PO.");
            throw e;
        }
    }

    // ============================================================ Varian & Stok
    public List<Variant> listVariants(final int productId) throws SQLException {
        return inTx(s -> {
            List<Variant> list = s.createQuery(
                    "select v from Variant v join fetch v.product p where p.id = :pid", Variant.class)
                    .setParameter("pid", productId)
                    .getResultList();
            for (Variant v : list) fill(v);
            list.sort(Comparator.comparing(Variant::getSize).thenComparing(Variant::getColor));
            return list;
        });
    }

    /** Seluruh varian semua model (untuk tabel Daftar Inventaris). */
    public List<Variant> listInventory(String keyword) throws SQLException {
        final String kw = like(keyword);
        return inTx(s -> {
            List<Variant> list = s.createQuery(
                    "select v from Variant v join fetch v.product p "
                  + "where lower(p.brand) like :kw or lower(p.model) like :kw "
                  + "or lower(v.sku) like :kw or lower(v.color) like :kw", Variant.class)
                    .setParameter("kw", kw)
                    .getResultList();
            for (Variant v : list) fill(v);
            list.sort(Comparator.comparing(Variant::getBrand).thenComparing(Variant::getModel)
                    .thenComparing(Variant::getSize).thenComparing(Variant::getColor));
            return list;
        });
    }

    /** Insert (id = 0) atau update varian. Validasi anti angka negatif juga dilakukan di sini (lapis kedua). */
    public void saveVariant(final Variant v) throws SQLException {
        if (v.getProductId() <= 0) throw new IllegalArgumentException("Pilih model sepatu terlebih dahulu.");
        if (blank(v.getSize())) throw new IllegalArgumentException("Ukuran wajib diisi.");
        if (blank(v.getColor())) throw new IllegalArgumentException("Warna wajib diisi.");
        if (blank(v.getSku())) throw new IllegalArgumentException("SKU wajib diisi.");
        if (v.getStock() < 0) throw new IllegalArgumentException("Stok tidak boleh negatif.");
        if (v.getMinStock() < 0) throw new IllegalArgumentException("Stok minimum tidak boleh negatif.");

        try {
            inTx(s -> {
                Product parent = s.getReference(Product.class, v.getProductId());   // relasi ke model induk
                if (v.getId() == 0) {
                    v.setProduct(parent);
                    v.setSize(v.getSize().trim());
                    v.setColor(v.getColor().trim());
                    v.setSku(v.getSku().trim());
                    s.persist(v);                                                     // INSERT
                } else {
                    Variant m = s.find(Variant.class, v.getId());
                    if (m == null) throw new IllegalStateException("Varian tidak ditemukan.");
                    m.setProduct(parent);
                    m.setSize(v.getSize().trim());                                    // UPDATE otomatis saat commit
                    m.setColor(v.getColor().trim());
                    m.setSku(v.getSku().trim());
                    m.setStock(v.getStock());
                    m.setMinStock(v.getMinStock());
                }
                return null;
            });
        } catch (SQLException e) {
            if (isDuplicate(e)) throw new IllegalStateException("SKU '" + v.getSku() + "' sudah dipakai varian lain.");
            throw e;
        }
    }

    public void deleteVariant(final int id) throws SQLException {
        try {
            inTx(s -> {
                Variant v = s.find(Variant.class, id);
                if (v != null) s.remove(v);                     // DELETE
                return null;
            });
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
