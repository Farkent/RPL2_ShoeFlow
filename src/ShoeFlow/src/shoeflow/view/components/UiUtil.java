package shoeflow.view.components;

import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import shoeflow.util.Rupiah;

/**
 * Design System Neobrutalism untuk ShoeFlow ERP:
 *  - High-contrast solid black borders (2px - 3px)
 *  - Hard, crisp offset drop shadows (3px - 5px tanpa blur)
 *  - Vibrant pop-color palette (Electric Yellow, Mint Lime, Sky Cyan, Coral Red, Purple)
 *  - Bold, punchy typography (Segoe UI Bold)
 *  - Tactile interactive feedback (tombol mechanical click & hover)
 */
public final class UiUtil {

    // --- Neobrutalism Color Palette
    public static final Color BORDER_DARK = new Color(0x18181B);   // Deep Ink Black
    public static final Color SIDEBAR = new Color(0x121212);       // Dark Canvas Sidebar
    public static final Color SIDEBAR_HOVER = new Color(0x27272A);
    public static final Color ACCENT = new Color(0xFFE500);        // Neobrutal Electric Yellow
    public static final Color ACCENT_YELLOW = new Color(0xFFE500);
    public static final Color ACCENT_CYAN = new Color(0x38BDF8);   // Vibrant Sky Cyan
    public static final Color DANGER = new Color(0xFF4343);        // High-Energy Coral Red
    public static final Color SUCCESS = new Color(0x22C55E);       // Punchy Emerald / Mint Lime
    public static final Color PURPLE = new Color(0xA855F7);        // Electric Violet
    public static final Color ORANGE = new Color(0xFB923C);        // Tangerine Orange
    public static final Color NEUTRAL = new Color(0x52525B);       // Slate Zinc
    public static final Color BG = new Color(0xFAF8F5);            // Warm Neobrutal Cream
    public static final Color CARD_BG = Color.WHITE;
    public static final Color LOW_ROW = new Color(0xFEE2E2);       // Warning Pink / Red
    public static final Color TEXT_MAIN = new Color(0x18181B);
    public static final Color TEXT_MUTED = new Color(0x52525B);

    // --- Typography Tokens
    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_BIG = new Font("Segoe UI", Font.BOLD, 26);

    private UiUtil() { }

