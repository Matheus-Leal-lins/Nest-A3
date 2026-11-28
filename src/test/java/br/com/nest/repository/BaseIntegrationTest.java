package br.com.nest.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

abstract class BaseIntegrationTest {

    @TempDir
    Path tempDir;

    private String previousDbProperty;

    @BeforeEach
    void setUpDatabase() {
        previousDbProperty = System.getProperty("nest.db");
        Path dbFile = tempDir.resolve("nest-test.db");
        System.setProperty("nest.db", dbFile.toAbsolutePath().toString());
    }

    @AfterEach
    void tearDownDatabase() {
        if (previousDbProperty == null) {
            System.clearProperty("nest.db");
        } else {
            System.setProperty("nest.db", previousDbProperty);
        }
    }

    protected Connection openConnection() throws SQLException {
        return Database.connect();
    }
}