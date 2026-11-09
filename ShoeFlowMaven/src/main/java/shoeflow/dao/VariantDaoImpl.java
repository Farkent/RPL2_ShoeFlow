package shoeflow.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import shoeflow.model.Product;
import shoeflow.model.Variant;

/** Implementasi VariantDao dengan Hibernate yang dikelola Spring (lihat penjelasan di ProductDaoImpl). */
@Repository
@Transactional
public class VariantDaoImpl implements VariantDao {

    @PersistenceContext
    private EntityManager em;

    private static String like(String keyword) {
        return "%" + (keyword == null ? "" : keyword.trim().toLowerCase()) + "%";
    }

    @Override
    @Transactional(readOnly = true)
    public List<Variant> findByProduct(int productId) {
        return em.createQuery(
                "select v from Variant v join fetch v.product p where p.id = :pid", Variant.class)
                .setParameter("pid", productId)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Variant> findByKeyword(String keyword) {
        return em.createQuery(
                "select v from Variant v join fetch v.product p "
              + "where lower(p.brand) like :kw or lower(p.model) like :kw "
              + "or lower(v.sku) like :kw or lower(v.color) like :kw", Variant.class)
                .setParameter("kw", like(keyword))
                .getResultList();
    }

    @Override
    public void insert(Variant variant) {
        Product parent = em.getReference(Product.class, variant.getProductId());   // relasi ke model induk
        variant.setProduct(parent);
        em.persist(variant);                                                        // INSERT
        em.flush();                                                                 // SKU kembar langsung terdeteksi
    }

    @Override
    public void update(Variant variant) {
        Variant m = em.find(Variant.class, variant.getId());
        if (m == null) throw new IllegalStateException("Varian tidak ditemukan.");
        m.setProduct(em.getReference(Product.class, variant.getProductId()));
        m.setSize(variant.getSize());       // UPDATE otomatis (dirty checking)
        m.setColor(variant.getColor());
        m.setSku(variant.getSku());
        m.setStock(variant.getStock());
        m.setMinStock(variant.getMinStock());
        em.flush();
    }

    @Override
    public void deleteById(int id) {
        Variant v = em.find(Variant.class, id);
        if (v != null) {
            em.remove(v);                   // DELETE
            em.flush();
        }
    }
}
