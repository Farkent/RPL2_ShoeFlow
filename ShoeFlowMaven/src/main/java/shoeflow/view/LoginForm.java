package shoeflow.view;

import java.awt.*;
import java.sql.SQLException;
import javax.swing.*;
import shoeflow.controller.LoginController;
import shoeflow.model.User;
import shoeflow.view.components.UiUtil;

/**
 * Form Login Modern Soft Neobrutalism:
 * Floating card dengan border radius membulat, crisp drop shadow, palette warna hidup, dan tata letak responsif.
 */
public class LoginForm extends JFrame {

    private final JTextField txtUsername = new JTextField(18);
    private final JPasswordField txtPassword = new JPasswordField(18);
    private final JButton btnLogin = UiUtil.button("MASUK KE SISTEM", UiUtil.SUCCESS);
    private final LoginController controller = new LoginController();

    public LoginForm() {
        setTitle("ShoeFlow ERP - Portal Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(UiUtil.BG);

        // Card Container Soft Neobrutal dengan Rounded Corners
        UiUtil.NeoPanel card = new UiUtil.NeoPanel(new BorderLayout(0, 20), Color.WHITE, 5, 18);
        card.setPadding(28, 32, 28, 32);
        card.setPreferredSize(new Dimension(420, 430));
        card.setMaximumSize(new Dimension(440, 460));

        // 1. Header (Brand Sticker Badge + Subtitle)
        JPanel head = new JPanel();
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.setOpaque(false);

        JLabel sticker = new JLabel("SHOEFLOW ERP", SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int arc = 10;
                // Shadow
                g.setColor(UiUtil.BORDER_DARK);
                g.fillRoundRect(3, 3, w - 3, h - 3, arc, arc);
                // Background
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
        sticker.setFont(new Font("Segoe UI", Font.BOLD, 19));
        sticker.setForeground(UiUtil.BORDER_DARK);
        sticker.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        sticker.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Sistem Inventaris & Point of Sales Modern");
        sub.setFont(UiUtil.FONT_REGULAR.deriveFont(12.5f));
        sub.setForeground(UiUtil.TEXT_MUTED);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        head.add(sticker);
        head.add(Box.createVerticalStrut(10));
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
            new UiUtil.RoundedBorder(UiUtil.BORDER_DARK, 2, UiUtil.RADIUS_INPUT),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        g.gridx = 0; g.gridy = 0; form.add(lblUser, g);
        g.gridy = 1; form.add(txtUsername, g);
        g.gridy = 2; form.add(Box.createVerticalStrut(4), g);
        g.gridy = 3; form.add(lblPass, g);
        g.gridy = 4; form.add(txtPassword, g);

        btnLogin.setFont(UiUtil.FONT_BOLD.deriveFont(14f));
        btnLogin.setPreferredSize(new Dimension(0, 46));
        g.gridy = 5;
        g.insets = new Insets(18, 0, 8, 0);
        form.add(btnLogin, g);

        // Footer Caption
        JLabel footerNote = new JLabel("Silakan login dengan kredensial terdaftar", SwingConstants.CENTER);
        footerNote.setFont(UiUtil.FONT_SMALL);
        footerNote.setForeground(UiUtil.TEXT_MUTED);

        card.add(head, BorderLayout.NORTH);
        card.add(form, BorderLayout.CENTER);
        card.add(footerNote, BorderLayout.SOUTH);

        // Frame Layout: Responsive Centered Scroll Wrapper
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(UiUtil.BG);
        root.add(card);

        JScrollPane scrollPane = new JScrollPane(root);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(UiUtil.BG);
        setContentPane(scrollPane);

        getRootPane().setDefaultButton(btnLogin);
        btnLogin.addActionListener(e -> doLogin());

        pack();
        setSize(520, 580);
        setMinimumSize(new Dimension(400, 480));
        setLocationRelativeTo(null);
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
