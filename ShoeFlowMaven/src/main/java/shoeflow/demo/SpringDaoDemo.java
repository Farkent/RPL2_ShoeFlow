package shoeflow.demo;

import java.util.List;
import shoeflow.config.AppContext;
import shoeflow.dao.ProductDao;
import shoeflow.dao.UserDao;
import shoeflow.dao.VariantDao;
import shoeflow.model.Product;
import shoeflow.model.User;
import shoeflow.model.Variant;

/**
 * Demo Spring DAO untuk laporan (jalankan langsung, tanpa login):
 *
 *   Tanpa Spring (HibernateDemo)                  Dengan Spring DAO
 *   HibernateUtil.openSession() + tutup manual    AppContext.getBean(ProductDao.class)  (disuntikkan Spring)
 *   session.createQuery("from Product")           productDao.findByKeyword("")
 *   session.find(Product.class, 1)                productDao.findById(1)
 *   JDBC manual (Connection, ResultSet)           userDao.findByUsername("admin")  (JdbcTemplate)
 *
 * Cara menjalankan: klik kanan file ini > Run File (NetBeans) atau
 *   mvn compile exec:java -Dexec.mainClass=shoeflow.demo.SpringDaoDemo
 */
public class SpringDaoDemo {

    public static void main(String[] args) {
        try {
            ProductDao productDao = AppContext.getBean(ProductDao.class);
            VariantDao variantDao = AppContext.getBean(VariantDao.class);
            UserDao userDao = AppContext.getBean(UserDao.class);

            System.out.println("=== 1. ProductDao.findByKeyword(\"\") : semua produk (DAO Hibernate) ===");
            List<Product> all = productDao.findByKeyword("");
            for (Product p : all) {
                System.out.println(p.getId() + " | " + p.getName() + " | harga " + p.getPrice()
                        + " | jumlah varian " + p.getVariants().size());
            }

            System.out.println("\n=== 2. ProductDao.findById(1) : satu produk berdasarkan id ===");
            Product one = productDao.findById(1);
            System.out.println(one == null ? "Produk dengan id 1 tidak ditemukan." : "Ditemukan: " + one.getName());

            System.out.println("\n=== 3. VariantDao.findByProduct(1) : varian milik produk tersebut ===");
            for (Variant v : variantDao.findByProduct(1)) {
                System.out.println("  ukuran " + v.getSize() + " | " + v.getColor()
                        + " | SKU " + v.getSku() + " | stok " + v.getStock());
            }

            System.out.println("\n=== 4. UserDao.findByUsername(\"admin\") : DAO berbasis JdbcTemplate ===");
            User u = userDao.findByUsername("admin");
            System.out.println(u == null ? "User 'admin' tidak ditemukan." : "Ditemukan: " + u.getName() + " (role " + u.getRoleName() + ")");
        } finally {
            AppContext.shutdown();
        }
    }
}
