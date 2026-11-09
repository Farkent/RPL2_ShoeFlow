package shoeflow.dao;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import shoeflow.model.User;

/**
 * Implementasi UserDao dengan JDBC lewat Spring JdbcTemplate.
 * Contoh bahwa satu pola DAO yang sama dapat dipakai untuk teknologi berbeda (JDBC di sini, Hibernate di ProductDaoImpl):
 * Spring yang membuka/menutup Connection, PreparedStatement, ResultSet, dan mengubah SQLException menjadi DataAccessException.
 */
@Repository
public class UserDaoImpl implements UserDao {

    private static final String SQL =
            "SELECT u.id, u.role_id, r.name AS role_name, u.name, u.username, u.password "
          + "FROM users u JOIN roles r ON r.id = u.role_id WHERE u.username = ?";

    /** Pemetaan satu baris hasil query menjadi objek User. */
    private static final RowMapper<User> MAPPER = (rs, rowNum) -> new User(
            rs.getInt("id"), rs.getInt("role_id"), rs.getString("role_name"),
            rs.getString("name"), rs.getString("username"), rs.getString("password"));

    private final JdbcTemplate jdbc;

    /** Constructor injection: Spring menyuntikkan JdbcTemplate yang didefinisikan di AppConfig. */
    public UserDaoImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        List<User> result = jdbc.query(SQL, MAPPER, username);   // parameter ? aman dari SQL Injection
        return result.isEmpty() ? null : result.get(0);
    }
}