    // ------------------------------------------------------------ Inisialisasi Look & Feel
    public static void setupLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignore) { }

        UIManager.put("Panel.background", BG);
        UIManager.put("Label.font", FONT_REGULAR);
        UIManager.put("Label.foreground", TEXT_MAIN);
        UIManager.put("Button.font", FONT_BOLD);
        UIManager.put("TextField.font", FONT_REGULAR);
        UIManager.put("Table.font", FONT_REGULAR);
        UIManager.put("TableHeader.font", FONT_BOLD);
        UIManager.put("TabbedPane.font", FONT_BOLD);
        UIManager.put("TabbedPane.background", Color.WHITE);
        UIManager.put("TabbedPane.foreground", TEXT_MAIN);
        UIManager.put("TabbedPane.selected", ACCENT);
        UIManager.put("TabbedPane.contentBorderInsets", new Insets(2, 2, 2, 2));
        UIManager.put("TitledBorder.font", FONT_BOLD);
        UIManager.put("TitledBorder.titleColor", BORDER_DARK);
    }

    // ------------------------------------------------------------ NeoPanel (Card / Container)
    public static class NeoPanel extends JPanel {
        private Color fillColor = Color.WHITE;
        private Color strokeColor = BORDER_DARK;
        private Color shadowColor = BORDER_DARK;
        private int strokeWidth = 2;
        private int shadowOffset = 4;
        private int arc = 0;

        public NeoPanel() { this(Color.WHITE, 4, 0); }
        public NeoPanel(Color fill) { this(fill, 4, 0); }
        public NeoPanel(Color fill, int shadow, int arc) {
            super();
            this.fillColor = fill;
            this.shadowOffset = shadow;
            this.arc = arc;
            setOpaque(false);
            setPadding(12, 14, 12, 14);
        }
        public NeoPanel(LayoutManager layout, Color fill, int shadow, int arc) {
            super(layout);
            this.fillColor = fill;
            this.shadowOffset = shadow;
            this.arc = arc;
            setOpaque(false);
            setPadding(12, 14, 12, 14);
        }

        public void setPadding(int top, int left, int bottom, int right) {
            setBorder(BorderFactory.createEmptyBorder(
                top + strokeWidth,
                left + strokeWidth,
                bottom + strokeWidth + shadowOffset,
                right + strokeWidth + shadowOffset
            ));
        }

        public void setFillColor(Color c) {
            this.fillColor = c;
            repaint();
        }

        public Color getFillColor() { return fillColor; }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            int s = shadowOffset;
            int bw = w - s;
            int bh = h - s;

            // 1. Drop shadow (solid hitam tanpa blur)
            if (s > 0) {
                g.setColor(shadowColor);
                if (arc > 0) g.fillRoundRect(s, s, bw, bh, arc, arc);
                else g.fillRect(s, s, bw, bh);
            }

            // 2. Body fill
            g.setColor(fillColor);
            if (arc > 0) g.fillRoundRect(0, 0, bw, bh, arc, arc);
            else g.fillRect(0, 0, bw, bh);

            // 3. Solid outline border
            g.setColor(strokeColor);
            g.setStroke(new BasicStroke(strokeWidth));
            if (arc > 0) g.drawRoundRect(strokeWidth / 2, strokeWidth / 2, bw - strokeWidth, bh - strokeWidth, arc, arc);
            else g.drawRect(0, 0, bw - 1, bh - 1);

            g.dispose();
            super.paintComponent(g0);
        }
    }

    public static NeoPanel panel(Color bg) { return new NeoPanel(bg, 4, 0); }
    public static NeoPanel card(Color bg) { return new NeoPanel(bg, 4, 0); }

    // ------------------------------------------------------------ NeoButton (Tactile Mechanical Click)
    public static class NeoButton extends JButton {
        private Color bg;
        private Color fg;
        private int shadow = 3;
        private boolean hover = false;
        private boolean pressed = false;

        public NeoButton(String text, Color bg, Color fg) {
            super(text);
            this.bg = bg;
            this.fg = fg;
            setOpaque(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(FONT_BOLD);
            setForeground(fg);
            setBorder(BorderFactory.createEmptyBorder(8, 14, 8 + shadow, 14 + shadow));

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; pressed = false; repaint(); }
                @Override public void mousePressed(MouseEvent e) { pressed = true; repaint(); }
                @Override public void mouseReleased(MouseEvent e) { pressed = false; repaint(); }
            });
        }

        @Override public void setBackground(Color c) {
            this.bg = c;
            super.setBackground(c);
            repaint();
        }

        @Override public void setForeground(Color c) {
            this.fg = c;
            super.setForeground(c);
            repaint();
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int s = shadow;

            int ox = pressed ? s : 0;
            int oy = pressed ? s : 0;
            int bw = w - s;
            int bh = h - s;

            // 1. Drop shadow jika tidak ditekan
            if (!pressed && isEnabled() && s > 0) {
                g.setColor(BORDER_DARK);
                g.fillRect(s, s, bw, bh);
            }

            // 2. Background button
            Color fill = !isEnabled() ? new Color(0xE2E8F0) : (hover && !pressed ? brighten(bg, 0.08f) : bg);
            g.setColor(fill);
            g.fillRect(ox, oy, bw, bh);

            // 3. Border solid hitam 2px
            g.setColor(BORDER_DARK);
            g.setStroke(new BasicStroke(2f));
            g.drawRect(ox + 1, oy + 1, bw - 2, bh - 2);

            // 4. Delegasi penggambaran icon & teks ke Swing (mendukung HTML & multi-line)
            Graphics2D gContent = (Graphics2D) g0.create();
            gContent.translate(ox, oy);
            super.paintComponent(gContent);
            gContent.dispose();

            g.dispose();
        }
    }

    public static JButton button(String text, Color bg) {
        Color fg = isDark(bg) ? Color.WHITE : BORDER_DARK;
        return new NeoButton(text, bg, fg);
    }

    public static JButton button(String text, Color bg, Color fg) {
        return new NeoButton(text, bg, fg);
    }

    // ------------------------------------------------------------ NeoBadge (Tag / Pill)
    public static JLabel badge(String text, Color bg, Color fg) {
        JLabel l = new JLabel(text, SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                // Shadow
                g.setColor(BORDER_DARK);
                g.fillRect(2, 2, w - 2, h - 2);
                // Fill
                g.setColor(bg);
                g.fillRect(0, 0, w - 2, h - 2);
                // Border
                g.setColor(BORDER_DARK);
                g.setStroke(new BasicStroke(1.5f));
                g.drawRect(0, 0, w - 3, h - 3);
                g.dispose();
                super.paintComponent(g0);
            }
        };
        l.setOpaque(false);
        l.setFont(FONT_BOLD.deriveFont(11f));
        l.setForeground(fg);
        l.setBorder(BorderFactory.createEmptyBorder(3, 8, 5, 8));
        return l;
    }

    public static JLabel title(String text) {
        JLabel l = new JLabel("⚡ " + text);
        l.setFont(FONT_TITLE);
        l.setForeground(TEXT_MAIN);
        l.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        return l;
    }

    // ------------------------------------------------------------ Form Input Styling
    public static JTextField styleField(JTextField f) {
        f.setFont(FONT_REGULAR);
        f.setBackground(Color.WHITE);
        f.setForeground(TEXT_MAIN);
        f.setCaretColor(TEXT_MAIN);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_DARK, 2),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        return f;
    }

    public static <T> JComboBox<T> styleCombo(JComboBox<T> c) {
        c.setFont(FONT_REGULAR);
        c.setBackground(Color.WHITE);
        c.setForeground(TEXT_MAIN);
        c.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_DARK, 2),
            BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));
        return c;
    }

    /** Form 2 kolom dengan Neobrutal labels & auto styling pada input fields */
    public static JPanel form(Object... labelAndField) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;
        for (int i = 0; i + 1 < labelAndField.length; i += 2) {
            g.gridx = 0; g.gridy = i / 2; g.fill = GridBagConstraints.NONE; g.weightx = 0;
            JLabel l = new JLabel(String.valueOf(labelAndField[i]));
            l.setFont(FONT_BOLD);
            l.setForeground(TEXT_MAIN);
            p.add(l, g);
            g.gridx = 1; g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1;
            Component comp = (Component) labelAndField[i + 1];
            if (comp instanceof JTextField) styleField((JTextField) comp);
            else if (comp instanceof JComboBox) styleCombo((JComboBox<?>) comp);
            p.add(comp, g);
        }
        return p;
    }

    public static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ------------------------------------------------------------ Dialog Pesan
    public static void info(Component p, String msg) {
        JOptionPane.showMessageDialog(p, msg, "Informasi", JOptionPane.INFORMATION_MESSAGE);
    }
    public static void warn(Component p, String msg) {
        JOptionPane.showMessageDialog(p, msg, "Perhatian", JOptionPane.WARNING_MESSAGE);
    }
    public static void dbError(Component p, Exception e) {
        JOptionPane.showMessageDialog(p, "Kesalahan database:\n" + e.getMessage(), "Error Database", JOptionPane.ERROR_MESSAGE);
    }
    public static boolean confirm(Component p, String msg) {
        return JOptionPane.showConfirmDialog(p, msg, "Konfirmasi", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    // ------------------------------------------------------------ Validasi Input
    public static JTextField digitsOnly(JTextField f) {
        ((AbstractDocument) f.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override public void insertString(FilterBypass fb, int off, String s, AttributeSet a) throws BadLocationException {
                if (s != null) super.insertString(fb, off, s.replaceAll("[^0-9]", ""), a);
            }
            @Override public void replace(FilterBypass fb, int off, int len, String s, AttributeSet a) throws BadLocationException {
                super.replace(fb, off, len, s == null ? null : s.replaceAll("[^0-9]", ""), a);
            }
        });
        return styleField(f);
    }

    public static int parseNonNegInt(String s, String field) {
        if (s == null || s.trim().isEmpty()) throw new IllegalArgumentException(field + " wajib diisi.");
        try {
            int v = Integer.parseInt(s.trim());
            if (v < 0) throw new IllegalArgumentException(field + " tidak boleh negatif.");
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(field + " harus berupa angka bulat 0 atau lebih.");
        }
    }

    // ------------------------------------------------------------ Tanggal
    public static JSpinner dateSpinner(LocalDate d) {
        Date date = Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
        JSpinner s = new JSpinner(new SpinnerDateModel(date, null, null, Calendar.DAY_OF_MONTH));
        s.setEditor(new JSpinner.DateEditor(s, "dd/MM/yyyy"));
        s.setFont(FONT_REGULAR);
        s.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_DARK, 2),
            BorderFactory.createEmptyBorder(3, 6, 3, 6)
        ));
        return s;
    }

    public static LocalDate toLocalDate(JSpinner s) {
        return ((Date) s.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    // ------------------------------------------------------------ Neobrutal Table Styling
    public static <T extends JTable> T style(T t) {
        t.setRowHeight(32);
        t.setFont(FONT_REGULAR);
        t.setGridColor(new Color(0xE4E4E7));
        t.setShowGrid(true);
        t.setShowHorizontalLines(true);
        t.setShowVerticalLines(true);
        t.setSelectionBackground(ACCENT);
        t.setSelectionForeground(BORDER_DARK);
        t.setFillsViewportHeight(true);

        JTableHeader h = t.getTableHeader();
        h.setFont(FONT_BOLD);
        h.setReorderingAllowed(false);
        h.setPreferredSize(new Dimension(0, 36));
        h.setBackground(BORDER_DARK);
        h.setForeground(Color.WHITE);
        h.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, BORDER_DARK));

        h.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                l.setBackground(BORDER_DARK);
                l.setForeground(ACCENT);
                l.setFont(FONT_BOLD);
                l.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 2, 1, new Color(0x3F3F46)),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)
                ));
                return l;
            }
        });

        DefaultTableCellRenderer money = new DefaultTableCellRenderer() {
            @Override protected void setValue(Object v) {
                setText(v instanceof BigDecimal ? Rupiah.format((BigDecimal) v) : String.valueOf(v));
            }
        };
        money.setHorizontalAlignment(SwingConstants.RIGHT);
        money.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        t.setDefaultRenderer(BigDecimal.class, money);

        final SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        DefaultTableCellRenderer dateRenderer = new DefaultTableCellRenderer() {
            @Override protected void setValue(Object v) { setText(v == null ? "-" : df.format((Date) v)); }
        };
        dateRenderer.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        t.setDefaultRenderer(Date.class, dateRenderer);

        DefaultTableCellRenderer strRenderer = new DefaultTableCellRenderer();
        strRenderer.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        t.setDefaultRenderer(String.class, strRenderer);

        DefaultTableCellRenderer intRenderer = new DefaultTableCellRenderer() {
            { setHorizontalAlignment(SwingConstants.RIGHT); setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8)); }
        };
        t.setDefaultRenderer(Integer.class, intRenderer);

        return t;
    }

    public static JScrollPane horizontalScroll(JTable t) {
        t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JScrollPane sp = new JScrollPane(t, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setBorder(BorderFactory.createLineBorder(BORDER_DARK, 2));
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    public static JScrollPane scroll(Component view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setBorder(BorderFactory.createLineBorder(BORDER_DARK, 2));
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    public static void packColumns(JTable t) {
        int rows = Math.min(t.getRowCount(), 100);
        for (int c = 0; c < t.getColumnCount(); c++) {
            TableColumn col = t.getColumnModel().getColumn(c);
            Component hc = t.getTableHeader().getDefaultRenderer()
                    .getTableCellRendererComponent(t, col.getHeaderValue(), false, false, -1, c);
            int w = hc.getPreferredSize().width;
            for (int r = 0; r < rows; r++) {
                Component cc = t.prepareRenderer(t.getCellRenderer(r, c), r, c);
                w = Math.max(w, cc.getPreferredSize().width);
            }
            col.setPreferredWidth(w + 24);
        }
    }

    public static JTable lowStockTable(final ReadOnlyModel m, final int stockCol, final int minCol) {
        JTable t = new JTable(m) {
            @Override public Component prepareRenderer(javax.swing.table.TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row)) {
                    int mr = convertRowIndexToModel(row);
                    Object s = m.getValueAt(mr, stockCol), mn = m.getValueAt(mr, minCol);
                    boolean low = s instanceof Integer && mn instanceof Integer && (Integer) s <= (Integer) mn;
                    c.setBackground(low ? LOW_ROW : Color.WHITE);
                    c.setForeground(low ? DANGER.darker() : TEXT_MAIN);
                    c.setFont(low ? FONT_BOLD : FONT_REGULAR);
                } else {
                    c.setBackground(ACCENT);
                    c.setForeground(BORDER_DARK);
                    c.setFont(FONT_BOLD);
                }
                return c;
            }
        };
        return style(t);
    }

    // ------------------------------------------------------------ Utility Helpers
    public static boolean isDark(Color c) {
        double luma = 0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue();
        return luma < 140;
    }

    public static Color brighten(Color c, float factor) {
        int r = Math.min(255, (int)(c.getRed() * (1 + factor)));
        int g = Math.min(255, (int)(c.getGreen() * (1 + factor)));
        int b = Math.min(255, (int)(c.getBlue() * (1 + factor)));
        return new Color(r, g, b, c.getAlpha());
    }
}

