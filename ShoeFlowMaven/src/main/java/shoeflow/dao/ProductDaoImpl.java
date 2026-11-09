package shoeflow.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import shoeflow.model.Product;

/**
 * Implementasi ProductDao dengan Hibernate (lewat JPA EntityManager) yang dikelola Spring.
 *  - @Repository   : bean DAO + exception Hibernate diubah menjadi DataAccessException.
 *  - @Transactional: Spring membuka transaksi sebelum method dan commit/rollback sesudahnya,
 *                    tidak perlu lagi beginTransaction()/commit()/rollback() manual.
 *  - @PersistenceContext: EntityManager disuntikkan (Dependency Injection) oleh Spring.
 */
@Repository
@Transactional
public class ProductDaoImpl implements ProductDao {

    @PersistenceContext
    private EntityManager em;

    private static String like(String keyword) {
        return "%" + (keyword == null ? "" : keyword.trim().toLowerCase()) + "%";
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> findByKeyword(String keyword) {
        return em.createQuery(
                "select distinct p from Product p left join fetch p.variants "
              + "where lower(p.brand) like :kw or lower(p.model) like :kw "
              + "or exists (select 1 from Variant x where x.product.id = p.id and lower(x.sku) like :kw) "
              + "order by p.brand, p.model", Product.class)
                .setParameter("kw", like(keyword))
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public Product findById(int id) {
        return em.find(Product.class, id);
    }

    @Override
    public int insert(Product product) {
        em.persist(product);    // INSERT
        em.flush();             // kirim ke database sekarang agar error (mis. constraint) langsung terdeteksi
        return product.getId();
    }

    @Override
    public void update(Product product) {
        Product m = em.find(Product.class, product.getId());
        if (m == null) throw new IllegalStateException("Model sepatu tidak ditemukan.");
        m.setBrand(product.getBrand());     // perubahan objek terdeteksi otomatis (dirty checking)
        m.setModel(product.getModel());
        m.setPrice(product.getPrice());
        m.setImage(product.getImage());
        em.flush();                         // UPDATE
    }

    @Override
    public void deleteById(int id) {
        Product p = em.find(Product.class, id);
        if (p != null) {
            em.remove(p);                   // DELETE (varian ikut terhapus lewat cascade)
            em.flush();                     // bila masih dipakai tabel lain, error foreign key muncul di sini
        }
    }
}
