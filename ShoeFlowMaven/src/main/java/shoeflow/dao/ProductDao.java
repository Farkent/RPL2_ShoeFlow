package shoeflow.dao;

import java.util.List;
import shoeflow.model.Product;

/** DAO untuk entity Product (master model sepatu). Controller hanya mengenal interface ini, bukan Hibernate. */
public interface ProductDao {

    /** Cari berdasarkan merek / model / SKU varian (tanpa membedakan huruf besar-kecil); keyword kosong = semua. */
    List<Product> findByKeyword(String keyword);

    Product findById(int id);

    /** INSERT produk baru, mengembalikan id hasil AUTO_INCREMENT. */
    int insert(Product product);

    /** UPDATE merek, model, harga, dan gambar produk yang id-nya sudah ada. */
    void update(Product product);

    /** DELETE produk beserta variannya (cascade). */
    void deleteById(int id);
}
