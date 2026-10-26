package shoeflow.view;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import shoeflow.controller.PosController;
import shoeflow.model.CartItem;
import shoeflow.model.Product;
import shoeflow.model.Sale;
import shoeflow.model.User;
import shoeflow.model.Variant;
import shoeflow.util.ImageUtil;
import shoeflow.util.PdfReceipt;
import shoeflow.util.Rupiah;
import shoeflow.view.components.*;

/**
 * Point of Sales. Dua tab sesuai diagram navigasi:
 *   Proses Transaksi : Scan/Cari Produk -> Keranjang Belanja -> Proses Pembayaran -> Cetak Struk (PDF)
 *   Riwayat Transaksi
 */
public class PosPanel extends JPanel implements Refreshable {

    private final User user;
    private final PosController ctl;
    private final List<CartItem> cart = new ArrayList<>();

    // --- Proses Transaksi
    private final JTextField txtSku = new JTextField(16);
    private final JTextField txtQty = UiUtil.digitsOnly(new JTextField("1", 3));
    private final JPanel grid = new JPanel(new GridLayout(0, 4, 10, 10));
    private final JLabel lblInfo = new JLabel(" ");
    private final ReadOnlyModel cartModel = new ReadOnlyModel("SKU", "Produk", "Uk.", "Warna", "Qty", "Harga", "Subtotal");
    private final JTable cartTable = UiUtil.style(new JTable(cartModel));
    private final JLabel lblTotal = new JLabel("Rp0");
    private final JLabel lblKembali = new JLabel("Rp0");
    private final JComboBox<String> cmbMetode = new JComboBox<>(new String[]{"Tunai", "Transfer Bank"});
    private final JTextField txtBayar = UiUtil.digitsOnly(new JTextField(12));

    // --- Riwayat
    private final JSpinner spFrom = UiUtil.dateSpinner(LocalDate.now().minusDays(7));
    private final JSpinner spTo = UiUtil.dateSpinner(LocalDate.now());
    private final ReadOnlyModel histModel = new ReadOnlyModel("Invoice", "Waktu", "Kasir", "Metode", "Total");
    private final JTable histTable = UiUtil.style(new JTable(histModel));
    private List<Sale> history = new ArrayList<>();

