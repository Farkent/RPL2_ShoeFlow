package shoeflow.dao;

import java.util.List;
import shoeflow.model.Variant;

/** DAO untuk entity Variant (ukuran, warna, SKU, dan stok). */
public interface VariantDao {

    /** Semua varian milik satu produk (relasi ke produk sudah ikut dimuat). */
    List<Variant> findByProduct(int productId);

    /** Seluruh varian yang cocok dengan merek / model / SKU / warna; keyword kosong = semua. */
    List<Variant> findByKeyword(String keyword);

    /** INSERT varian baru untuk produk dengan id = variant.getProductId(). */
    void insert(Variant variant);

    /** UPDATE varian yang id-nya sudah ada. */
    void update(Variant variant);

    void deleteById(int id);
}
