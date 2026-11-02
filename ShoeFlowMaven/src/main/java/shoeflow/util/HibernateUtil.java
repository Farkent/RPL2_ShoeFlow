package shoeflow.util;

import jakarta.persistence.PersistenceException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import shoeflow.config.Koneksi;

/**
 * Pengelola SessionFactory Hibernate (dibuat sekali, dipakai seluruh aplikasi).
 * SessionFactory dibuat saat pertama kali dibutuhkan, sehingga login dan POS yang
 * memakai JDBC tidak terpengaruh bila konfigurasi Hibernate bermasalah.
 */
public final class HibernateUtil {

    private static SessionFactory sessionFactory;

    private HibernateUtil() { }

    public static synchronized SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            try {
                Configuration cfg = new Configuration().configure();   // membaca hibernate.cfg.xml
                cfg.setProperty("hibernate.connection.url", Koneksi.getUrl());
                cfg.setProperty("hibernate.connection.username", Koneksi.getUser());
                cfg.setProperty("hibernate.connection.password", Koneksi.getPass());
                sessionFactory = cfg.buildSessionFactory();
                Runtime.getRuntime().addShutdownHook(new Thread(HibernateUtil::shutdown));
            } catch (RuntimeException e) {
                throw new PersistenceException("Gagal menginisialisasi Hibernate: " + e.getMessage(), e);
            }
        }
        return sessionFactory;
    }

    /** Membuka Session baru. Wajib ditutup (pakai try-with-resources). */
    public static Session openSession() {
        return getSessionFactory().openSession();
    }

    public static synchronized void shutdown() {
        if (sessionFactory != null) {
            sessionFactory.close();
            sessionFactory = null;
        }
    }
}
