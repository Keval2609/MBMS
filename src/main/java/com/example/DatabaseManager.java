package com.example;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfoService;

import java.io.InputStream;
import java.util.Properties;

public class DatabaseManager {
    
    private static HikariDataSource dataSource;
    private static Flyway flyway;

    public static void initializeDatabase() {
        System.out.println("Initializing Database Connection Pool & Migrations...");

        try {
            Properties props = new Properties();
            try (InputStream input = DatabaseManager.class.getClassLoader().getResourceAsStream("application.properties")) {
                if (input != null) {
                    props.load(input);
                }
            }

            // Environment variables override properties file if provided
            String dbUrl = System.getenv("DB_URL") != null ? System.getenv("DB_URL") 
                    : props.getProperty("db.url", "jdbc:mysql://localhost:3307/mbms_db");
            String dbUser = System.getenv("DB_USER") != null ? System.getenv("DB_USER") 
                    : props.getProperty("db.user", "mbms_user");
            String dbPassword = System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") 
                    : props.getProperty("db.password", "mbms_password");

            // Configure HikariCP connection pool
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(dbUrl);
            config.setUsername(dbUser);
            config.setPassword(dbPassword);

            // HikariCP pool sizing settings from properties
            if (props.containsKey("db.hikari.maximum-pool-size")) {
                config.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.hikari.maximum-pool-size")));
            }

            // MySQL specific performance configurations
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(config);
            System.out.println("HikariCP connection pool initialized successfully.");

            // Configure and Run Flyway
            flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations(props.getProperty("flyway.locations", "classpath:db/migration"))
                    .baselineOnMigrate(Boolean.parseBoolean(props.getProperty("flyway.baselineOnMigrate", "true")))
                    .load();

            System.out.println("Scanning for migration scripts and executing updates...");
            var result = flyway.migrate();
            System.out.println("Flyway migrations executed! Migrations applied: " + result.migrationsExecuted);

            printMigrationStatus();

        } catch (Exception e) {
            System.err.println("CRITICAL: Database initialization failed!");
            e.printStackTrace();
            throw new RuntimeException("Could not initialize database architecture", e);
        }
    }

    public static void printMigrationStatus() {
        if (flyway != null) {
            MigrationInfoService infoService = flyway.info();
            System.out.println("=== Flyway Migration Status ===");
            System.out.println("Current Schema Version: " + (infoService.current() != null ? infoService.current().getVersion() : "None"));
            for (var info : infoService.all()) {
                System.out.printf(" - [%s] %s (State: %s, Applied On: %s)%n",
                        info.getVersion(), info.getDescription(), info.getState(), info.getInstalledOn());
            }
        }
    }

    public static HikariDataSource getDataSource() {
        return dataSource;
    }

    public static Flyway getFlyway() {
        return flyway;
    }
}
