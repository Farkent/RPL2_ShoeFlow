package shoeflow.model;

import java.math.BigDecimal;

/** Item keranjang belanja sementara (tidak disimpan ke database sampai pembayaran). */
public class CartItem {
    private int variantId;
    private String sku;
    private String productName;
    private String size;
    private String color;
    private int qty;
    private BigDecimal unitPrice;
    private int availableStock;   // stok saat item dimasukkan, untuk validasi qty

    public CartItem() { }

    public CartItem(Variant v, int qty) {
        this.variantId = v.getId();
        this.sku = v.getSku();
        this.productName = v.getProductName();
        this.size = v.getSize();
        this.color = v.getColor();
        this.unitPrice = v.getPrice();
        this.availableStock = v.getStock();
        this.qty = qty;
    }

    public BigDecimal getSubtotal() { return unitPrice.multiply(BigDecimal.valueOf(qty)); }

    public int getVariantId() { return variantId; }
    public void setVariantId(int variantId) { this.variantId = variantId; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public int getAvailableStock() { return availableStock; }
    public void setAvailableStock(int availableStock) { this.availableStock = availableStock; }
}
