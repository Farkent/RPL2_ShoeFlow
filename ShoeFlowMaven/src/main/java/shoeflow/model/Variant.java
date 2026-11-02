package shoeflow.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;

/**
 * Model + Entity Hibernate untuk tabel variants (ukuran/warna + stok).
 * Field brand, model, price hanya bantuan tampilan (diambil dari products), bukan kolom tabel.
 */
@Entity
@Table(name = "variants")
public class Variant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    // Kolom product_id dibaca sebagai angka biasa (untuk kode JDBC lama),
    // sedangkan penulisannya dilakukan lewat relasi 'product' di bawah.
    @Column(name = "product_id", insertable = false, updatable = false)
    private int productId;

    // Relasi N : 1 ke tabel products (foreign key product_id)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "size", nullable = false, length = 10)
    private String size;

    @Column(name = "color", nullable = false, length = 30)
    private String color;

    @Column(name = "sku", nullable = false, unique = true, length = 50)
    private String sku;

    @Column(name = "stock", nullable = false)
    private int stock;

    @Column(name = "min_stock", nullable = false)
    private int minStock;

    // data hasil JOIN products (tidak ada kolomnya di tabel variants)
    @Transient
    private String brand;
    @Transient
    private String model;
    @Transient
    private BigDecimal price;

    public Variant() { }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public int getMinStock() { return minStock; }
    public void setMinStock(int minStock) { this.minStock = minStock; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getProductName() { return brand + " " + model; }
}
