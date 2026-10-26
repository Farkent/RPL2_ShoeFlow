package shoeflow.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import shoeflow.config.Koneksi;
import shoeflow.model.User;
import shoeflow.util.PasswordUtil;
import shoeflow.view.MainFrame;

/** Logika autentikasi dan pembukaan jendela utama sesuai role. */
public class LoginController {

    /** @return objek User jika login valid, null jika username/password salah. */
    public User login(String username, String password) throws SQLException {
        String sql = "SELECT u.id, u.role_id, r.name AS role_name, u.name, u.username, u.password "
                   + "FROM users u JOIN roles r ON r.id = u.role_id WHERE u.username = ?";
        try (Connection c = Koneksi.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);   // PreparedStatement -> aman dari SQL Injection
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && PasswordUtil.matches(password, rs.getString("password"))) {
                    return new User(rs.getInt("id"), rs.getInt("role_id"), rs.getString("role_name"),
                            rs.getString("name"), rs.getString("username"), rs.getString("password"));
                }
            }
        }
        return null;
    }

    /**
     * Membuka jendela utama. Menu di sidebar ditentukan oleh role di MainFrame:
     *  - admin / manajer : Dashboard, Produk, POS, Pembelian, Laporan
     *  - kasir           : hanya POS (Proses Transaksi / Riwayat Transaksi)
     */
    public void openMainForm(User user) {
        new MainFrame(user).setVisible(true);
    }
}
