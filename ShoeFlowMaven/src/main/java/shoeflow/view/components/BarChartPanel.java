package shoeflow.view.components;

import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

/**
 * Grafik batang Modern Soft Neobrutalism:
 * Border radius rounded pada card & batang grafik, soft drop shadow, serta siklus warna vibran.
 * Responsif mengikuti ukuran container.
 */
public class BarChartPanel extends JPanel {
    private List<String> labels = new ArrayList<>();
    private List<BigDecimal> values = new ArrayList<>();
    private String title = "";

    // Neobrutal Color Palette siklus untuk batang grafik
    private static final Color[] BAR_COLORS = {
        new Color(0xFFE500), // Sunshine Yellow
        new Color(0x38BDF8), // Sky Cyan
        new Color(0x10B981), // Mint Emerald
        new Color(0xA855F7), // Electric Purple
        new Color(0xFB923C), // Tangerine
        new Color(0xEC4899), // Rose Pink
        new Color(0x3B82F6), // Royal Blue
        new Color(0x14B8A6)  // Teal
    };

    public BarChartPanel(String title) {
        this.title = title;
        setOpaque(false);
        setPreferredSize(new Dimension(600, 270));
        setMinimumSize(new Dimension(360, 220));
    }

    public void setData(List<String> labels, List<BigDecimal> values) {
        this.labels = labels;
        this.values = values;
        repaint();
    }

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();
        int shadow = 4;
        int arc = UiUtil.RADIUS_CARD;
        int bwCard = w - shadow;
        int bhCard = h - shadow;

        if (bwCard <= 20 || bhCard <= 20) {
            g.dispose();
            return;
        }

        // 1. Gambar Container Card Soft Neobrutal Rounded
        g.setColor(UiUtil.BORDER_DARK);
        g.fillRoundRect(shadow, shadow, bwCard, bhCard, arc, arc); // Drop Shadow

        g.setColor(Color.WHITE);
        g.fillRoundRect(0, 0, bwCard, bhCard, arc, arc); // Body

        g.setColor(UiUtil.BORDER_DARK);
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(0, 0, bwCard - 1, bhCard - 1, arc, arc); // Border

        // Header Title Bar Rounded Top
        Shape oldClip = g.getClip();
        g.clipRect(0, 0, bwCard, 36);
        g.setColor(UiUtil.BORDER_DARK);
        g.fillRoundRect(0, 0, bwCard, 48, arc, arc);
        g.setClip(oldClip);

        g.setColor(UiUtil.ACCENT);
        g.setFont(UiUtil.FONT_BOLD);
        g.drawString(title.toUpperCase(), 16, 23);

        int left = 70, right = 24, top = 54, bottom = 34;
        int cw = bwCard - left - right, ch = bhCard - top - bottom;

        if (cw <= 10 || ch <= 10) {
            g.dispose();
            return;
        }

        double max = 0;
        for (BigDecimal v : values) max = Math.max(max, v.doubleValue());
        if (max <= 0) max = 1;
        max = niceMax(max);

        // Garis bantu horizontal + label sumbu Y
        Stroke dashed = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4, 4}, 0);

        for (int i = 0; i <= 4; i++) {
            int y = top + ch - (int) (ch * i / 4.0);
            g.setColor(new Color(0xE2E8F0));
            g.setStroke(dashed);
            g.drawLine(left, y, bwCard - right, y);

            g.setColor(UiUtil.TEXT_MUTED);
            g.setFont(UiUtil.FONT_BOLD.deriveFont(10.5f));
            String t = compact(max * i / 4.0);
            g.drawString(t, left - 8 - g.getFontMetrics().stringWidth(t), y + 4);
        }

        // Garis dasar sumbu X
        g.setColor(UiUtil.BORDER_DARK);
        g.setStroke(new BasicStroke(2f));
        g.drawLine(left - 4, top + ch, bwCard - right, top + ch);

        if (values.isEmpty()) {
            g.setColor(UiUtil.TEXT_MUTED);
            g.setFont(UiUtil.FONT_REGULAR);
            String empty = "Belum ada data penjualan tercatat.";
            g.drawString(empty, left + (cw - g.getFontMetrics().stringWidth(empty)) / 2, top + ch / 2);
            g.dispose();
            return;
        }

        int n = Math.max(values.size(), 1);
        double slot = (double) cw / n;
        int bw = (int) Math.max(10, slot * 0.55);
        int barArc = Math.min(8, bw);

        for (int i = 0; i < values.size(); i++) {
            double v = values.get(i).doubleValue();
            int bh = (int) (ch * v / max);
            int x = left + (int) (slot * i + (slot - bw) / 2);
            int y = top + ch - bh;

            if (bh > 0) {
                // 1. Drop shadow batang grafik rounded
                g.setColor(UiUtil.BORDER_DARK);
                g.fillRoundRect(x + 2, y + 2, bw, bh, barArc, barArc);

                // 2. Isi batang grafik dengan warna cerah rounded
                Color barColor = BAR_COLORS[i % BAR_COLORS.length];
                g.setColor(barColor);
                g.fillRoundRect(x, y, bw, bh, barArc, barArc);

                // 3. Border solid hitam batang grafik rounded
                g.setColor(UiUtil.BORDER_DARK);
                g.setStroke(new BasicStroke(1.5f));
                g.drawRoundRect(x, y, bw, bh, barArc, barArc);

                // 4. Label nilai di atas batang (bold)
                g.setFont(UiUtil.FONT_BOLD.deriveFont(10.5f));
                String t = compact(v);
                int tw = g.getFontMetrics().stringWidth(t);
                g.setColor(UiUtil.TEXT_MAIN);
                g.drawString(t, x + (bw - tw) / 2, y - 5);
            }

            // Label waktu di bawah sumbu X
            g.setColor(UiUtil.TEXT_MAIN);
            g.setFont(UiUtil.FONT_BOLD.deriveFont(10.5f));
            String lb = labels.get(i);
            int lw = g.getFontMetrics().stringWidth(lb);
            g.drawString(lb, x + (bw - lw) / 2, top + ch + 18);
        }

        g.dispose();
    }

    private static double niceMax(double v) {
        double p = Math.pow(10, Math.floor(Math.log10(v)));
        double f = v / p;
        double nf = f <= 1 ? 1 : f <= 2 ? 2 : f <= 5 ? 5 : 10;
        return nf * p;
    }

    static String compact(double v) {
        if (v >= 1_000_000_000) return String.format("%.1f M", v / 1_000_000_000).replace('.', ',');
        if (v >= 1_000_000) return String.format("%.1f jt", v / 1_000_000).replace('.', ',');
        if (v >= 1_000) return String.format("%.0f rb", v / 1_000);
        return String.format("%.0f", v);
    }
}
