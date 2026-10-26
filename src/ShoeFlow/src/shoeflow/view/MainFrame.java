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
 * Jendela utama ShoeFlow ERP - Tema Neobrutalism:
 *  - Sidebar hitam tegas dengan border pemisah solid 3px
 *  - Sticker badge ShoeFlow kuning terang
 *  - Role pill dengan warna Neobrutal (Admin: Merah, Manajer: Ungu, Kasir: Kuning)
 *  - Tombol navigasi taktil dengan efek mechanical click
 */
public class MainFrame extends JFrame {

    private final User user;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, Supplier<JComponent>> factories = new LinkedHashMap<>();
    private final Map<String, JComponent> created = new LinkedHashMap<>();
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();

    private static final Map<String, String> ICONS = Map.of(
        "Dashboard", "Dashboard",
        "Manajemen Produk", "Manajemen Produk",
        "Point of Sales", "Point of Sales",
        "Manajemen Pembelian", "Manajemen Pembelian",
        "Laporan", "Laporan Bisnis"
    );

    public MainFrame(User user) {
        UiUtil.setupLookAndFeel();
        this.user = user;
        boolean management = Roles.ADMIN.equalsIgnoreCase(user.getRoleName()) || Roles.MANAJER.equalsIgnoreCase(user.getRoleName());

        setTitle("ShoeFlow ERP " + user.getName() + " (" + user.getRoleName().toUpperCase() + ")");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 780);
        setMinimumSize(new Dimension(1120, 680));
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

    private JPanel buildSidebar() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(UiUtil.SIDEBAR);
        side.setPreferredSize(new Dimension(240, 0));
        side.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 0, 3, UiUtil.BORDER_DARK),
            BorderFactory.createEmptyBorder(20, 14, 20, 14)
        ));

        // Brand Sticker Badge
        JPanel brandBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        brandBox.setOpaque(false);
        brandBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel brandSticker = new JLabel("SHOEFLOW", SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                // Shadow
                g.setColor(Color.BLACK);
                g.fillRect(3, 3, w - 3, h - 3);
                // Fill
                g.setColor(UiUtil.ACCENT);
                g.fillRect(0, 0, w - 3, h - 3);
                // Border
                g.setColor(UiUtil.BORDER_DARK);
                g.setStroke(new BasicStroke(2f));
                g.drawRect(0, 0, w - 4, h - 4);
                g.dispose();
                super.paintComponent(g0);
            }
        };
        brandSticker.setOpaque(false);
        brandSticker.setFont(new Font("Segoe UI", Font.BOLD, 18));
        brandSticker.setForeground(UiUtil.BORDER_DARK);
        brandSticker.setBorder(BorderFactory.createEmptyBorder(6, 12, 8, 12));
        brandBox.add(brandSticker);

        JLabel erpTag = new JLabel("ENTERPRISE ERP");
        erpTag.setFont(new Font("Segoe UI", Font.BOLD, 10));
        erpTag.setForeground(new Color(0xA1A1AA));
        erpTag.setAlignmentX(Component.LEFT_ALIGNMENT);

        // User Info Card
        JPanel userCard = new JPanel(new BorderLayout(8, 4));
        userCard.setOpaque(false);
        userCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        userCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        userCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x3F3F46), 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

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
        side.add(Box.createVerticalStrut(16));
        side.add(userCard);
        side.add(Box.createVerticalStrut(20));

        // Navigation Buttons
        for (String key : factories.keySet()) {
            String label = ICONS.getOrDefault(key, key);
            JButton b = new JButton(label);
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setFont(UiUtil.FONT_BOLD.deriveFont(13f));
            b.setOpaque(false);
            b.setFocusPainted(false);
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
        logout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        logout.addActionListener(e -> {
            if (UiUtil.confirm(this, "Apakah Anda yakin ingin keluar dari ShoeFlow?")) {
                dispose();
                new LoginForm().setVisible(true);
            }
        });
        side.add(logout);
        return side;
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

        // Update styling tombol navigasi (Neobrutal Active vs Inactive)
        for (Map.Entry<String, JButton> e : navButtons.entrySet()) {
            boolean active = e.getKey().equals(key);
            JButton b = e.getValue();
            if (active) {
                b.setBackground(UiUtil.ACCENT);
                b.setForeground(UiUtil.BORDER_DARK);
                b.setOpaque(true);
                b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UiUtil.BORDER_DARK, 2),
                    BorderFactory.createEmptyBorder(0, 12, 0, 12)
                ));
            } else {
                b.setBackground(UiUtil.SIDEBAR_HOVER);
                b.setForeground(new Color(0xD4D4D8));
                b.setOpaque(true);
                b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0x3F3F46), 1),
                    BorderFactory.createEmptyBorder(0, 13, 0, 13)
                ));
            }
        }
    }
}

