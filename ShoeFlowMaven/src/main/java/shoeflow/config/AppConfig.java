package shoeflow.config;

import jakarta.persistence.EntityManagerFactory;
import java.util.Properties;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Konfigurasi Spring (pengganti applicationContext.xml) untuk lapisan DAO.
 * Semua objek yang dibutuhkan DAO dibuat dan dihubungkan oleh Spring (Dependency Injection / IoC):
 *
 *   DataSource  ->  EntityManagerFactory (Hibernate)  ->  JpaTransactionManager
 *        |                                                       |
 *   JdbcTemplate                                      @Transactional pada DAO
 *
 * URL, user, dan password database tetap diambil dari Koneksi.java (satu tempat pengaturan).
 */
@Configuration
@ComponentScan(basePackages = "shoeflow.dao")   // mencari class bertanda @Repository di package dao
@EnableTransactionManagement                    // mengaktifkan @Transactional (declarative transaction)
public class AppConfig {

    /** Sumber koneksi database untuk Spring (dipakai Hibernate maupun JdbcTemplate). */
    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setUrl(Koneksi.getUrl());
        ds.setUsername(Koneksi.getUser());
        ds.setPassword(Koneksi.getPass());
        return ds;
    }

    /** Dipakai DAO berbasis JDBC (UserDaoImpl). Spring yang membuka/menutup koneksi dan menangani SQLException. */
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /** Hibernate dikelola Spring: class @Entity di package shoeflow.model dipetakan otomatis. */
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setPackagesToScan("shoeflow.model");
        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Properties p = new Properties();
        p.setProperty("hibernate.hbm2ddl.auto", "none");   // tabel dibuat manual di MySQL
        p.setProperty("hibernate.show_sql", "true");        // tampilkan SQL buatan Hibernate di console
        p.setProperty("hibernate.format_sql", "true");
        emf.setJpaProperties(p);
        return emf;
    }

    /** Pengelola transaksi: commit bila method @Transactional sukses, rollback bila terjadi RuntimeException. */
    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    /** Mengubah exception teknologi (Hibernate/JPA) pada class @Repository menjadi DataAccessException milik Spring. */
    @Bean
    public static PersistenceExceptionTranslationPostProcessor exceptionTranslation() {
        return new PersistenceExceptionTranslationPostProcessor();
    }
}
