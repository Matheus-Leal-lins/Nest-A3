package br.com.nest.repository;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Acesso ao SQLite local. O arquivo padrão é {@code nest.db}; use {@code -Dnest.db=caminho} para trocar
 * (os testes usam isso para bancos temporários). O esquema é criado/migrado automaticamente.
 */
public final class Database {
    private static final Set<String> initialized = new HashSet<>();

    private Database() {}

    public static synchronized Connection connect() throws SQLException {
        String path = System.getProperty("nest.db", "nest.db");
        Connection c = DriverManager.getConnection("jdbc:sqlite:" + path);
        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys=ON");
        }
        if (initialized.add(path)) {
            try {
                createSchema(c);
            } catch (SQLException e) {
                initialized.remove(path);
                c.close();
                throw e;
            }
        }
        return c;
    }

    private static void createSchema(Connection c) throws SQLException {
        try (Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS guardians(id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "username TEXT NOT NULL UNIQUE,salt TEXT NOT NULL,hash TEXT NOT NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS children(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,"
                    + "age INTEGER NOT NULL CHECK(age BETWEEN 0 AND 17))");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS links(id INTEGER PRIMARY KEY AUTOINCREMENT,child_id INTEGER NOT NULL,"
                    + "platform TEXT NOT NULL,account_label TEXT NOT NULL,"
                    + "FOREIGN KEY(child_id) REFERENCES children(id) ON DELETE CASCADE)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS usage_sessions(id INTEGER PRIMARY KEY AUTOINCREMENT,child_id INTEGER NOT NULL,"
                    + "platform TEXT NOT NULL,date TEXT NOT NULL,minutes INTEGER NOT NULL CHECK(minutes>=0),"
                    + "FOREIGN KEY(child_id) REFERENCES children(id) ON DELETE CASCADE)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS alerts(id INTEGER PRIMARY KEY AUTOINCREMENT,child_id INTEGER NOT NULL,"
                    + "platform TEXT NOT NULL,message TEXT NOT NULL,created_at TEXT NOT NULL,is_read INTEGER NOT NULL DEFAULT 0,"
                    + "dedupe_key TEXT UNIQUE,FOREIGN KEY(child_id) REFERENCES children(id) ON DELETE CASCADE)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS limits(child_id INTEGER NOT NULL,platform TEXT NOT NULL,"
                    + "minutes INTEGER NOT NULL CHECK(minutes>0),PRIMARY KEY(child_id,platform),"
                    + "FOREIGN KEY(child_id) REFERENCES children(id) ON DELETE CASCADE)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS setting_requests(id INTEGER PRIMARY KEY AUTOINCREMENT,child_id INTEGER NOT NULL,"
                    + "platform TEXT NOT NULL,setting TEXT NOT NULL,value TEXT NOT NULL,status TEXT NOT NULL,created_at TEXT NOT NULL,"
                    + "FOREIGN KEY(child_id) REFERENCES children(id) ON DELETE CASCADE)");
        }
        // Migração de bancos criados pela versão anterior do protótipo.
        addColumnIfMissing(c, "children", "guardian_id", "INTEGER");
        addColumnIfMissing(c, "links", "status", "TEXT NOT NULL DEFAULT 'SIMULADO'");
        addColumnIfMissing(c, "links", "last_sync", "TEXT");
        addColumnIfMissing(c, "usage_sessions", "source", "TEXT NOT NULL DEFAULT 'MANUAL'");
    }

    private static void addColumnIfMissing(Connection c, String table, String column, String definition) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (r.next()) {
                if (column.equalsIgnoreCase(r.getString("name"))) return;
            }
        }
        try (Statement s = c.createStatement()) {
            s.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }
}
