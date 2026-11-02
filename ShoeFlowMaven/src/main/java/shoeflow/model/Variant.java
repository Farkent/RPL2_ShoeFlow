package shoeflow.model;

import java.math.BigDecimal;

/**
 * Model tabel variants (ukuran/warna + stok).
 * Field brand, model, price diisi dari JOIN ke products agar POS tidak perlu query kedua.
 */
public class Variant {
    private int id;
    private int productId;
    private String size;
    private String color;
    private String sku;
    private int stock;
    private int minStock;

    // data hasil JOIN products
    private String brand;
    private String model;
    private BigDecimal price;

    public Variant() { }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
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
