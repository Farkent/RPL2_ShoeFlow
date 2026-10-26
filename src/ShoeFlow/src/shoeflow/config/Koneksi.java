package shoeflow.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Koneksi JDBC ke MySQL (XAMPP).
 * Setiap pemanggilan getConnection() membuat koneksi baru, sehingga aman untuk
 * database transaction (setAutoCommit(false)) dan aman dipakai banyak pengguna bersamaan.
 * Wajib: tambahkan library "mysql-connector-j" ke Libraries proyek.
 */
public class Koneksi {

    private static final String URL  = System.getProperty("shoeflow.db.url",
            "jdbc:mysql://localhost:3306/shoeflow?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jakarta");
    private static final String USER = System.getProperty("shoeflow.db.user", "root");  // default XAMPP
    private static final String PASS = System.getProperty("shoeflow.db.pass", "");      // default XAMPP kosong

    private Koneksi() { }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
