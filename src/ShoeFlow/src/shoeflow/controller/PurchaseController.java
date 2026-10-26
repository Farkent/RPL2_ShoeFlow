package shoeflow.controller;

import java.math.BigDecimal;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import shoeflow.config.Koneksi;
import shoeflow.model.*;
import shoeflow.util.Roles;

/**
 * Manajemen Pembelian: Supplier, Purchase Order (PO), dan Penerimaan Barang (Goods Receipt).
 * Kasir TIDAK bisa membuat controller ini (ditolak oleh BaseController).
 */
public class PurchaseController extends BaseController {

    public PurchaseController(User user) { super(user, Roles.ADMIN, Roles.MANAJER); }

    // ------------------------------------------------------------ Supplier
    public List<Supplier> listSuppliers() throws SQLException {
        List<Supplier> list = new ArrayList<>();
        try (Connection c = Koneksi.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, name, phone, address FROM suppliers ORDER BY name")) {
            while (rs.next()) {
                Supplier s = new Supplier();
                s.setId(rs.getInt(1)); s.setName(rs.getString(2));
                s.setPhone(rs.getString(3)); s.setAddress(rs.getString(4));
                list.add(s);
            }
        }
        return list;
    }

    public void saveSupplier(Supplier s) throws SQLException {
        if (s.getName() == null || s.getName().trim().isEmpty()) throw new IllegalArgumentException("Nama supplier wajib diisi.");
        try (Connection c = Koneksi.getConnection()) {
            if (s.getId() == 0) {
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO suppliers (name, phone, address) VALUES (?,?,?)")) {
                    ps.setString(1, s.getName().trim()); ps.setString(2, s.getPhone()); ps.setString(3, s.getAddress());
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = c.prepareStatement("UPDATE suppliers SET name=?, phone=?, address=? WHERE id=?")) {
                    ps.setString(1, s.getName().trim()); ps.setString(2, s.getPhone()); ps.setString(3, s.getAddress());
                    ps.setInt(4, s.getId());
                    ps.executeUpdate();
                }
            }
        }
    }

    public void deleteSupplier(int id) throws SQLException {
        try (Connection c = Koneksi.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM suppliers WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (isInUse(e)) throw new IllegalStateException("Supplier tidak bisa dihapus karena sudah dipakai pada PO.");
            throw e;
        }
    }

    // ------------------------------------------------------------ Pencarian varian untuk baris PO
    public Variant findVariantBySku(String sku) throws SQLException {
        try (Connection c = Koneksi.getConnection();
             PreparedStatement ps = c.prepareStatement(VARIANT_SELECT + "WHERE v.sku = ?")) {
            ps.setString(1, sku.trim());
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? mapVariant(rs) : null; }
        }
    }

    public List<Variant> searchVariants(String keyword) throws SQLException {
        String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        List<Variant> list = new ArrayList<>();
        try (Connection c = Koneksi.getConnection();
             PreparedStatement ps = c.prepareStatement(VARIANT_SELECT
                     + "WHERE p.brand LIKE ? OR p.model LIKE ? OR v.sku LIKE ? ORDER BY p.brand, p.model, v.size LIMIT 200")) {
            ps.setString(1, like); ps.setString(2, like); ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapVariant(rs)); }
        }
        return list;
    }

    // ------------------------------------------------------------ Purchase Order
    /**
     * Membuat PO (status awal 'pending'). Header + semua baris item disimpan dalam SATU transaksi.
     * Stok BELUM berubah; stok baru bertambah saat Goods Receipt disetujui.
     * @return nomor PO
     */
    public String createPO(int supplierId, List<PurchaseOrderItem> items) throws SQLException {
        if (supplierId <= 0) throw new IllegalArgumentException("Pilih supplier terlebih dahulu.");
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("Tambahkan minimal satu barang ke PO.");
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseOrderItem it : items) {
            if (it.getQty() <= 0) throw new IllegalArgumentException("Qty " + it.getSku() + " harus lebih dari 0.");
            if (it.getUnitCost().signum() < 0) throw new IllegalArgumentException("Harga beli " + it.getSku() + " tidak boleh negatif.");
            total = total.add(it.getSubtotal());
        }
        String poNumber = "PO-" + new SimpleDateFormat("yyyyMMdd-HHmmssSSS").format(new Date());

        Connection c = null;
        try {
            c = Koneksi.getConnection();
            c.setAutoCommit(false);
            int poId;
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO purchase_orders (po_number, supplier_id, total_amount) VALUES (?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, poNumber); ps.setInt(2, supplierId); ps.setBigDecimal(3, total);
                ps.executeUpdate();
                try (ResultSet k = ps.getGeneratedKeys()) { k.next(); poId = k.getInt(1); }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO purchase_order_items (po_id, variant_id, qty, unit_cost, subtotal) VALUES (?,?,?,?,?)")) {
                for (PurchaseOrderItem it : items) {
                    ps.setInt(1, poId); ps.setInt(2, it.getVariantId()); ps.setInt(3, it.getQty());
                    ps.setBigDecimal(4, it.getUnitCost()); ps.setBigDecimal(5, it.getSubtotal());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            c.commit();
            return poNumber;
        } catch (SQLException | RuntimeException e) {
            rollbackQuietly(c);
            throw e;
        } finally {
            closeQuietly(c);
        }
    }

    /** @param status "pending", "received", atau null untuk semua */
    public List<PurchaseOrder> listPOs(String status) throws SQLException {
        String sql = "SELECT po.id, po.po_number, po.supplier_id, s.name, po.status, po.total_amount, po.created_at, po.received_at "
                   + "FROM purchase_orders po JOIN suppliers s ON s.id = po.supplier_id "
                   + (status != null ? "WHERE po.status = ? " : "") + "ORDER BY po.created_at DESC, po.id DESC";
        List<PurchaseOrder> list = new ArrayList<>();
        try (Connection c = Koneksi.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (status != null) ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PurchaseOrder po = new PurchaseOrder();
                    po.setId(rs.getInt(1)); po.setPoNumber(rs.getString(2)); po.setSupplierId(rs.getInt(3));
                    po.setSupplierName(rs.getString(4)); po.setStatus(rs.getString(5));
                    po.setTotalAmount(rs.getBigDecimal(6)); po.setCreatedAt(rs.getTimestamp(7));
                    po.setReceivedAt(rs.getTimestamp(8));
                    list.add(po);
                }
            }
        }
        return list;
    }

    public List<PurchaseOrderItem> getItems(int poId) throws SQLException {
        String sql = "SELECT i.id, i.variant_id, v.sku, CONCAT(p.brand,' ',p.model), v.size, v.color, i.qty, i.unit_cost "
                   + "FROM purchase_order_items i JOIN variants v ON v.id = i.variant_id "
                   + "JOIN products p ON p.id = v.product_id WHERE i.po_id = ? ORDER BY i.id";
        List<PurchaseOrderItem> list = new ArrayList<>();
        try (Connection c = Koneksi.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, poId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PurchaseOrderItem it = new PurchaseOrderItem();
                    it.setId(rs.getInt(1)); it.setPoId(poId); it.setVariantId(rs.getInt(2));
                    it.setSku(rs.getString(3)); it.setProductName(rs.getString(4));
                    it.setSize(rs.getString(5)); it.setColor(rs.getString(6));
                    it.setQty(rs.getInt(7)); it.setUnitCost(rs.getBigDecimal(8));
                    list.add(it);
                }
            }
        }
        return list;
    }

    /**
     * GOODS RECEIPT: menyetujui penerimaan barang.
     * Dalam satu transaksi: kunci baris PO (FOR UPDATE) -> pastikan masih 'pending' ->
     * tambah stok tiap varian -> ubah status jadi 'received'. Penguncian mencegah stok
     * bertambah dua kali jika dua pengguna menyetujui PO yang sama bersamaan.
     */
    public void receiveGoods(int poId) throws SQLException {
        Connection c = null;
        try {
            c = Koneksi.getConnection();
            c.setAutoCommit(false);

            String status;
            try (PreparedStatement ps = c.prepareStatement("SELECT status FROM purchase_orders WHERE id = ? FOR UPDATE")) {
                ps.setInt(1, poId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new IllegalStateException("PO tidak ditemukan.");
                    status = rs.getString(1);
                }
            }
            if (!"pending".equals(status)) throw new IllegalStateException("PO ini sudah diterima sebelumnya.");

            try (PreparedStatement sel = c.prepareStatement("SELECT variant_id, qty FROM purchase_order_items WHERE po_id = ?");
                 PreparedStatement upd = c.prepareStatement("UPDATE variants SET stock = stock + ? WHERE id = ?")) {
                sel.setInt(1, poId);
                try (ResultSet rs = sel.executeQuery()) {
                    while (rs.next()) {
                        upd.setInt(1, rs.getInt("qty"));
                        upd.setInt(2, rs.getInt("variant_id"));
                        upd.executeUpdate();
                    }
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE purchase_orders SET status = 'received', received_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                ps.setInt(1, poId);
                ps.executeUpdate();
            }
            c.commit();
        } catch (SQLException | RuntimeException e) {
            rollbackQuietly(c);
            throw e;
        } finally {
            closeQuietly(c);
        }
    }

    /** Hapus PO yang masih pending (item ikut terhapus via ON DELETE CASCADE). */
    public void deletePendingPO(int poId) throws SQLException {
        try (Connection c = Koneksi.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM purchase_orders WHERE id = ? AND status = 'pending'")) {
            ps.setInt(1, poId);
            if (ps.executeUpdate() == 0) throw new IllegalStateException("Hanya PO berstatus pending yang bisa dihapus.");
        }
    }
}
