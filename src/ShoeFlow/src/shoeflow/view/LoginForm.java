package shoeflow.view;

import java.awt.*;
import java.sql.SQLException;
import javax.swing.*;
import shoeflow.controller.LoginController;
import shoeflow.model.User;
import shoeflow.view.components.UiUtil;

/** Form Login Neobrutalism: Floating card dengan border solid hitam 3px, hard drop shadow, dan quick login chips. */
public class LoginForm extends JFrame {

    private final JTextField txtUsername = new JTextField(18);
    private final JPasswordField txtPassword = new JPasswordField(18);
    private final JButton btnLogin = UiUtil.button("MASUK KE SISTEM", UiUtil.SUCCESS);
    private final LoginController controller = new LoginController();

    public LoginForm() {
        setTitle("ShoeFlow ERP Login Portal");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(UiUtil.BG);

        // Card Container Neobrutal
        UiUtil.NeoPanel card = new UiUtil.NeoPanel(new BorderLayout(0, 16), Color.WHITE, 6, 0);
        card.setPadding(24, 28, 24, 28);
        card.setPreferredSize(new Dimension(420, 520));

        // 1. Header (Sticker Badge + Title)
        JPanel head = new JPanel();
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.setOpaque(false);

        JLabel sticker = new JLabel("SHOEFLOW ERP", SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g.setColor(UiUtil.BORDER_DARK);
                g.fillRect(3, 3, w - 3, h - 3);
                g.setColor(UiUtil.ACCENT);
                g.fillRect(0, 0, w - 3, h - 3);
                g.setColor(UiUtil.BORDER_DARK);
                g.setStroke(new BasicStroke(2f));
                g.drawRect(0, 0, w - 4, h - 4);
                g.dispose();
                super.paintComponent(g0);
            }
        };
        sticker.setFont(new Font("Segoe UI", Font.BOLD, 20));
        sticker.setForeground(UiUtil.BORDER_DARK);
        sticker.setBorder(BorderFactory.createEmptyBorder(6, 16, 8, 16));
        sticker.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Sistem Inventaris & Point of Sales Modern");
        sub.setFont(UiUtil.FONT_REGULAR.deriveFont(12f));
        sub.setForeground(UiUtil.TEXT_MUTED);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        head.add(sticker);
        head.add(Box.createVerticalStrut(8));
        head.add(sub);

        // 2. Form Input Fields
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 0, 6, 0);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1.0;

        JLabel lblUser = new JLabel("Username");
        lblUser.setFont(UiUtil.FONT_BOLD);
        lblUser.setForeground(UiUtil.BORDER_DARK);

        UiUtil.styleField(txtUsername);

        JLabel lblPass = new JLabel("Password");
        lblPass.setFont(UiUtil.FONT_BOLD);
        lblPass.setForeground(UiUtil.BORDER_DARK);

        txtPassword.setFont(UiUtil.FONT_REGULAR);
        txtPassword.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UiUtil.BORDER_DARK, 2),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));

        g.gridx = 0; g.gridy = 0; form.add(lblUser, g);
        g.gridy = 1; form.add(txtUsername, g);
        g.gridy = 2; form.add(Box.createVerticalStrut(4), g);
        g.gridy = 3; form.add(lblPass, g);
        g.gridy = 4; form.add(txtPassword, g);

        btnLogin.setFont(UiUtil.FONT_BOLD.deriveFont(13.5f));
        btnLogin.setPreferredSize(new Dimension(0, 44));
        g.gridy = 5;
        g.insets = new Insets(14, 0, 8, 0);
        form.add(btnLogin, g);

        // 3. Quick Demo Login Chips
        JPanel demoBox = new JPanel();
        demoBox.setLayout(new BoxLayout(demoBox, BoxLayout.Y_AXIS));
        demoBox.setOpaque(false);

        JLabel lblDemo = new JLabel("⚡ Akun Demo Cepat (Klik untuk isi):");
        lblDemo.setFont(UiUtil.FONT_BOLD.deriveFont(11f));
        lblDemo.setForeground(UiUtil.TEXT_MUTED);
        lblDemo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        chips.setOpaque(false);

        JButton chipAdmin = UiUtil.button("Admin", Color.WHITE);
        JButton chipManajer = UiUtil.button("Manajer", Color.WHITE);
        JButton chipKasir = UiUtil.button("Kasir", Color.WHITE);

        chipAdmin.setFont(UiUtil.FONT_BOLD.deriveFont(11f));
        chipManajer.setFont(UiUtil.FONT_BOLD.deriveFont(11f));
        chipKasir.setFont(UiUtil.FONT_BOLD.deriveFont(11f));

        chipAdmin.addActionListener(e -> fillCreds("admin", "admin123"));
        chipManajer.addActionListener(e -> fillCreds("manajer", "manajer123"));
        chipKasir.addActionListener(e -> fillCreds("kasir", "kasir123"));

        chips.add(chipAdmin);
        chips.add(chipManajer);
        chips.add(chipKasir);

        demoBox.add(lblDemo);
        demoBox.add(Box.createVerticalStrut(4));
        demoBox.add(chips);

        card.add(head, BorderLayout.NORTH);
        card.add(form, BorderLayout.CENTER);
        card.add(demoBox, BorderLayout.SOUTH);

        // Frame Layout: Center card
        setLayout(new GridBagLayout());
        add(card);

        getRootPane().setDefaultButton(btnLogin);
        btnLogin.addActionListener(e -> doLogin());

        pack();
        setSize(480, 600);
        setLocationRelativeTo(null);
    }

    private void fillCreds(String user, String pass) {
        txtUsername.setText(user);
        txtPassword.setText(pass);
        btnLogin.requestFocus();
    }

    private void doLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            UiUtil.warn(this, "Username dan password wajib diisi.");
            return;
        }
        try {
            User user = controller.login(username, password);
            if (user == null) {
                JOptionPane.showMessageDialog(this, "Username atau password salah.", "Login Gagal",
                        JOptionPane.ERROR_MESSAGE);
                txtPassword.setText("");
                return;
            }
            controller.openMainForm(user);
            dispose();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Gagal terhubung ke database:\n" + ex.getMessage()
                    + "\n\nPastikan MySQL di XAMPP sudah Start.", "Error Database", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        UiUtil.setupLookAndFeel();
        SwingUtilities.invokeLater(() -> new LoginForm().setVisible(true));
    }
}

