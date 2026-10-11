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
        assertEquals("mbms_password", props.getProperty("db.password"));
        assertEquals("classpath:db/migration", props.getProperty("flyway.locations"));
    }

    @Test
    public void testMigrationScriptsExist() {
        InputStream v1Script = getClass().getClassLoader().getResourceAsStream("db/migration/V1__Initial_schema.sql");
        assertNotNull(v1Script, "V1__Initial_schema.sql should exist on classpath");

        InputStream v2Script = getClass().getClassLoader().getResourceAsStream("db/migration/V2__Seed_initial_data.sql");
        assertNotNull(v2Script, "V2__Seed_initial_data.sql should exist on classpath");

        InputStream v3Script = getClass().getClassLoader().getResourceAsStream("db/migration/V3__Update_admin_password_hash.sql");
        assertNotNull(v3Script, "V3__Update_admin_password_hash.sql should exist on classpath");
    }

    @Test
    public void testV3AdminPasswordHashIsValid() throws Exception {
        try (InputStream v3Script = getClass().getClassLoader().getResourceAsStream("db/migration/V3__Update_admin_password_hash.sql")) {
            assertNotNull(v3Script, "V3 script must exist");
            String content = new String(v3Script.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("'(\\$2[aby]?\\$\\d{2}\\$[A-Za-z0-9./]{53})'").matcher(content);
            assertTrue(matcher.find(), "Valid BCrypt hash pattern must be found in V3 script");
            String hash = matcher.group(1);
            assertTrue(org.mindrot.jbcrypt.BCrypt.checkpw("admin", hash), "Extracted hash must verify against password 'admin'");
        }
    }
}
