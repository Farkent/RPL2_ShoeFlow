package shoeflow.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Model + Entity Hibernate untuk tabel products (master model sepatu).
 * Anotasi JPA/Hibernate memetakan class ini ke tabel (ORM mapping).
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // id AUTO_INCREMENT di MySQL
    private int id;

    @Column(name = "brand", nullable = false, length = 50)
    private String brand;

    @Column(name = "model", nullable = false, length = 100)
    private String model;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "image", length = 255)
    private String image;

    // Diisi otomatis oleh MySQL (DEFAULT CURRENT_TIMESTAMP), jadi tidak ikut INSERT/UPDATE
    @Column(name = "created_at", insertable = false, updatable = false)
    private Timestamp createdAt;

    // Relasi 1 : N ke tabel variants (satu model punya banyak varian ukuran/warna)
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Variant> variants = new ArrayList<>();

    // Data agregat hasil hitungan, tidak ada kolomnya di tabel
    @Transient
    private int totalStock;
    @Transient
    private int variantCount;

    public Product() { }

    public Product(int id, String brand, String model, BigDecimal price, String image, Timestamp createdAt) {
        this.id = id; this.brand = brand; this.model = model;
        this.price = price; this.image = image; this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public List<Variant> getVariants() { return variants; }
    public void setVariants(List<Variant> variants) { this.variants = variants; }
    public int getTotalStock() { return totalStock; }
    public void setTotalStock(int totalStock) { this.totalStock = totalStock; }
    public int getVariantCount() { return variantCount; }
    public void setVariantCount(int variantCount) { this.variantCount = variantCount; }

    public String getName() { return brand + " " + model; }
    @Override public String toString() { return getName(); }
}
