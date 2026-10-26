package shoeflow.view;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import shoeflow.controller.ProductController;
import shoeflow.model.Product;
import shoeflow.model.User;
import shoeflow.model.Variant;
import shoeflow.util.ImageUtil;
import shoeflow.util.Rupiah;
import shoeflow.view.components.*;

/** Manajemen Produk: Master Model -> Varian & Stok, plus Daftar Inventaris (scroll horizontal). */
public class ProductPanel extends JPanel implements Refreshable {

    private final ProductController ctl;
    private final JTextField txtSearch = new JTextField(20);

    // Master model
    private final ReadOnlyModel prodModel = new ReadOnlyModel("Merek", "Model", "Harga Jual", "Total Stok", "Jml Varian");
    private final JTable prodTable = UiUtil.style(new JTable(prodModel));
    private List<Product> products = new ArrayList<>();
    private final JLabel imgPreview = new JLabel("", SwingConstants.CENTER);

    // Varian
    private final ReadOnlyModel varModel = new ReadOnlyModel("Ukuran", "Warna", "SKU", "Stok", "Stok Min.", "Status");
    private final JTable varTable = UiUtil.lowStockTable(varModel, 3, 4);
    private List<Variant> variants = new ArrayList<>();

    // Inventaris
    private final JTextField txtInvSearch = new JTextField(20);
    private final ReadOnlyModel invModel = new ReadOnlyModel("Merek", "Model", "Ukuran", "Warna", "SKU", "Harga Jual", "Stok", "Stok Min.", "Status", "Nilai Stok (Harga Jual)");
    private final JTable invTable = UiUtil.lowStockTable(invModel, 6, 7);

