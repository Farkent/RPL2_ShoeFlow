package shoeflow.demo;

import java.util.List;
import org.hibernate.Session;
import shoeflow.model.Product;
import shoeflow.model.Variant;
import shoeflow.util.HibernateUtil;

/**
 * Demo ORM untuk laporan (jalankan langsung, tanpa login):
 *
 *   Tanpa ORM (JDBC)                         Dengan ORM (Hibernate)
 *   SELECT * FROM products;            ->    Product.all   : createQuery("from Product")
 *   SELECT * FROM products WHERE id=1; ->    Product.find(1): session.find(Product.class, 1)
 *
 * Cara menjalankan: klik kanan file ini > Run File (NetBeans) atau
 *   mvn compile exec:java -Dexec.mainClass=shoeflow.demo.HibernateDemo
 */
public class HibernateDemo {

    public static void main(String[] args) {
        try (Session session = HibernateUtil.openSession()) {

            System.out.println("=== 1. Ambil semua produk (setara SELECT * FROM products) ===");
            List<Product> all = session.createQuery("from Product", Product.class).getResultList();
            for (Product p : all) {
                System.out.println(p.getId() + " | " + p.getName() + " | harga " + p.getPrice());
            }

            System.out.println("\n=== 2. Cari satu produk berdasarkan id (setara WHERE id = 1) ===");
            Product one = session.find(Product.class, 1);
            if (one == null) {
                System.out.println("Produk dengan id 1 tidak ditemukan.");
            } else {
                System.out.println("Ditemukan: " + one.getName());

                System.out.println("\n=== 3. Relasi 1:N: varian dari produk tersebut (tanpa JOIN manual) ===");
                for (Variant v : one.getVariants()) {   // varian dimuat otomatis oleh Hibernate
                    System.out.println("  ukuran " + v.getSize() + " | " + v.getColor()
                            + " | SKU " + v.getSku() + " | stok " + v.getStock());
                }
            }
        } finally {
            HibernateUtil.shutdown();
        }
    }
}
