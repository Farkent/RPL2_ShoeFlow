package shoeflow.config;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.dao.DataAccessResourceFailureException;

/**
 * Pemegang Spring ApplicationContext (IoC container), dibuat sekali dan dipakai seluruh aplikasi.
 * Dibuat saat pertama kali ada yang meminta DAO, sehingga tampilan login tetap bisa muncul
 * walaupun MySQL belum menyala; kegagalan baru terlihat saat data diakses.
 */
public final class AppContext {

    private static AnnotationConfigApplicationContext context;

    private AppContext() { }

    /** Mengambil bean (DAO) dari container Spring, misalnya AppContext.getBean(ProductDao.class). */
    public static synchronized <T> T getBean(Class<T> type) {
        if (context == null) {
            try {
                context = new AnnotationConfigApplicationContext(AppConfig.class);
                Runtime.getRuntime().addShutdownHook(new Thread(AppContext::shutdown));
            } catch (RuntimeException e) {
                throw new DataAccessResourceFailureException("Gagal menginisialisasi Spring: " + e.getMessage(), e);
            }
        }
        return context.getBean(type);
    }

    public static synchronized void shutdown() {
        if (context != null) {
            context.close();
            context = null;
        }
    }
}
