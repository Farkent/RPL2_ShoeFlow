package shoeflow.controller;

import java.math.BigDecimal;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import shoeflow.config.Koneksi;
import shoeflow.model.*;
import shoeflow.util.Roles;

/** Logika bisnis Point of Sales (admin, manajer, dan kasir). */
public class PosController extends BaseController {

    public PosController(User user) { super(user, Roles.ADMIN, Roles.MANAJER, Roles.KASIR); }

    // ------------------------------------------------------------ pencarian produk
    /** Cari varian berdasarkan kode SKU. Null jika tidak ada. */
    public Variant findBySku(String sku) throws SQLException {
        try (Connection c = Koneksi.getConnection();
             PreparedStatement ps = c.prepareStatement(VARIANT_SELECT + "WHERE v.sku = ?")) {
            ps.setString(1, sku.trim());
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? mapVariant(rs) : null; }
        }
    }

    /** Data untuk grid gambar produk (filter merek / model / SKU). */
    public List<Product> searchProducts(String keyword) throws SQLException {
        return queryProducts(keyword, 80);
    }

    public List<Variant> getVariantsOfProduct(int productId) throws SQLException {
        List<Variant> list = new ArrayList<>();
        try (Connection c = Koneksi.getConnection();
             PreparedStatement ps = c.prepareStatement(VARIANT_SELECT + "WHERE v.product_id = ? ORDER BY v.size, v.color")) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapVariant(rs)); }
        }
        return list;
    }

    // ------------------------------------------------------------ keranjang
    /**
     * Masukkan varian ke keranjang (SKU yang sama digabung).
     * Total qty di keranjang tidak boleh melebihi stok -> mencegah overselling.
     * @return pesan error, atau null jika berhasil.
     */
    public String addToCart(List<CartItem> cart, Variant v, int qty) {
        if (qty <= 0) return "Jumlah harus lebih dari 0.";
        for (CartItem item : cart) {
            if (item.getVariantId() == v.getId()) {
                if (item.getQty() + qty > v.getStock())
                    return "Stok tidak cukup. Stok tersedia: " + v.getStock() + ", sudah di keranjang: " + item.getQty();
                item.setQty(item.getQty() + qty);
                item.setAvailableStock(v.getStock());
                return null;
            }
        }
        if (qty > v.getStock()) return "Stok tidak cukup. Stok tersedia: " + v.getStock();
        cart.add(new CartItem(v, qty));
        return null;
    }

    /** Tambah/kurangi qty item (delta +1/-1). Qty menjadi 0 -> item dihapus. @return pesan error atau null. */
    public String changeQty(List<CartItem> cart, int index, int delta) {
        CartItem it = cart.get(index);
        int nq = it.getQty() + delta;
        if (nq <= 0) { cart.remove(index); return null; }
        if (nq > it.getAvailableStock()) return "Stok tidak cukup. Stok tersedia: " + it.getAvailableStock();
        it.setQty(nq);
        return null;
    }

    public BigDecimal calcTotal(List<CartItem> cart) {
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem i : cart) total = total.add(i.getSubtotal());
        return total;
    }

    // ------------------------------------------------------------ pembayaran (DATABASE TRANSACTION)
    /**
     * PROSES PEMBAYARAN dengan DATABASE TRANSACTION.
     * Insert sales + insert sale_items + potong stok harus sukses SEMUA atau dibatalkan SEMUA (rollback).
     * Pemotongan stok memakai "stock >= ?" sehingga atomik: jika kasir lain lebih dulu mengambil stok,
     * transaksi ini dibatalkan dan tidak terjadi stok minus.
     */
    public Sale processPayment(List<CartItem> cart, String method, BigDecimal amountPaid) throws SQLException {
        if (cart.isEmpty()) throw new IllegalStateException("Keranjang masih kosong.");
        BigDecimal total = calcTotal(cart);
        if ("tunai".equals(method) && amountPaid.compareTo(total) < 0)
            throw new IllegalStateException("Uang yang dibayarkan kurang dari total.");
        if ("transfer".equals(method)) amountPaid = total;   // transfer = bayar pas
        BigDecimal change = amountPaid.subtract(total);

        Sale sale = new Sale();
        sale.setInvoiceNo("INV" + new SimpleDateFormat("yyyyMMdd-HHmmssSSS").format(new Date()));
        sale.setCashierId(user.getId());
        sale.setCashierName(user.getName());
        sale.setTotal(total);
        sale.setPaymentMethod(method);
        sale.setAmountPaid(amountPaid);
        sale.setChangeAmount(change);
        sale.setCreatedAt(new Timestamp(System.currentTimeMillis()));

        Connection conn = null;
        try {
            conn = Koneksi.getConnection();
            conn.setAutoCommit(false);   // <-- mulai transaksi

            String insSale = "INSERT INTO sales (invoice_no, cashier_id, total, payment_method, amount_paid, change_amount) VALUES (?,?,?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(insSale, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, sale.getInvoiceNo());
                ps.setInt(2, user.getId());
                ps.setBigDecimal(3, total);
                ps.setString(4, method);
                ps.setBigDecimal(5, amountPaid);
                ps.setBigDecimal(6, change);
                ps.executeUpdate();
                try (ResultSet k = ps.getGeneratedKeys()) { k.next(); sale.setId(k.getInt(1)); }
            }

            String insItem = "INSERT INTO sale_items (sale_id, variant_id, qty, unit_price, subtotal) VALUES (?,?,?,?,?)";
            String updStock = "UPDATE variants SET stock = stock - ? WHERE id = ? AND stock >= ?";
            try (PreparedStatement psItem = conn.prepareStatement(insItem);
                 PreparedStatement psStock = conn.prepareStatement(updStock)) {
                for (CartItem it : cart) {
                    psStock.setInt(1, it.getQty());
                    psStock.setInt(2, it.getVariantId());
                    psStock.setInt(3, it.getQty());
                    if (psStock.executeUpdate() == 0)
                        throw new IllegalStateException("Stok SKU " + it.getSku() + " tidak mencukupi (sudah berubah). Transaksi dibatalkan.");
                    psItem.setInt(1, sale.getId());
                    psItem.setInt(2, it.getVariantId());
                    psItem.setInt(3, it.getQty());
                    psItem.setBigDecimal(4, it.getUnitPrice());
                    psItem.setBigDecimal(5, it.getSubtotal());
                    psItem.executeUpdate();
                }
            }
            conn.commit();   // semua sukses -> simpan permanen
            return sale;
        } catch (SQLException | RuntimeException e) {
            rollbackQuietly(conn);   // ada yang gagal -> batalkan semua
            throw e;
        } finally {
            closeQuietly(conn);
        }
    }

    // ------------------------------------------------------------ riwayat transaksi
    /** Kasir hanya melihat transaksinya sendiri; admin/manajer melihat semua. */
    public List<Sale> getHistory(LocalDate from, LocalDate to) throws SQLException {
        boolean onlyMine = Roles.KASIR.equalsIgnoreCase(user.getRoleName());
        String sql = "SELECT s.id, s.invoice_no, s.cashier_id, u.name, s.total, s.payment_method, s.amount_paid, s.change_amount, s.created_at "
                   + "FROM sales s JOIN users u ON u.id = s.cashier_id "
                   + "WHERE s.created_at >= ? AND s.created_at < ? " + (onlyMine ? "AND s.cashier_id = ? " : "")
                   + "ORDER BY s.created_at DESC, s.id DESC LIMIT 500";
        List<Sale> list = new ArrayList<>();
        try (Connection c = Koneksi.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(from.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
            if (onlyMine) ps.setInt(3, user.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Sale s = new Sale();
                    s.setId(rs.getInt(1)); s.setInvoiceNo(rs.getString(2)); s.setCashierId(rs.getInt(3));
                    s.setCashierName(rs.getString(4)); s.setTotal(rs.getBigDecimal(5)); s.setPaymentMethod(rs.getString(6));
                    s.setAmountPaid(rs.getBigDecimal(7)); s.setChangeAmount(rs.getBigDecimal(8)); s.setCreatedAt(rs.getTimestamp(9));
                    list.add(s);
                }
            }
        }
        return list;
    }

    /** Item sebuah transaksi (untuk cetak ulang struk dari riwayat). */
    public List<CartItem> getSaleItems(int saleId) throws SQLException {
        String sql = "SELECT si.variant_id, v.sku, CONCAT(p.brand,' ',p.model), v.size, v.color, si.qty, si.unit_price "
                   + "FROM sale_items si JOIN variants v ON v.id = si.variant_id JOIN products p ON p.id = v.product_id "
                   + "WHERE si.sale_id = ? ORDER BY si.id";
        List<CartItem> list = new ArrayList<>();
        try (Connection c = Koneksi.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, saleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CartItem it = new CartItem();
                    it.setVariantId(rs.getInt(1)); it.setSku(rs.getString(2)); it.setProductName(rs.getString(3));
                    it.setSize(rs.getString(4)); it.setColor(rs.getString(5)); it.setQty(rs.getInt(6));
                    it.setUnitPrice(rs.getBigDecimal(7));
                    list.add(it);
                }
            }
        }
        return list;
    }
}
