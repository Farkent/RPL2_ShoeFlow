package shoeflow.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/** Model tabel purchase_orders. */
public class PurchaseOrder {
    private int id;
    private String poNumber;
    private int supplierId;
    private String supplierName;
    private String status;   // pending / received
    private BigDecimal totalAmount;
    private Timestamp createdAt;
    private Timestamp receivedAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getPoNumber() { return poNumber; }
    public void setPoNumber(String poNumber) { this.poNumber = poNumber; }
    public int getSupplierId() { return supplierId; }
    public void setSupplierId(int supplierId) { this.supplierId = supplierId; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Timestamp getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Timestamp receivedAt) { this.receivedAt = receivedAt; }
}
