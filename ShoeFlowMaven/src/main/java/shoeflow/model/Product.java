package shoeflow.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/** Model tabel products (master model sepatu) + agregat stok dari tabel variants. */
public class Product {
    private int id;
    private String brand;
    private String model;
    private BigDecimal price;
    private String image;
    private Timestamp createdAt;
    private int totalStock;     // hasil SUM(variants.stock)
    private int variantCount;   // hasil COUNT(variants)

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
    public int getTotalStock() { return totalStock; }
    public void setTotalStock(int totalStock) { this.totalStock = totalStock; }
    public int getVariantCount() { return variantCount; }
    public void setVariantCount(int variantCount) { this.variantCount = variantCount; }

    public String getName() { return brand + " " + model; }
    @Override public String toString() { return getName(); }
}
