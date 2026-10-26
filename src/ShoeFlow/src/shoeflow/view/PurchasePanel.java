package shoeflow.view;

import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import shoeflow.controller.PurchaseController;
import shoeflow.model.*;
import shoeflow.util.Rupiah;
import shoeflow.view.components.*;

/**
 * Manajemen Pembelian (admin/manajer): Buat Purchase Order, Penerimaan Barang (Goods Receipt), dan Supplier.
 * Kasir tidak bisa membuka panel ini (MainFrame tidak menampilkan menunya dan PurchaseController menolaknya).
 */
public class PurchasePanel extends JPanel implements Refreshable {

    private final PurchaseController ctl;
    private final JTabbedPane tabs = new JTabbedPane();

    // ---- Buat PO
    private final JComboBox<Supplier> cmbSupplier = new JComboBox<>();
    private final JTextField txtSku = new JTextField(14);
    private final JLabel lblPicked = new JLabel("(belum ada varian dipilih)");
    private final JTextField txtQty = UiUtil.digitsOnly(new JTextField("1", 5));
    private final JTextField txtCost = UiUtil.digitsOnly(new JTextField(9));
    private Variant picked;
    private final PoItemsModel itemsModel = new PoItemsModel();
    private final JTable itemsTable = UiUtil.style(new JTable(itemsModel));
    private final JLabel lblPoTotal = new JLabel("Rp0");

    // ---- Daftar PO / Penerimaan
    private final JComboBox<String> cmbStatus = new JComboBox<>(new String[]{"Semua", "pending", "received"});
    private final ReadOnlyModel poModel = new ReadOnlyModel("No. PO", "Supplier", "Status", "Total", "Dibuat", "Diterima");
    private final JTable poTable = UiUtil.style(new JTable(poModel));
    private List<PurchaseOrder> pos = new ArrayList<>();
    private final ReadOnlyModel poItemModel = new ReadOnlyModel("SKU", "Produk", "Ukuran", "Warna", "Qty", "Harga Beli", "Subtotal");
    private final JTable poItemTable = UiUtil.style(new JTable(poItemModel));

    // ---- Supplier
    private final ReadOnlyModel supModel = new ReadOnlyModel("Nama", "Telepon", "Alamat");
    private final JTable supTable = UiUtil.style(new JTable(supModel));
    private List<Supplier> suppliers = new ArrayList<>();

    public PurchasePanel(User user) {
        ctl = new PurchaseController(user);   // SecurityException untuk kasir
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(UiUtil.BG);
        add(UiUtil.title("Manajemen Pembelian"), BorderLayout.NORTH);

        tabs.addTab("Buat Purchase Order", buildCreateTab());
        tabs.addTab("Penerimaan Barang (Goods Receipt)", buildReceiveTab());
        tabs.addTab("Supplier", buildSupplierTab());
        tabs.addChangeListener(e -> {
            int i = tabs.getSelectedIndex();
            if (i == 0) loadSupplierCombo();
            if (i == 1) loadPOs();
            if (i == 2) loadSuppliers();
        });
        add(tabs, BorderLayout.CENTER);
    }

    @Override public void refresh() { loadSupplierCombo(); loadPOs(); loadSuppliers(); }

