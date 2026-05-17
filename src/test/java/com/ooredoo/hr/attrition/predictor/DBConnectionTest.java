package com.ooredoo.hr.attrition.predictor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DBConnectionTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void testDatabaseConnection() {
        assertDoesNotThrow(() -> {
            try (Connection connection = dataSource.getConnection()) {
                assertNotNull(connection, "La connexion ne doit pas être null");
                assertFalse(connection.isClosed(), "La connexion ne doit pas être fermée");
                System.out.println("Connexion réussie : " + connection.getMetaData().getURL());
            }
        }, "Échec de connexion à la base de données PostgreSQL");
    }
}