package shoeflow.view;

import java.awt.*;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.*;
import shoeflow.controller.DashboardController;
import shoeflow.model.User;
import shoeflow.util.Rupiah;
import shoeflow.view.components.*;

/** Dashboard Neobrutalism: ringkasan penjualan hari ini, grafik bulanan, dan alert stok minimum. */
public class DashboardPanel extends JPanel implements Refreshable {

    private final DashboardController ctl;
    private final JLabel vToday = new JLabel("-"), vTrx = new JLabel("-"), vItems = new JLabel("-"), vLow = new JLabel("-");
    private final BarChartPanel chart = new BarChartPanel("Grafik Penjualan Bulanan (12 bulan terakhir)");
    private final ReadOnlyModel alertModel = new ReadOnlyModel("SKU", "Produk", "Ukuran", "Warna", "Stok", "Stok Min.");
    private final JLabel banner = new JLabel(" ");
    private final UiUtil.NeoPanel bannerPanel = new UiUtil.NeoPanel(UiUtil.SUCCESS, 3, 0);

    public DashboardPanel(User user) {
        ctl = new DashboardController(user);
        setLayout(new BorderLayout(14, 14));
        setBorder(BorderFactory.createEmptyBorder(18, 20, 20, 20));
        setBackground(UiUtil.BG);

        // Header Top Bar
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(UiUtil.title("Dashboard & Statistik Penjualan"), BorderLayout.WEST);

        JButton refresh = UiUtil.button("Refresh Data", UiUtil.ACCENT);
        refresh.addActionListener(e -> refresh());
        top.add(refresh, BorderLayout.EAST);

        // 4 Neobrutalist Stat Cards
        JPanel cardsRow = new JPanel(new GridLayout(1, 4, 14, 0));
        cardsRow.setOpaque(false);
        cardsRow.add(statCard("PENJUALAN HARI INI", vToday, "Omset transaksi hari ini", UiUtil.ACCENT, "[ OMSET ]"));
        cardsRow.add(statCard("JUMLAH TRANSAKSI", vTrx, "Struk kasir diterbitkan", UiUtil.SUCCESS, "[ TRANSAKSI ]"));
        cardsRow.add(statCard("PASANG TERJUAL", vItems, "Volume sepatu terjual", UiUtil.ACCENT_CYAN, "[ VOLUME ]"));
        cardsRow.add(statCard("VARIAN MENIPIS", vLow, "Stok <= batas minimum", UiUtil.DANGER, "[ PERINGATAN ]"));

        JPanel north = new JPanel(new BorderLayout(0, 14));
        north.setOpaque(false);
        north.add(top, BorderLayout.NORTH);
        north.add(cardsRow, BorderLayout.CENTER);

        // Alert Banner Stok - Neobrutal Box
        banner.setFont(UiUtil.FONT_BOLD.deriveFont(13.5f));
        banner.setForeground(UiUtil.BORDER_DARK);
        bannerPanel.setLayout(new BorderLayout());
        bannerPanel.setPadding(8, 14, 8, 14);
        bannerPanel.add(banner, BorderLayout.CENTER);

        // Alert Table Container
        JTable alertTable = UiUtil.lowStockTable(alertModel, 4, 5);
        UiUtil.NeoPanel alertContainer = new UiUtil.NeoPanel(new BorderLayout(0, 8), Color.WHITE, 4, 0);
        alertContainer.setPadding(10, 12, 10, 12);

        JLabel alertHeader = new JLabel("DAFTAR VARIAN KRITIS (Mencapai Batas Stok Minimum)");
        alertHeader.setFont(UiUtil.FONT_BOLD.deriveFont(13f));
        alertHeader.setForeground(UiUtil.BORDER_DARK);

        alertContainer.add(alertHeader, BorderLayout.NORTH);
        alertContainer.add(UiUtil.scroll(alertTable), BorderLayout.CENTER);

        JPanel alertBox = new JPanel(new BorderLayout(0, 10));
        alertBox.setOpaque(false);
        alertBox.add(bannerPanel, BorderLayout.NORTH);
        alertBox.add(alertContainer, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, chart, alertBox);
        split.setResizeWeight(0.5);
        split.setBorder(null);
        split.setOpaque(false);
        split.setDividerSize(8);

        add(north, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
    }

    private JPanel statCard(String title, JLabel value, String subtitle, Color accent, String tagText) {
        UiUtil.NeoPanel p = new UiUtil.NeoPanel(new BorderLayout(0, 8), Color.WHITE, 4, 0);
        p.setPadding(10, 12, 10, 12);

        // Top Row: Title + Neobrutal Tag Badge
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JLabel t = new JLabel(title);
        t.setFont(UiUtil.FONT_BOLD.deriveFont(11.5f));
        t.setForeground(UiUtil.TEXT_MUTED);

        JLabel tag = UiUtil.badge(tagText, accent, UiUtil.isDark(accent) ? Color.WHITE : UiUtil.BORDER_DARK);

        topRow.add(t, BorderLayout.WEST);
        topRow.add(tag, BorderLayout.EAST);

        // Value in Large Bold Font
        value.setFont(UiUtil.FONT_BIG);
        value.setForeground(UiUtil.BORDER_DARK);

        // Subtitle caption
        JLabel sub = new JLabel(subtitle);
        sub.setFont(UiUtil.FONT_REGULAR.deriveFont(11f));
        sub.setForeground(UiUtil.TEXT_MUTED);

        JPanel centerBox = new JPanel(new BorderLayout(0, 2));
        centerBox.setOpaque(false);
        centerBox.add(value, BorderLayout.NORTH);
        centerBox.add(sub, BorderLayout.CENTER);

        p.add(topRow, BorderLayout.NORTH);
        p.add(centerBox, BorderLayout.CENTER);
        return p;
    }

    /** Dimuat di thread terpisah (SwingWorker) agar jendela tidak membeku. */
    @Override public void refresh() {
        new SwingWorker<DashboardController.Data, Void>() {
            @Override protected DashboardController.Data doInBackground() throws Exception { return ctl.load(); }
            @Override protected void done() {
                try {
                    DashboardController.Data d = get();
                    vToday.setText(Rupiah.format(d.todayTotal));
                    vTrx.setText(d.todayTrx + " Trx");
                    vItems.setText(d.todayItems + " Pasang");
                    vLow.setText(d.lowStock.size() + " Varian");
                    alertModel.setRows(d.lowStock);

                    boolean safe = d.lowStock.isEmpty();
                    banner.setText(safe
                            ? "SEMUA AMAN: Seluruh stok varian sepatu berada di atas batas minimum."
                            : "PERINGATAN KRITIS: " + d.lowStock.size() + " varian sepatu mencapai batas minimum — Segera buat Purchase Order!");
                    banner.setForeground(safe ? UiUtil.BORDER_DARK : Color.WHITE);
                    bannerPanel.setFillColor(safe ? UiUtil.SUCCESS : UiUtil.DANGER);

                    List<String> labels = new ArrayList<>();
                    List<BigDecimal> vals = new ArrayList<>();
                    for (Map.Entry<YearMonth, BigDecimal> e : d.monthly.entrySet()) {
                        labels.add(e.getKey().getMonth().getDisplayName(TextStyle.SHORT, new Locale("id", "ID")) + " " + String.valueOf(e.getKey().getYear()).substring(2));
                        vals.add(e.getValue());
                    }
                    chart.setData(labels, vals);
                } catch (Exception ex) {
                    UiUtil.warn(DashboardPanel.this, "Gagal memuat dashboard: " + ex.getMessage());
                }
            }
        }.execute();
    }
}

