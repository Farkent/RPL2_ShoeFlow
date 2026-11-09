package shoeflow.view;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import javax.swing.*;
import shoeflow.model.User;
import shoeflow.util.Roles;
import shoeflow.view.components.Refreshable;
import shoeflow.view.components.UiUtil;

/**
 * Jendela utama ShoeFlow ERP - Modern Soft Neobrutalism:
 *  - Border radius pada seluruh elemen navigasi, kartu user, dan container
 *  - Palette warna-warni yang kaya (Colorful Neobrutalism) per menu
 *  - Transisi visual aktif/inaktif yang elegan
 *  - Desain sepenuhnya responsif saat diminimalkan (minisize) atau dimaksimalkan (fullsize)
 */
public class MainFrame extends JFrame {

    private final User user;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, Supplier<JComponent>> factories = new LinkedHashMap<>();
    private final Map<String, JComponent> created = new LinkedHashMap<>();
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();

    // Menu titles tanpa emoji, dengan typography bersih & modern
    private static final Map<String, String> LABELS = Map.of(
        "Dashboard", "Dashboard",
        "Manajemen Produk", "Manajemen Produk",
        "Point of Sales", "Point of Sales",
        "Manajemen Pembelian", "Manajemen Pembelian",
        "Laporan", "Laporan Bisnis"
    );

    // Warna tema khusus untuk masing-masing menu agar GUI lebih colorful
    private static final Map<String, Color> MENU_COLORS = Map.of(
        "Dashboard", UiUtil.ACCENT_CYAN,
        "Manajemen Produk", UiUtil.PURPLE,
        "Point of Sales", UiUtil.SUCCESS,
        "Manajemen Pembelian", UiUtil.ORANGE,
        "Laporan", UiUtil.PINK
    );

    public MainFrame(User user) {
        UiUtil.setupLookAndFeel();
        this.user = user;
        boolean management = Roles.ADMIN.equalsIgnoreCase(user.getRoleName()) || Roles.MANAJER.equalsIgnoreCase(user.getRoleName());

        setTitle("ShoeFlow ERP - " + user.getName() + " (" + user.getRoleName().toUpperCase() + ")");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(880, 560));
        setLocationRelativeTo(null);

        // Routing berdasarkan RBAC
        if (management) {
            factories.put("Dashboard", () -> new DashboardPanel(user));
            factories.put("Manajemen Produk", () -> new ProductPanel(user));
        }
        factories.put("Point of Sales", () -> new PosPanel(user));
        if (management) {
            factories.put("Manajemen Pembelian", () -> new PurchasePanel(user));
            factories.put("Laporan", () -> new ReportPanel(user));
        }

        setLayout(new BorderLayout());
        add(buildSidebar(), BorderLayout.WEST);
        content.setBackground(UiUtil.BG);
        add(content, BorderLayout.CENTER);