    public PosPanel(User user) {
        this.user = user;
        this.ctl = new PosController(user);
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(UiUtil.BG);
        add(UiUtil.title("Point of Sales"), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Proses Transaksi", buildTransactionTab());
        tabs.addTab("Riwayat Transaksi", buildHistoryTab());
        tabs.addChangeListener(e -> { if (tabs.getSelectedIndex() == 1) loadHistory(); });
        add(tabs, BorderLayout.CENTER);
    }

    @Override public void refresh() { loadGrid(""); updateTotals(); }

    // ================================================================= TAB: PROSES TRANSAKSI
    private JComponent buildTransactionTab() {
        // ----- kiri: pencarian SKU + grid gambar produk
        JButton bCari = UiUtil.button("🔍 Cari / Tambah", UiUtil.ACCENT);
        JButton bAll = UiUtil.button("Semua Produk", Color.WHITE);

        UiUtil.styleField(txtSku);
        UiUtil.styleField(txtQty);

        UiUtil.NeoPanel searchPanel = new UiUtil.NeoPanel(new FlowLayout(FlowLayout.LEFT, 8, 6), Color.WHITE, 3, 0);
        searchPanel.setPadding(6, 10, 6, 10);
        JLabel lblSkuPrompt = new JLabel("Kode SKU / kata kunci:");
        lblSkuPrompt.setFont(UiUtil.FONT_BOLD);
        JLabel lblQtyPrompt = new JLabel("Qty:");
        lblQtyPrompt.setFont(UiUtil.FONT_BOLD);

        searchPanel.add(lblSkuPrompt); searchPanel.add(txtSku);
        searchPanel.add(lblQtyPrompt); searchPanel.add(txtQty);
        searchPanel.add(bCari); searchPanel.add(bAll);

        txtSku.addActionListener(e -> searchOrAdd());
        bCari.addActionListener(e -> searchOrAdd());
        bAll.addActionListener(e -> { txtSku.setText(""); loadGrid(""); });

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.add(grid, BorderLayout.NORTH);
        wrap.setBackground(UiUtil.BG);
        grid.setOpaque(false);
        wrap.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JScrollPane gridScroll = UiUtil.scroll(wrap);
        gridScroll.getVerticalScrollBar().setUnitIncrement(24);

        lblInfo.setFont(UiUtil.FONT_BOLD.deriveFont(12f));
        lblInfo.setForeground(UiUtil.BORDER_DARK);
        lblInfo.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JPanel left = new JPanel(new BorderLayout(0, 8));
        left.setOpaque(false);
        JPanel leftTop = new JPanel(new BorderLayout(0, 4));
        leftTop.setOpaque(false);
        leftTop.add(searchPanel, BorderLayout.NORTH);
        leftTop.add(lblInfo, BorderLayout.SOUTH);
        left.add(leftTop, BorderLayout.NORTH);
        left.add(gridScroll, BorderLayout.CENTER);

        // ----- kanan: keranjang + pembayaran
        JButton bPlus = UiUtil.button(" + ", UiUtil.ACCENT);
        JButton bMinus = UiUtil.button(" − ", UiUtil.ACCENT);
        JButton bHapus = UiUtil.button("Hapus Item", UiUtil.DANGER);
        JButton bReset = UiUtil.button("Kosongkan", Color.WHITE);
        bPlus.addActionListener(e -> changeQty(1));
        bMinus.addActionListener(e -> changeQty(-1));
        bHapus.addActionListener(e -> removeSelected());
        bReset.addActionListener(e -> resetCart());

        JPanel cartTools = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        cartTools.setOpaque(false);
        cartTools.add(bPlus); cartTools.add(bMinus); cartTools.add(bHapus); cartTools.add(bReset);
        cartTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        UiUtil.NeoPanel cartBox = new UiUtil.NeoPanel(new BorderLayout(0, 8), Color.WHITE, 4, 0);
        cartBox.setPadding(10, 12, 10, 12);
        JLabel lblCartTitle = new JLabel("KERANJANG");
        lblCartTitle.setFont(UiUtil.FONT_BOLD.deriveFont(13f));
        lblCartTitle.setForeground(UiUtil.BORDER_DARK);

        JPanel cartHeader = new JPanel(new BorderLayout());
        cartHeader.setOpaque(false);
        cartHeader.add(lblCartTitle, BorderLayout.WEST);
        cartHeader.add(cartTools, BorderLayout.EAST);

        cartBox.add(cartHeader, BorderLayout.NORTH);
        cartBox.add(UiUtil.scroll(cartTable), BorderLayout.CENTER);

        lblTotal.setFont(UiUtil.FONT_BIG);
        lblTotal.setForeground(UiUtil.BORDER_DARK);
        lblKembali.setFont(UiUtil.FONT_BOLD.deriveFont(16f));
        lblKembali.setForeground(new Color(0x16A34A));

        JButton bBayar = UiUtil.button("PROSES PEMBAYARAN & CETAK STRUK ⚡", UiUtil.SUCCESS);
        bBayar.setFont(UiUtil.FONT_BOLD.deriveFont(13.5f));
        bBayar.setPreferredSize(new Dimension(0, 44));
        bBayar.addActionListener(e -> doPayment());

        UiUtil.styleCombo(cmbMetode);
        UiUtil.styleField(txtBayar);

        JPanel pay = UiUtil.form("TOTAL", lblTotal, "Metode", cmbMetode, "Dibayar (Rp)", txtBayar, "Kembalian", lblKembali);
        pay.setOpaque(false);

        UiUtil.NeoPanel payBox = new UiUtil.NeoPanel(new BorderLayout(0, 10), Color.WHITE, 4, 0);
        payBox.setPadding(10, 12, 10, 12);

        JLabel lblPayTitle = new JLabel("💳 RINGKASAN PEMBAYARAN");
        lblPayTitle.setFont(UiUtil.FONT_BOLD.deriveFont(13f));
        lblPayTitle.setForeground(UiUtil.BORDER_DARK);

        payBox.add(lblPayTitle, BorderLayout.NORTH);
        payBox.add(pay, BorderLayout.CENTER);
        payBox.add(bBayar, BorderLayout.SOUTH);

        txtBayar.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateTotals(); }
            public void removeUpdate(DocumentEvent e) { updateTotals(); }
            public void changedUpdate(DocumentEvent e) { updateTotals(); }
        });
        cmbMetode.addActionListener(e -> {
            boolean tunai = cmbMetode.getSelectedIndex() == 0;
            txtBayar.setEnabled(tunai);
            if (!tunai) txtBayar.setText("");
            updateTotals();
        });

        JPanel right = new JPanel(new BorderLayout(0, 12));
        right.setOpaque(false);
        right.setPreferredSize(new Dimension(460, 0));
        right.add(cartBox, BorderLayout.CENTER);
        right.add(payBox, BorderLayout.SOUTH);

        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setOpaque(false);
        p.add(left, BorderLayout.CENTER);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    // ----------------------------------------------------------------- grid & pencarian
    private void loadGrid(String keyword) {
        try {
            grid.removeAll();
            List<Product> list = ctl.searchProducts(keyword);
            for (Product p : list) grid.add(buildCard(p));
            if (list.isEmpty()) lblInfo.setText("Tidak ada produk yang cocok.");
            grid.revalidate();
            grid.repaint();
        } catch (SQLException ex) { UiUtil.dbError(this, ex); }
    }

    /** Kartu produk Neobrutalism (border solid hitam 2px, hard drop shadow, price badge kuning). */
    private JButton buildCard(Product p) {
        boolean out = p.getTotalStock() <= 0;
        String html = "<html><center><div style='padding-top:4px;'><b>" + UiUtil.esc(p.getName()) + "</b></div>"
                + "<div style='margin-top:4px;'><span style='background:#FFE500;color:#18181B;border:1px solid #18181B;padding:2px 8px;'><b>"
                + Rupiah.format(p.getPrice()) + "</b></span></div>"
                + "<div style='margin-top:6px;'>"
                + (out ? "<font color='#DC2626'><b>● STOK HABIS</b></font>" : "<font color='#16A34A'><b>✔ Stok: " + p.getTotalStock() + " pasang</b></font>")
                + "</div></center></html>";

        JButton b = new JButton(html, ImageUtil.load(p.getImage(), 130, 95)) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int s = 3;
                // Hard Shadow
                g.setColor(UiUtil.BORDER_DARK);
                g.fillRect(s, s, w - s, h - s);
                // Card Fill
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, w - s, h - s);
                // Solid Border 2px
                g.setColor(UiUtil.BORDER_DARK);
                g.setStroke(new BasicStroke(2f));
                g.drawRect(0, 0, w - s - 1, h - s - 1);
                g.dispose();
                super.paintComponent(g0);
            }
        };
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(6, 6, 9, 9));
        b.setVerticalTextPosition(SwingConstants.BOTTOM);
        b.setHorizontalTextPosition(SwingConstants.CENTER);
        b.setPreferredSize(new Dimension(160, 205));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> chooseVariant(p));
        return b;
    }

    /** Enter pada kolom SKU: jika SKU persis ditemukan -> langsung masuk keranjang; jika tidak -> filter grid. */
    private void searchOrAdd() {
        String kw = txtSku.getText().trim();
        if (kw.isEmpty()) { loadGrid(""); return; }
        try {
            Variant v = ctl.findBySku(kw);
            if (v != null) {
                if (v.getStock() <= 0) { lblInfo.setText("Stok " + v.getSku() + " habis."); return; }
                int qty = txtQty.getText().isEmpty() ? 1 : Integer.parseInt(txtQty.getText());
                addToCart(v, qty);
                txtSku.setText(""); txtQty.setText("1"); txtSku.requestFocus();
            } else {
                lblInfo.setText("SKU '" + kw + "' tidak ditemukan persis - menampilkan hasil pencarian.");
                loadGrid(kw);
            }
        } catch (SQLException ex) { UiUtil.dbError(this, ex); }
    }

    /** Klik kartu produk -> pilih ukuran/warna & qty. */
    private void chooseVariant(Product p) {
        try {
            List<Variant> all = ctl.getVariantsOfProduct(p.getId());
            List<Variant> avail = new ArrayList<>();
            for (Variant v : all) if (v.getStock() > 0) avail.add(v);
            if (avail.isEmpty()) { UiUtil.warn(this, "Stok semua varian " + p.getName() + " habis."); return; }

            JComboBox<Variant> cmb = new JComboBox<>(avail.toArray(new Variant[0]));
            cmb.setRenderer(new DefaultListCellRenderer() {
                @Override public Component getListCellRendererComponent(JList<?> l, Object val, int i, boolean s, boolean f) {
                    super.getListCellRendererComponent(l, val, i, s, f);
                    if (val instanceof Variant) {
                        Variant v = (Variant) val;
                        setText("Ukuran " + v.getSize() + " / " + v.getColor() + "  (" + v.getSku() + ")  stok: " + v.getStock());
                    }
                    return this;
                }
            });
            JTextField qty = UiUtil.digitsOnly(new JTextField("1", 5));
            JPanel form = UiUtil.form("Produk", new JLabel(p.getName() + " - " + Rupiah.format(p.getPrice())),
                    "Varian", cmb, "Qty", qty);
            if (JOptionPane.showConfirmDialog(this, form, "Pilih Varian", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
            addToCart((Variant) cmb.getSelectedItem(), UiUtil.parseNonNegInt(qty.getText(), "Qty"));
        } catch (IllegalArgumentException ex) {
            UiUtil.warn(this, ex.getMessage());
        } catch (SQLException ex) { UiUtil.dbError(this, ex); }
    }

    // ----------------------------------------------------------------- keranjang
    private void addToCart(Variant v, int qty) {
        String err = ctl.addToCart(cart, v, qty);
        if (err != null) { UiUtil.warn(this, err); return; }
        lblInfo.setText("Ditambahkan: " + v.getProductName() + " " + v.getSize() + "/" + v.getColor() + " x" + qty);
        refreshCart();
    }

    private void changeQty(int delta) {
        int r = cartTable.getSelectedRow();
        if (r < 0) { lblInfo.setText("Pilih item di keranjang terlebih dahulu."); return; }
        String err = ctl.changeQty(cart, r, delta);
        if (err != null) UiUtil.warn(this, err);
        refreshCart();
        if (r < cart.size()) cartTable.setRowSelectionInterval(r, r);
    }

    private void removeSelected() {
        int r = cartTable.getSelectedRow();
        if (r < 0) { lblInfo.setText("Pilih item yang akan dihapus."); return; }
        cart.remove(r);
        refreshCart();
    }

    private void resetCart() {
        cart.clear();
        txtBayar.setText("");
        refreshCart();
    }

    private void refreshCart() {
        cartModel.setRowCount(0);
        for (CartItem i : cart)
            cartModel.addRow(new Object[]{i.getSku(), i.getProductName(), i.getSize(), i.getColor(),
                    i.getQty(), i.getUnitPrice(), i.getSubtotal()});
        updateTotals();
    }

    private void updateTotals() {
        BigDecimal total = ctl.calcTotal(cart);
        lblTotal.setText(Rupiah.format(total));
        BigDecimal change = Rupiah.parse(txtBayar.getText()).subtract(total);
        lblKembali.setText(change.signum() >= 0 ? Rupiah.format(change) : "Kurang " + Rupiah.format(change.negate()));
    }

    // ----------------------------------------------------------------- pembayaran + struk
    private void doPayment() {
        if (cart.isEmpty()) { UiUtil.warn(this, "Keranjang masih kosong."); return; }
        boolean tunai = cmbMetode.getSelectedIndex() == 0;
        BigDecimal total = ctl.calcTotal(cart);
        BigDecimal paid = tunai ? Rupiah.parse(txtBayar.getText()) : total;
        if (tunai && paid.compareTo(total) < 0) { UiUtil.warn(this, "Uang yang dibayarkan kurang dari total."); return; }
        if (!UiUtil.confirm(this, "Proses pembayaran sebesar " + Rupiah.format(total) + "?")) return;

        try {
            List<CartItem> items = new ArrayList<>(cart);
            Sale sale = ctl.processPayment(items, tunai ? "tunai" : "transfer", paid);
            resetCart();
            loadGrid(txtSku.getText().trim());
            printReceipt(sale, items, true);
        } catch (IllegalStateException ex) {
            UiUtil.warn(this, ex.getMessage());
            loadGrid("");
        } catch (SQLException ex) { UiUtil.dbError(this, ex); }
    }

    /** Membuat struk PDF di folder struk/ lalu membukanya (untuk dicetak). */
    private void printReceipt(Sale sale, List<CartItem> items, boolean isNew) {
        try {
            File pdf = PdfReceipt.create(sale, items);
            boolean opened = false;
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(pdf);
                    opened = true;
                }
            } catch (IOException | RuntimeException ignore) { }
            UiUtil.info(this, (isNew ? "Transaksi berhasil: " + sale.getInvoiceNo() + "\nKembalian: " + Rupiah.format(sale.getChangeAmount()) + "\n\n" : "")
                    + "Struk PDF " + (opened ? "dibuka." : "tersimpan di:\n" + pdf.getAbsolutePath()));
        } catch (IOException ex) {
            UiUtil.warn(this, "Transaksi tersimpan, tetapi struk PDF gagal dibuat: " + ex.getMessage());
        }
    }

    // ================================================================= TAB: RIWAYAT
    private JComponent buildHistoryTab() {
        JButton bShow = UiUtil.button("🔍 Tampilkan", UiUtil.ACCENT);
        JButton bPrint = UiUtil.button("🖨️ Lihat / Cetak Ulang Struk (PDF)", UiUtil.SUCCESS);

        UiUtil.NeoPanel tb = new UiUtil.NeoPanel(new FlowLayout(FlowLayout.LEFT, 10, 8), Color.WHITE, 3, 0);
        tb.setPadding(6, 10, 6, 10);

        JLabel lFrom = new JLabel("Dari:"); lFrom.setFont(UiUtil.FONT_BOLD);
        JLabel lTo = new JLabel("Sampai:"); lTo.setFont(UiUtil.FONT_BOLD);

        tb.add(lFrom); tb.add(spFrom);
        tb.add(lTo); tb.add(spTo);
        tb.add(bShow); tb.add(bPrint);
        if ("kasir".equalsIgnoreCase(user.getRoleName())) {
            JLabel lKasirNotice = new JLabel("  (menampilkan transaksi Anda sendiri)");
            lKasirNotice.setFont(UiUtil.FONT_REGULAR);
            lKasirNotice.setForeground(UiUtil.TEXT_MUTED);
            tb.add(lKasirNotice);
        }

        bShow.addActionListener(e -> loadHistory());
        bPrint.addActionListener(e -> reprint());
        histTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        p.add(tb, BorderLayout.NORTH);
        p.add(UiUtil.scroll(histTable), BorderLayout.CENTER);
        return p;
    }

    private void loadHistory() {
        LocalDate from = UiUtil.toLocalDate(spFrom), to = UiUtil.toLocalDate(spTo);
        if (from.isAfter(to)) { UiUtil.warn(this, "Tanggal 'Dari' tidak boleh setelah 'Sampai'."); return; }
        try {
            history = ctl.getHistory(from, to);
            histModel.setRowCount(0);
            for (Sale s : history)
                histModel.addRow(new Object[]{s.getInvoiceNo(), s.getCreatedAt(), s.getCashierName(),
                        "tunai".equals(s.getPaymentMethod()) ? "Tunai" : "Transfer", s.getTotal()});
        } catch (SQLException ex) { UiUtil.dbError(this, ex); }
    }

    private void reprint() {
        int r = histTable.getSelectedRow();
        if (r < 0 || r >= history.size()) { UiUtil.warn(this, "Pilih transaksi terlebih dahulu."); return; }
        try {
            Sale s = history.get(r);
            printReceipt(s, ctl.getSaleItems(s.getId()), false);
        } catch (SQLException ex) { UiUtil.dbError(this, ex); }
    }
}