    public ProductPanel(User user) {
        ctl = new ProductController(user);   // melempar SecurityException bila bukan admin/manajer
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(UiUtil.BG);
        add(UiUtil.title("Manajemen Produk & Inventaris"), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Master Model & Varian", buildMasterTab());
        tabs.addTab("Daftar Inventaris", buildInventoryTab());
        tabs.addChangeListener(e -> { if (tabs.getSelectedIndex() == 1) loadInventory(); });
        add(tabs, BorderLayout.CENTER);
    }

    // ================================================================= TAB 1
    private JComponent buildMasterTab() {
        // --- toolbar model
        JButton bSearch = UiUtil.button("Cari", UiUtil.ACCENT);
        JButton bAdd = UiUtil.button("Tambah Model", UiUtil.SUCCESS);
        JButton bEdit = UiUtil.button("Edit Model", Color.WHITE);
        JButton bDel = UiUtil.button("Hapus Model", UiUtil.DANGER);

        UiUtil.styleField(txtSearch);

        UiUtil.NeoPanel tb = new UiUtil.NeoPanel(new FlowLayout(FlowLayout.LEFT, 8, 6), Color.WHITE, 3, 0);
        tb.setPadding(6, 10, 6, 10);
        JLabel lPrompt = new JLabel("Cari merek / model / SKU:");
        lPrompt.setFont(UiUtil.FONT_BOLD);
        tb.add(lPrompt); tb.add(txtSearch); tb.add(bSearch);
        tb.add(bAdd); tb.add(bEdit); tb.add(bDel);

        bSearch.addActionListener(e -> loadProducts());
        txtSearch.addActionListener(e -> loadProducts());
        bAdd.addActionListener(e -> editProduct(null));
        bEdit.addActionListener(e -> { Product p = selectedProduct(); if (p != null) editProduct(p); });
        bDel.addActionListener(e -> deleteProduct());

        prodTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        prodTable.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) onProductSelected(); });

        UiUtil.NeoPanel imgBox = new UiUtil.NeoPanel(new BorderLayout(0, 4), Color.WHITE, 3, 0);
        imgBox.setPadding(8, 8, 8, 8);
        imgBox.setPreferredSize(new Dimension(170, 170));
        JLabel imgTitle = new JLabel("Foto Sepatu", SwingConstants.CENTER);
        imgTitle.setFont(UiUtil.FONT_BOLD.deriveFont(11.5f));
        imgPreview.setPreferredSize(new Dimension(150, 130));
        imgBox.add(imgTitle, BorderLayout.NORTH);
        imgBox.add(imgPreview, BorderLayout.CENTER);

        JPanel upper = new JPanel(new BorderLayout(10, 10));
        upper.setOpaque(false);
        upper.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        upper.add(tb, BorderLayout.NORTH);
        upper.add(UiUtil.scroll(prodTable), BorderLayout.CENTER);
        upper.add(imgBox, BorderLayout.EAST);

        // --- varian
        JButton vAdd = UiUtil.button("Tambah Varian", UiUtil.SUCCESS);
        JButton vEdit = UiUtil.button("Edit Varian", Color.WHITE);
        JButton vDel = UiUtil.button("Hapus Varian", UiUtil.DANGER);

        UiUtil.NeoPanel vb = new UiUtil.NeoPanel(new FlowLayout(FlowLayout.LEFT, 8, 6), Color.WHITE, 3, 0);
        vb.setPadding(6, 10, 6, 10);
        JLabel lVarPrompt = new JLabel("Varian ukuran/warna & stok dari model terpilih:");
        lVarPrompt.setFont(UiUtil.FONT_BOLD);
        vb.add(lVarPrompt); vb.add(vAdd); vb.add(vEdit); vb.add(vDel);

        vAdd.addActionListener(e -> editVariant(null));
        vEdit.addActionListener(e -> { Variant v = selectedVariant(); if (v != null) editVariant(v); });
        vDel.addActionListener(e -> deleteVariant());
        varTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel lower = new JPanel(new BorderLayout(0, 8));
        lower.setOpaque(false);
        lower.add(vb, BorderLayout.NORTH);
        lower.add(UiUtil.scroll(varTable), BorderLayout.CENTER);

        JSplitPane sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, upper, lower);
        sp.setResizeWeight(0.5);
        sp.setOpaque(false);
        sp.setBorder(null);
        sp.setDividerSize(8);
        return sp;
    }

    // ================================================================= TAB 2
    private JComponent buildInventoryTab() {
        JButton b = UiUtil.button("Cari", UiUtil.ACCENT);
        JButton ex = UiUtil.button("Export CSV", UiUtil.SUCCESS);

        UiUtil.styleField(txtInvSearch);

        UiUtil.NeoPanel tb = new UiUtil.NeoPanel(new FlowLayout(FlowLayout.LEFT, 8, 6), Color.WHITE, 3, 0);
        tb.setPadding(6, 10, 6, 10);
        JLabel lCari = new JLabel("Cari:");
        lCari.setFont(UiUtil.FONT_BOLD);
        tb.add(lCari); tb.add(txtInvSearch); tb.add(b); tb.add(ex);

        JLabel lNotice = new JLabel("   (baris merah = stok mencapai batas minimum)");
        lNotice.setFont(UiUtil.FONT_REGULAR);
        lNotice.setForeground(UiUtil.DANGER.darker());
        tb.add(lNotice);

        b.addActionListener(e -> loadInventory());
        txtInvSearch.addActionListener(e -> loadInventory());
        ex.addActionListener(e -> {
            try { File f = shoeflow.util.CsvExporter.export(this, invModel, "inventaris_shoeflow"); if (f != null) UiUtil.info(this, "Tersimpan: " + f.getAbsolutePath()); }
            catch (IOException ioe) { UiUtil.warn(this, "Gagal export: " + ioe.getMessage()); }
        });
        invTable.setAutoCreateRowSorter(true);

        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        p.add(tb, BorderLayout.NORTH);
        p.add(UiUtil.horizontalScroll(invTable), BorderLayout.CENTER);
        return p;
    }

    // ================================================================= data
    @Override public void refresh() { loadProducts(); }

    private void loadProducts() {
        try {
            int keep = prodTable.getSelectedRow();
            products = ctl.listProducts(txtSearch.getText());
            prodModel.setRowCount(0);
            for (Product p : products)
                prodModel.addRow(new Object[]{p.getBrand(), p.getModel(), p.getPrice(), p.getTotalStock(), p.getVariantCount()});
            if (!products.isEmpty()) {
                int row = Math.min(Math.max(keep, 0), products.size() - 1);
                prodTable.setRowSelectionInterval(row, row);
            } else {
                onProductSelected();
            }
        } catch (SQLException ex) { UiUtil.dbError(this, ex); }
    }

    private void onProductSelected() {
        Product p = selectedProduct();
        variants = new ArrayList<>();
        varModel.setRowCount(0);
        if (p == null) { imgPreview.setIcon(null); return; }
        imgPreview.setIcon(ImageUtil.load(p.getImage(), 130, 110));
        try {
            variants = ctl.listVariants(p.getId());
            for (Variant v : variants)
                varModel.addRow(new Object[]{v.getSize(), v.getColor(), v.getSku(), v.getStock(), v.getMinStock(),
                        v.getStock() <= v.getMinStock() ? "MENIPIS" : "Aman"});
        } catch (SQLException ex) { UiUtil.dbError(this, ex); }
    }

    private void loadInventory() {
        try {
            invModel.setRowCount(0);
            for (Variant v : ctl.listInventory(txtInvSearch.getText()))
                invModel.addRow(new Object[]{v.getBrand(), v.getModel(), v.getSize(), v.getColor(), v.getSku(), v.getPrice(),
                        v.getStock(), v.getMinStock(), v.getStock() <= v.getMinStock() ? "MENIPIS" : "Aman",
                        v.getPrice().multiply(java.math.BigDecimal.valueOf(v.getStock()))});
            UiUtil.packColumns(invTable);
        } catch (SQLException ex) { UiUtil.dbError(this, ex); }
    }

    private Product selectedProduct() {
        int r = prodTable.getSelectedRow();
        if (r < 0 || r >= products.size()) { return null; }
        return products.get(r);
    }

    private Variant selectedVariant() {
        int r = varTable.getSelectedRow();
        if (r < 0 || r >= variants.size()) { UiUtil.warn(this, "Pilih varian terlebih dahulu."); return null; }
        return variants.get(r);
    }

    // ================================================================= dialog Model
    private void editProduct(Product existing) {
        final Product p = existing == null ? new Product() : existing;
        JTextField tBrand = new JTextField(existing == null ? "" : p.getBrand(), 20);
        JTextField tModel = new JTextField(existing == null ? "" : p.getModel(), 20);
        JTextField tPrice = UiUtil.digitsOnly(new JTextField(existing == null ? "" : p.getPrice().toBigInteger().toString(), 20));
        final String[] img = {existing == null ? null : p.getImage()};
        JLabel lblImg = new JLabel(ImageUtil.load(img[0], 96, 80));
        JButton bImg = new JButton("Pilih Gambar...");
        bImg.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Gambar (png, jpg)", "png", "jpg", "jpeg"));
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                try { img[0] = ImageUtil.copyToImages(fc.getSelectedFile()); lblImg.setIcon(ImageUtil.load(img[0], 96, 80)); }
                catch (IOException ex) { UiUtil.warn(this, "Gagal menyalin gambar: " + ex.getMessage()); }
            }
        });
        JPanel imgBox = new JPanel(new FlowLayout(FlowLayout.LEFT));
        imgBox.add(lblImg); imgBox.add(bImg);
        JPanel form = UiUtil.form("Merek", tBrand, "Model", tModel, "Harga Jual (Rp)", tPrice, "Gambar", imgBox);

        while (true) {
            if (JOptionPane.showConfirmDialog(this, form, existing == null ? "Tambah Model Sepatu" : "Edit Model Sepatu",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
            try {
                p.setBrand(tBrand.getText());
                p.setModel(tModel.getText());
                p.setPrice(Rupiah.parse(tPrice.getText()));
                p.setImage(img[0]);
                ctl.saveProduct(p);
                loadProducts();
                return;
            } catch (IllegalArgumentException | IllegalStateException ex) {
                UiUtil.warn(this, ex.getMessage());   // validasi gagal -> form ditampilkan lagi
            } catch (SQLException ex) { UiUtil.dbError(this, ex); return; }
        }
    }

    private void deleteProduct() {
        Product p = selectedProduct();
        if (p == null) { UiUtil.warn(this, "Pilih model terlebih dahulu."); return; }
        if (!UiUtil.confirm(this, "Hapus model '" + p.getName() + "' beserta semua variannya?")) return;
        try { ctl.deleteProduct(p.getId()); loadProducts(); }
        catch (IllegalStateException ex) { UiUtil.warn(this, ex.getMessage()); }
        catch (SQLException ex) { UiUtil.dbError(this, ex); }
    }

    // ================================================================= dialog Varian
    private void editVariant(Variant existing) {
        final Product p = selectedProduct();
        if (p == null) { UiUtil.warn(this, "Pilih model sepatu terlebih dahulu."); return; }
        final Variant v = existing == null ? new Variant() : existing;

        JTextField tSize = new JTextField(existing == null ? "" : v.getSize(), 12);
        JTextField tColor = new JTextField(existing == null ? "" : v.getColor(), 12);
        JTextField tSku = new JTextField(existing == null ? "" : v.getSku(), 18);
        JTextField tStock = UiUtil.digitsOnly(new JTextField(existing == null ? "0" : String.valueOf(v.getStock()), 8));
        JTextField tMin = UiUtil.digitsOnly(new JTextField(existing == null ? "2" : String.valueOf(v.getMinStock()), 8));
        JButton auto = new JButton("Auto");
        auto.addActionListener(e -> tSku.setText(ProductController.suggestSku(p, tSize.getText(), tColor.getText())));
        JPanel skuBox = new JPanel(new BorderLayout(4, 0));
        skuBox.add(tSku, BorderLayout.CENTER); skuBox.add(auto, BorderLayout.EAST);
        JPanel form = UiUtil.form("Model", new JLabel(p.getName()), "Ukuran", tSize, "Warna", tColor,
                "SKU", skuBox, "Stok", tStock, "Stok Minimum", tMin);

        while (true) {
            if (JOptionPane.showConfirmDialog(this, form, existing == null ? "Tambah Varian" : "Edit Varian",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
            try {
                v.setProductId(p.getId());
                v.setSize(tSize.getText());
                v.setColor(tColor.getText());
                v.setSku(tSku.getText());
                v.setStock(UiUtil.parseNonNegInt(tStock.getText(), "Stok"));          // validasi anti negatif
                v.setMinStock(UiUtil.parseNonNegInt(tMin.getText(), "Stok minimum"));
                ctl.saveVariant(v);
                loadProducts();
                return;
            } catch (IllegalArgumentException | IllegalStateException ex) {
                UiUtil.warn(this, ex.getMessage());
            } catch (SQLException ex) { UiUtil.dbError(this, ex); return; }
        }
    }

    private void deleteVariant() {
        Variant v = selectedVariant();
        if (v == null) return;
        if (!UiUtil.confirm(this, "Hapus varian " + v.getSku() + "?")) return;
        try { ctl.deleteVariant(v.getId()); loadProducts(); }
        catch (IllegalStateException ex) { UiUtil.warn(this, ex.getMessage()); }
        catch (SQLException ex) { UiUtil.dbError(this, ex); }
    }
}
