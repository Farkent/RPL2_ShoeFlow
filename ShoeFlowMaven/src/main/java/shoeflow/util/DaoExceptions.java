package shoeflow.util;

import java.sql.SQLException;

/**
 * Menjembatani DataAccessException (Spring DAO) dengan layar lama yang menangkap SQLException.
 * SQLException asli (beserta kode error MySQL, mis. 1062 / 1451) dicari di rantai penyebab,
 * sehingga isDuplicate() dan isInUse() di BaseController tetap bekerja.
 */
public final class DaoExceptions {

    private DaoExceptions() { }

    public static SQLException toSql(RuntimeException e) {
        Throwable t = e;
        while (t != null) {
            if (t instanceof SQLException) {
                SQLException orig = (SQLException) t;
                return new SQLException(orig.getMessage(), orig.getSQLState(), orig.getErrorCode(), e);
            }
            t = t.getCause();
        }
        return new SQLException(e.getMessage(), e);
    }
}
