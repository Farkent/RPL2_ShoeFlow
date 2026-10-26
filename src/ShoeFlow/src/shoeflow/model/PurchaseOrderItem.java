package shoeflow.model;

import java.math.BigDecimal;

/** Model tabel purchase_order_items (satu baris barang pada PO). */
public class PurchaseOrderItem {
    private int id;
    private int poId;
    private int variantId;
    private String sku;
    private String productName;
    private String size;
    private String color;
    private int qty;
    private BigDecimal unitCost = BigDecimal.ZERO;

    public PurchaseOrderItem() { }

    public PurchaseOrderItem(Variant v, int qty, BigDecimal unitCost) {
        this.variantId = v.getId();
        this.sku = v.getSku();
        this.productName = v.getProductName();
        this.size = v.getSize();
        this.color = v.getColor();
        this.qty = qty;
        this.unitCost = unitCost;
    }

    public BigDecimal getSubtotal() { return unitCost.multiply(BigDecimal.valueOf(qty)); }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getPoId() { return poId; }
    public void setPoId(int poId) { this.poId = poId; }
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
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
}