    // ================================================================= TAB 1: BUAT PO
    private JComponent buildCreateTab() {
        JButton bMgr = UiUtil.button("Kelola Supplier", Color.WHITE);
        bMgr.addActionListener(e -> tabs.setSelectedIndex(2));

        UiUtil.styleCombo(cmbSupplier);
        UiUtil.NeoPanel head = new UiUtil.NeoPanel(new FlowLayout(FlowLayout.LEFT, 8, 6), Color.WHITE, 3, 0);
        head.setPadding(6, 10, 6, 10);
        JLabel lSupPrompt = new JLabel("Pilih Supplier:");
        lSupPrompt.setFont(UiUtil.FONT_BOLD);
        head.add(lSupPrompt); head.add(cmbSupplier); head.add(bMgr);

        JButton bFind = UiUtil.button("Cari Varian...", UiUtil.ACCENT);
        JButton bAdd = UiUtil.button("Tambah ke PO", UiUtil.SUCCESS);
        bFind.addActionListener(e -> openPicker());
        txtSku.addActionListener(e -> pickBySku());
        bAdd.addActionListener(e -> addRow());

        UiUtil.styleField(txtSku);
        UiUtil.styleField(txtQty);
        UiUtil.styleField(txtCost);

        UiUtil.NeoPanel entry = new UiUtil.NeoPanel(new FlowLayout(FlowLayout.LEFT, 8, 6), Color.WHITE, 3, 0);
        entry.setPadding(6, 10, 6, 10);
        JLabel lSkuPrompt = new JLabel("SKU:"); lSkuPrompt.setFont(UiUtil.FONT_BOLD);
        JLabel lQtyPrompt = new JLabel("Qty:"); lQtyPrompt.setFont(UiUtil.FONT_BOLD);
        JLabel lCostPrompt = new JLabel("Harga Beli (Rp):"); lCostPrompt.setFont(UiUtil.FONT_BOLD);

        lblPicked.setFont(UiUtil.FONT_BOLD.deriveFont(12f));
        lblPicked.setForeground(UiUtil.ACCENT_CYAN.darker());

        entry.add(lSkuPrompt); entry.add(txtSku); entry.add(bFind);
        entry.add(lblPicked);
        entry.add(lQtyPrompt); entry.add(txtQty);
        entry.add(lCostPrompt); entry.add(txtCost);
        entry.add(bAdd);

        JPanel north = new JPanel(new BorderLayout(0, 8));
        north.setOpaque(false);
        north.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        north.add(head, BorderLayout.NORTH);
        north.add(entry, BorderLayout.SOUTH);

        itemsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JButton bDel = UiUtil.button("Hapus Baris", UiUtil.DANGER);
        JButton bClear = UiUtil.button("Kosongkan", Color.WHITE);
        JButton bSave = UiUtil.button("Simpan Purchase Order", UiUtil.SUCCESS);
        bDel.addActionListener(e -> {
            int r = itemsTable.getSelectedRow();
            if (r >= 0) { if (itemsTable.isEditing()) itemsTable.getCellEditor().stopCellEditing(); itemsModel.remove(r); updatePoTotal(); }
        });
        bClear.addActionListener(e -> { itemsModel.clear(); updatePoTotal(); });
        bSave.addActionListener(e -> savePO());

        lblPoTotal.setFont(UiUtil.FONT_BIG);
        lblPoTotal.setForeground(UiUtil.BORDER_DARK);

        UiUtil.NeoPanel south = new UiUtil.NeoPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6), Color.WHITE, 3, 0);
        south.setPadding(8, 12, 8, 12);
        JLabel lTotalPrompt = new JLabel("Total Estimasi PO:");
        lTotalPrompt.setFont(UiUtil.FONT_BOLD);
        south.add(lTotalPrompt); south.add(lblPoTotal);
        south.add(bDel); south.add(bClear); south.add(bSave);

        itemsModel.addTableModelListener(e -> updatePoTotal());

        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);
        p.add(north, BorderLayout.NORTH);
        p.add(UiUtil.scroll(itemsTable), BorderLayout.CENTER);
        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    private void loadSupplierCombo() {
        try {
            Supplier sel = (Supplier) cmbSupplier.getSelectedItem();
            cmbSupplier.removeAllItems();
            for (Supplier s : ctl.listSuppliers()) {
                cmbSupplier.addItem(s);
                if (sel != null && sel.getId() == s.getId()) cmbSupplier.setSelectedItem(s);
            }
        } catch (SQLException ex) { UiUtil.dbError(this, ex); }
    }

    private void pickBySku() {
        try {
            Variant v = ctl.findVariantBySku(txtSku.getText());
            if (v == null) { UiUtil.warn(this, "SKU tidak ditemukan. Gunakan 'Cari Varian...' untuk mencari."); return; }
            setPicked(v);
            txtCost.requestFocus();
        } catch (SQLException ex) { UiUtil.dbError(this, ex); }
    }

    private void setPicked(Variant v) {
        picked = v;
        txtSku.setText(v.getSku());
        lblPicked.setText(v.getProductName() + " " + v.getSize() + "/" + v.getColor() + " (stok " + v.getStock() + ")");
    }

    /** Dialog pencarian varian: ketik kata kunci, pilih baris. */
    private void openPicker() {
        JTextField kw = new JTextField(18);
        ReadOnlyModel m = new ReadOnlyModel("SKU", "Produk", "Ukuran", "Warna", "Stok");
        JTable t = UiUtil.style(new JTable(m));
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        final java.util.concurrent.atomic.AtomicReference<List<Variant>> found = new java.util.concurrent.atomic.AtomicReference<>(new ArrayList<Variant>());
        Runnable search = () -> {
            try {
                found.set(ctl.searchVariants(kw.getText()));
                m.setRowCount(0);
                for (Variant v : found.get()) m.addRow(new Object[]{v.getSku(), v.getProductName(), v.getSize(), v.getColor(), v.getStock()});
            } catch (SQLException ex) { UiUtil.dbError(this, ex); }
        };
        kw.addActionListener(e -> search.run());
        JButton go = new JButton("Cari");
        go.addActionListener(e -> search.run());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Kata kunci:")); top.add(kw); top.add(go);
        JPanel box = new JPanel(new BorderLayout());
        box.add(top, BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(t);
        sp.setPreferredSize(new Dimension(620, 300));
        box.add(sp, BorderLayout.CENTER);
        search.run();
        if (JOptionPane.showConfirmDialog(this, box, "Pilih Varian", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            int r = t.getSelectedRow();
            if (r >= 0 && r < found.get().size()) setPicked(found.get().get(r));
        }
    }

    private void addRow() {
        if (picked == null) { UiUtil.warn(this, "Pilih varian terlebih dahulu (isi SKU lalu Enter, atau klik 'Cari Varian...')."); return; }
        try {
            int qty = UiUtil.parseNonNegInt(txtQty.getText(), "Qty");
            if (qty <= 0) throw new IllegalArgumentException("Qty harus lebih dari 0.");
            BigDecimal cost = Rupiah.parse(txtCost.getText());
            itemsModel.add(new PurchaseOrderItem(picked, qty, cost));
            picked = null; txtSku.setText(""); txtQty.setText("1"); txtCost.setText("");
            lblPicked.setText("(belum ada varian dipilih)");
            txtSku.requestFocus();
        } catch (IllegalArgumentException ex) { UiUtil.warn(this, ex.getMessage()); }
    }

    private void updatePoTotal() {
        BigDecimal t = BigDecimal.ZERO;
        for (PurchaseOrderItem i : itemsModel.items) t = t.add(i.getSubtotal());
        lblPoTotal.setText(Rupiah.format(t));
    }

    private void savePO() {
        if (itemsTable.isEditing()) itemsTable.getCellEditor().stopCellEditing();
        Supplier s = (Supplier) cmbSupplier.getSelectedItem();
        try {
            String no = ctl.createPO(s == null ? 0 : s.getId(), itemsModel.items);
            itemsModel.clear();
            updatePoTotal();
            UiUtil.info(this, "Purchase Order " + no + " berhasil dibuat (status: pending).\n"
                    + "Stok akan bertambah setelah penerimaan barang disetujui.");
            loadPOs();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            UiUtil.warn(this, ex.getMessage());
        } catch (SQLException ex) { UiUtil.dbError(this, ex); }
    }

    /** Model tabel baris PO: kolom Qty dan Harga Beli bisa diedit langsung di tabel. */
    private static class PoItemsModel extends AbstractTableModel {
        private final String[] cols = {"SKU", "Produk", "Ukuran", "Warna", "Qty", "Harga Beli", "Subtotal"};
        final List<PurchaseOrderItem> items = new ArrayList<>();

        void add(PurchaseOrderItem it) {
            for (int i = 0; i < items.size(); i++) {   // varian sama -> qty digabung
                if (items.get(i).getVariantId() == it.getVariantId()) {
                    items.get(i).setQty(items.get(i).getQty() + it.getQty());
                    items.get(i).setUnitCost(it.getUnitCost());
                    fireTableRowsUpdated(i, i);
                    return;
                }
            }
            items.add(it);
            fireTableRowsInserted(items.size() - 1, items.size() - 1);
        }
        void remove(int r) { items.remove(r); fireTableRowsDeleted(r, r); }
        void clear() { items.clear(); fireTableDataChanged(); }

        @Override public int getRowCount() { return items.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Class<?> getColumnClass(int c) { return c == 4 ? Integer.class : (c >= 5 ? BigDecimal.class : String.class); }
        @Override public boolean isCellEditable(int r, int c) { return c == 4 || c == 5; }
        @Override public Object getValueAt(int r, int c) {
            PurchaseOrderItem i = items.get(r);
            switch (c) {
                case 0: return i.getSku();
                case 1: return i.getProductName();
                case 2: return i.getSize();
                case 3: return i.getColor();
                case 4: return i.getQty();
                case 5: return i.getUnitCost();
                default: return i.getSubtotal();
            }
        }
        @Override public void setValueAt(Object v, int r, int c) {
            PurchaseOrderItem i = items.get(r);
            try {
                if (c == 4) {
                    int q = Integer.parseInt(String.valueOf(v).trim());
                    if (q <= 0) throw new IllegalArgumentException("Qty harus lebih dari 0.");
                    i.setQty(q);
                } else if (c == 5) {
                    BigDecimal b = new BigDecimal(String.valueOf(v).trim());
                    if (b.signum() < 0) throw new IllegalArgumentException("Harga beli tidak boleh negatif.");
                    i.setUnitCost(b);
                }
                fireTableRowsUpdated(r, r);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Masukkan angka yang valid.", "Perhatian", JOptionPane.WARNING_MESSAGE);
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(null, e.getMessage(), "Perhatian", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    // ================================================================= TAB 2: PENERIMAAN BARANG
    private JComponent buildReceiveTab() {
        JButton bShow = UiUtil.button("Tampilkan", UiUtil.ACCENT);
        JButton bRecv = UiUtil.button("Setujui Penerimaan Barang", UiUtil.SUCCESS);
        JButton bDel = UiUtil.button("Hapus PO (pending)", UiUtil.DANGER);

        UiUtil.styleCombo(cmbStatus);
        UiUtil.NeoPanel tb = new UiUtil.NeoPanel(new FlowLayout(FlowLayout.LEFT, 8, 6), Color.WHITE, 3, 0);
        tb.setPadding(6, 10, 6, 10);
        JLabel lStatusPrompt = new JLabel("Filter Status:");
        lStatusPrompt.setFont(UiUtil.FONT_BOLD);
        tb.add(lStatusPrompt); tb.add(cmbStatus); tb.add(bShow); tb.add(bRecv); tb.add(bDel);

        bShow.addActionListener(e -> loadPOs());
        cmbStatus.addActionListener(e -> loadPOs());
        bRecv.addActionListener(e -> receive());
        bDel.addActionListener(e -> deletePO());

        poTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        poTable.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) loadPoItems(); });

        UiUtil.NeoPanel detail = new UiUtil.NeoPanel(new BorderLayout(0, 8), Color.WHITE, 3, 0);
        detail.setPadding(8, 10, 8, 10);
        JLabel lDetailTitle = new JLabel("RINCIAN BARANG PADA PO TERPILIH");
        lDetailTitle.setFont(UiUtil.FONT_BOLD.deriveFont(12.5f));
        detail.add(lDetailTitle, BorderLayout.NORTH);
        detail.add(UiUtil.scroll(poItemTable), BorderLayout.CENTER);

        JSplitPane sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, UiUtil.scroll(poTable), detail);
        sp.setResizeWeight(0.55);
        sp.setOpaque(false);
        sp.setBorder(null);
        sp.setDividerSize(8);

        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        p.add(tb, BorderLayout.NORTH);
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    private void loadPOs() {
        try {
            String st = (String) cmbStatus.getSelectedItem();
            pos = ctl.listPOs("Semua".equals(st) ? null : st);
            poModel.setRowCount(0);
            for (PurchaseOrder po : pos)
                poModel.addRow(new Object[]{po.getPoNumber(), po.getSupplierName(), po.getStatus(),
                        po.getTotalAmount(), po.getCreatedAt(), po.getReceivedAt()});
            loadPoItems();
        } catch (SQLException ex) { UiUtil.dbError(this, ex); }
    }

    private PurchaseOrder selectedPO() {
        int r = poTable.getSelectedRow();
        return (r < 0 || r >= pos.size()) ? null : pos.get(r);
    }

    private void loadPoItems() {
        poItemModel.setRowCount(0);
        PurchaseOrder po = selectedPO();
        if (po == null) return;
        try {
            for (PurchaseOrderItem i : ctl.getItems(po.getId()))
                poItemModel.addRow(new Object[]{i.getSku(), i.getProductName(), i.getSize(), i.getColor(),
                        i.getQty(), i.getUnitCost(), i.getSubtotal()});
        } catch (SQLException ex) { UiUtil.dbError(this, ex); }
    }

    private void receive() {
        PurchaseOrder po = selectedPO();
        if (po == null) { UiUtil.warn(this, "Pilih PO terlebih dahulu."); return; }
        if (!"pending".equals(po.getStatus())) { UiUtil.warn(this, "PO ini sudah diterima."); return; }
        if (!UiUtil.confirm(this, "Setujui penerimaan barang untuk " + po.getPoNumber() + "?\nStok seluruh barang pada PO akan langsung bertambah.")) return;
        try {
            ctl.receiveGoods(po.getId());
            UiUtil.info(this, "Penerimaan barang disetujui. Stok sudah bertambah.");
            loadPOs();
        } catch (IllegalStateException ex) {
            UiUtil.warn(this, ex.getMessage());
            loadPOs();
        } catch (SQLException ex) { UiUtil.dbError(this, ex); }
    }

    private void deletePO() {
        PurchaseOrder po = selectedPO();
        if (po == null) { UiUtil.warn(this, "Pilih PO terlebih dahulu."); return; }
        if (!UiUtil.confirm(this, "Hapus " + po.getPoNumber() + "?")) return;
        try { ctl.deletePendingPO(po.getId()); loadPOs(); }
        catch (IllegalStateException ex) { UiUtil.warn(this, ex.getMessage()); }
        catch (SQLException ex) { UiUtil.dbError(this, ex); }
    }

    // ================================================================= TAB 3: SUPPLIER
    private JComponent buildSupplierTab() {
        JButton bAdd = UiUtil.button("Tambah Supplier", UiUtil.SUCCESS);
        JButton bEdit = UiUtil.button("Edit Supplier", Color.WHITE);
        JButton bDel = UiUtil.button("Hapus Supplier", UiUtil.DANGER);

        UiUtil.NeoPanel tb = new UiUtil.NeoPanel(new FlowLayout(FlowLayout.LEFT, 8, 6), Color.WHITE, 3, 0);
        tb.setPadding(6, 10, 6, 10);
        tb.add(bAdd); tb.add(bEdit); tb.add(bDel);

        bAdd.addActionListener(e -> editSupplier(null));
        bEdit.addActionListener(e -> { Supplier s = selectedSupplier(); if (s != null) editSupplier(s); });
        bDel.addActionListener(e -> deleteSupplier());
        supTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        p.add(tb, BorderLayout.NORTH);
        p.add(UiUtil.scroll(supTable), BorderLayout.CENTER);
        return p;
    }

    private void loadSuppliers() {
        try {
            suppliers = ctl.listSuppliers();
            supModel.setRowCount(0);
            for (Supplier s : suppliers) supModel.addRow(new Object[]{s.getName(), s.getPhone(), s.getAddress()});
        } catch (SQLException ex) { UiUtil.dbError(this, ex); }
    }

    private Supplier selectedSupplier() {
        int r = supTable.getSelectedRow();
        if (r < 0 || r >= suppliers.size()) { UiUtil.warn(this, "Pilih supplier terlebih dahulu."); return null; }
        return suppliers.get(r);
    }

    private void editSupplier(Supplier existing) {
        Supplier s = existing == null ? new Supplier() : existing;
        JTextField n = new JTextField(existing == null ? "" : s.getName(), 22);
        JTextField ph = new JTextField(existing == null ? "" : s.getPhone(), 22);
        JTextArea ad = new JTextArea(existing == null ? "" : s.getAddress(), 3, 22);
        JPanel form = UiUtil.form("Nama", n, "Telepon", ph, "Alamat", new JScrollPane(ad));
        while (true) {
            if (JOptionPane.showConfirmDialog(this, form, existing == null ? "Tambah Supplier" : "Edit Supplier",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
            try {
                s.setName(n.getText()); s.setPhone(ph.getText()); s.setAddress(ad.getText());
                ctl.saveSupplier(s);
                loadSuppliers();
                return;
            } catch (IllegalArgumentException ex) { UiUtil.warn(this, ex.getMessage()); }
            catch (SQLException ex) { UiUtil.dbError(this, ex); return; }
        }
    }

    private void deleteSupplier() {
        Supplier s = selectedSupplier();
        if (s == null) return;
        if (!UiUtil.confirm(this, "Hapus supplier '" + s.getName() + "'?")) return;
        try { ctl.deleteSupplier(s.getId()); loadSuppliers(); }
        catch (IllegalStateException ex) { UiUtil.warn(this, ex.getMessage()); }
        catch (SQLException ex) { UiUtil.dbError(this, ex); }
    }
}
