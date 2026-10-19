package com.example.library.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;


public final class DataSourceConfig {
    private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/library";

    private DataSourceConfig() { }

    public static DataSource createDataSource() {
        return createDataSource(
                env("DB_URL", DEFAULT_URL),
                env("DB_USER", "postgres"),
                env("DB_PASSWORD", "159753"),
                Integer.parseInt(env("DB_POOL_SIZE", "10")));
    }

    public static DataSource createDataSource(String url, String user, String password) {
        return createDataSource(url, user, password, 10);
    }

    public static DataSource createDataSource(String url, String user, String password, int maxPoolSize) {
        HikariConfig cfg = new HikariConfig();
        cfg.setPoolName("library-pool");
        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(password);


        cfg.setMaximumPoolSize(maxPoolSize);
        cfg.setMinimumIdle(Math.min(2, maxPoolSize));

        // Таймауты (мс)
        cfg.setConnectionTimeout(30_000);   // сколько ждать свободное соединение из пула
        cfg.setValidationTimeout(5_000);    // сколько ждать проверку соединения (< connectionTimeout)
        cfg.setIdleTimeout(600_000);        // простаивающие соединения сверх minimumIdle закрываются через 10 мин
        cfg.setMaxLifetime(1_800_000);      // пересоздание соединений каждые 30 мин (меньше таймаутов БД/файрвола)
        cfg.setKeepaliveTime(300_000);      // пинг неактивных соединений раз в 5 мин


        cfg.setLeakDetectionThreshold(20_000);


        cfg.setAutoCommit(true);
        return new HikariDataSource(cfg);
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