        showPage(factories.keySet().iterator().next());
    }

    private JComponent buildSidebar() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(UiUtil.SIDEBAR);
        side.setPreferredSize(new Dimension(240, 0));
        side.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 0, 2, UiUtil.BORDER_DARK),
            BorderFactory.createEmptyBorder(18, 14, 18, 14)
        ));

        // Brand Sticker Badge dengan sudut membulat
        JPanel brandBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        brandBox.setOpaque(false);
        brandBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel brandSticker = new JLabel("SHOEFLOW", SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int arc = 10;
                // Shadow
                g.setColor(new Color(0x020617));
                g.fillRoundRect(3, 3, w - 3, h - 3, arc, arc);
                // Fill
                g.setColor(UiUtil.ACCENT);
                g.fillRoundRect(0, 0, w - 3, h - 3, arc, arc);
                // Border
                g.setColor(UiUtil.BORDER_DARK);
                g.setStroke(new BasicStroke(2f));
                g.drawRoundRect(0, 0, w - 4, h - 4, arc, arc);
                g.dispose();
                super.paintComponent(g0);
            }
        };
        brandSticker.setOpaque(false);
        brandSticker.setFont(new Font("Segoe UI", Font.BOLD, 17));
        brandSticker.setForeground(UiUtil.BORDER_DARK);
        brandSticker.setBorder(BorderFactory.createEmptyBorder(6, 14, 7, 14));
        brandBox.add(brandSticker);

        JLabel erpTag = new JLabel("ENTERPRISE ERP");
        erpTag.setFont(new Font("Segoe UI", Font.BOLD, 10));
        erpTag.setForeground(new Color(0x94A3B8));
        erpTag.setAlignmentX(Component.LEFT_ALIGNMENT);

        // User Info Card Rounded
        JPanel userCard = new JPanel(new BorderLayout(8, 4)) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(new Color(0x1E293B));
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g.setColor(new Color(0x334155));
                g.setStroke(new BasicStroke(1.5f));
                g.drawRoundRect(0, 0, getWidth() - 2, getHeight() - 2, 10, 10);
                g.dispose();
                super.paintComponent(g0);
            }
        };
        userCard.setOpaque(false);
        userCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        userCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        userCard.setPreferredSize(new Dimension(212, 50));
        userCard.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JLabel uName = new JLabel(user.getName());
        uName.setFont(UiUtil.FONT_BOLD);
        uName.setForeground(Color.WHITE);

        Color roleBg = Roles.ADMIN.equalsIgnoreCase(user.getRoleName()) ? UiUtil.DANGER
                     : Roles.MANAJER.equalsIgnoreCase(user.getRoleName()) ? UiUtil.PURPLE
                     : UiUtil.ACCENT;
        Color roleFg = Roles.KASIR.equalsIgnoreCase(user.getRoleName()) ? UiUtil.BORDER_DARK : Color.WHITE;
        JLabel uRole = UiUtil.badge(user.getRoleName().toUpperCase(), roleBg, roleFg);

        userCard.add(uName, BorderLayout.WEST);
        userCard.add(uRole, BorderLayout.EAST);

        side.add(brandBox);
        side.add(Box.createVerticalStrut(4));
        side.add(erpTag);
        side.add(Box.createVerticalStrut(14));
        side.add(userCard);
        side.add(Box.createVerticalStrut(18));

        // Navigation Buttons dengan Border Radius & Color Coding
        for (String key : factories.keySet()) {
            String label = LABELS.getOrDefault(key, key);
            Color menuColor = MENU_COLORS.getOrDefault(key, UiUtil.ACCENT);

            JButton b = new JButton(label) {
                @Override protected void paintComponent(Graphics g0) {
                    Graphics2D g = (Graphics2D) g0.create();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int w = getWidth(), h = getHeight();
                    int arc = UiUtil.RADIUS_BTN;
                    boolean active = getBackground().equals(menuColor);

                    if (active) {
                        // Drop shadow rounded saat aktif
                        g.setColor(new Color(0x020617));
                        g.fillRoundRect(2, 2, w - 3, h - 3, arc, arc);
                        g.setColor(menuColor);
                        g.fillRoundRect(0, 0, w - 3, h - 3, arc, arc);
                        g.setColor(UiUtil.BORDER_DARK);
                        g.setStroke(new BasicStroke(2f));
                        g.drawRoundRect(0, 0, w - 4, h - 4, arc, arc);
                    } else {
                        // Inactive soft pill
                        g.setColor(getBackground());
                        g.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
                        g.setColor(new Color(0x334155));
                        g.setStroke(new BasicStroke(1f));
                        g.drawRoundRect(0, 0, w - 2, h - 2, arc, arc);
                    }
                    g.dispose();
                    super.paintComponent(g0);
                }
            };
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            b.setPreferredSize(new Dimension(212, 42));
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setFont(UiUtil.FONT_BOLD.deriveFont(13f));
            b.setOpaque(false);
            b.setContentAreaFilled(false);
            b.setFocusPainted(false);
            b.setBorderPainted(false);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
            b.addActionListener(e -> showPage(key));

            navButtons.put(key, b);
            side.add(b);
            side.add(Box.createVerticalStrut(8));
        }

        side.add(Box.createVerticalGlue());

        JButton logout = UiUtil.button("Logout Sistem", UiUtil.DANGER);
        logout.setAlignmentX(Component.LEFT_ALIGNMENT);
        logout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        logout.setPreferredSize(new Dimension(212, 40));
        logout.addActionListener(e -> {
            if (UiUtil.confirm(this, "Apakah Anda yakin ingin keluar dari ShoeFlow?")) {
                dispose();
                new LoginForm().setVisible(true);
            }
        });
        side.add(logout);

        // Bungkus dalam scrollbar jika layar vertikal sangat kecil
        JScrollPane sideScroll = new JScrollPane(side);
        sideScroll.setBorder(null);
        sideScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sideScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        sideScroll.getViewport().setBackground(UiUtil.SIDEBAR);
        return sideScroll;
    }

    /** Menampilkan halaman terpilih dan mengatur status visual tombol aktif/inaktif */
    private void showPage(String key) {
        Supplier<JComponent> f = factories.get(key);
        if (f == null) { UiUtil.warn(this, "Anda tidak memiliki akses ke halaman ini."); return; }
        try {
            JComponent page = created.get(key);
            if (page == null) {
                page = f.get();
                created.put(key, page);
                content.add(page, key);
            }
            cards.show(content, key);
            if (page instanceof Refreshable) ((Refreshable) page).refresh();
        } catch (SecurityException ex) {
            UiUtil.warn(this, ex.getMessage());
            return;
        }

        // Update styling tombol navigasi (Colorful Active vs Inactive)
        for (Map.Entry<String, JButton> e : navButtons.entrySet()) {
            boolean active = e.getKey().equals(key);
            JButton b = e.getValue();
            Color menuColor = MENU_COLORS.getOrDefault(e.getKey(), UiUtil.ACCENT);
            if (active) {
                b.setBackground(menuColor);
                b.setForeground(UiUtil.isDark(menuColor) ? Color.WHITE : UiUtil.BORDER_DARK);
            } else {
                b.setBackground(UiUtil.SIDEBAR_HOVER);
                b.setForeground(new Color(0xCBD5E1));
            }
            b.repaint();
        }
    }
}
