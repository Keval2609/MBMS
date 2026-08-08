package com.example;

import org.junit.jupiter.api.Test;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseMigrationTest {

    @Test
    public void testApplicationPropertiesLoad() throws Exception {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            assertNotNull(input, "application.properties should exist in resources");
            props.load(input);
        }

        assertEquals("jdbc:mysql://localhost:3307/mbms_db", props.getProperty("db.url"));
        assertEquals("mbms_user", props.getProperty("db.user"));
        assertEquals("dell7390", props.getProperty("db.password"));
        assertEquals("classpath:db/migration", props.getProperty("flyway.locations"));
    }

    @Test
    public void testMigrationScriptsExist() {
        InputStream v1Script = getClass().getClassLoader().getResourceAsStream("db/migration/V1__Initial_schema.sql");
        assertNotNull(v1Script, "V1__Initial_schema.sql should exist on classpath");

        InputStream v2Script = getClass().getClassLoader().getResourceAsStream("db/migration/V2__Seed_initial_data.sql");
        assertNotNull(v2Script, "V2__Seed_initial_data.sql should exist on classpath");
    }
}
