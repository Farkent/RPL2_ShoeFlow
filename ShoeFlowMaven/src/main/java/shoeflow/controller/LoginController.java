package shoeflow.controller;

import java.sql.SQLException;
import shoeflow.config.AppContext;
import shoeflow.dao.UserDao;
import shoeflow.model.User;
import shoeflow.util.DaoExceptions;
import shoeflow.util.PasswordUtil;
import shoeflow.view.MainFrame;

/** Logika autentikasi (lewat Spring DAO) dan pembukaan jendela utama sesuai role. */
public class LoginController {

    /**
     * Pengecekan username dilakukan UserDao (Spring DAO + JdbcTemplate), sedangkan pencocokan password tetap di sini.
     * @return objek User jika login valid, null jika username/password salah.
     */
    public User login(String username, String password) throws SQLException {
        try {
            User u = AppContext.getBean(UserDao.class).findByUsername(username);
            if (u != null && PasswordUtil.matches(password, u.getPassword())) return u;
            return null;
        } catch (RuntimeException e) {          // DataAccessException (Spring) -> SQLException agar LoginForm tidak berubah
            throw DaoExceptions.toSql(e);
        }
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
