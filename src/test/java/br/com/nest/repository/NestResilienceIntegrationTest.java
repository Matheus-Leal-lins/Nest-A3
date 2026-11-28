package br.com.nest.repository;

import org.junit.jupiter.api.Test;

import java.sql.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

class NestResilienceIntegrationTest extends BaseIntegrationTest {

    @Test
    void t11_deveBloquearIdadeNegativaEmChildren() throws Exception {
        try (Connection conn = openConnection()) {
            assertThrows(SQLException.class, () -> {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO children(name, age) VALUES(?, ?)")) {
                    ps.setString(1, "BebeNegativo");
                    ps.setInt(2, -1);
                    ps.executeUpdate();
                }
            });
        }
    }

    @Test
    void t12_deveBloquearIdadeMaiorQue17EmChildren() throws Exception {
        try (Connection conn = openConnection()) {
            assertThrows(SQLException.class, () -> {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO children(name, age) VALUES(?, ?)")) {
                    ps.setString(1, "MaiorDeIdade");
                    ps.setInt(2, 18);
                    ps.executeUpdate();
                }
            });
        }
    }

    @Test
    void t13_deveAceitarLimitesExtremosDeIdadeZeroEDezessete() throws Exception {
        try (Connection conn = openConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO children(name, age) VALUES(?, ?), (?, ?)")) {
                ps.setString(1, "ZeroAnos");
                ps.setInt(2, 0);
                ps.setString(3, "DezesseteAnos");
                ps.setInt(4, 17);
                assertEquals(2, ps.executeUpdate());
            }
        }
    }

    @Test
    void t14_deveBloquearMinutosNegativosEmUsageSessions() throws Exception {
        try (Connection conn = openConnection()) {
            long childId = inserirFilho(conn, "Ana", 10);

            assertThrows(SQLException.class, () -> {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO usage_sessions(child_id, platform, date, minutes) VALUES(?, ?, ?, ?)")) {
                    ps.setLong(1, childId);
                    ps.setString(2, "Nintendo Switch");
                    ps.setString(3, "2026-11-27");
                    ps.setInt(4, -15);
                    ps.executeUpdate();
                }
            });
        }
    }

    @Test
    void t15_deveBloquearLimiteZeroOuNegativoEmLimits() throws Exception {
        try (Connection conn = openConnection()) {
            long childId = inserirFilho(conn, "Bia", 13);

            assertThrows(SQLException.class, () -> {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO limits(child_id, platform, minutes) VALUES(?, ?, ?)")) {
                    ps.setLong(1, childId);
                    ps.setString(2, "Todas");
                    ps.setInt(3, 0);
                    ps.executeUpdate();
                }
            });
        }
    }

    @Test
    void t16_deveBloquearUsernameDuplicadoEmGuardians() throws Exception {
        try (Connection conn = openConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO guardians(username, salt, hash) VALUES(?, ?, ?)")) {
                ps.setString(1, "snil_unico");
                ps.setString(2, "s1");
                ps.setString(3, "h1");
                ps.executeUpdate();
            }

            assertThrows(SQLException.class, () -> {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO guardians(username, salt, hash) VALUES(?, ?, ?)")) {
                    ps.setString(1, "snil_unico");
                    ps.setString(2, "s2");
                    ps.setString(3, "h2");
                    ps.executeUpdate();
                }
            });
        }
    }

    @Test
    void t17_deveBloquearChildIdInexistenteEmLinks() throws Exception {
        try (Connection conn = openConnection()) {
            assertThrows(SQLException.class, () -> {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO links(child_id, platform, account_label) VALUES(?, ?, ?)")) {
                    ps.setLong(1, 999999L);
                    ps.setString(2, "Nintendo Switch");
                    ps.setString(3, "Fantasma");
                    ps.executeUpdate();
                }
            });
        }
    }

    @Test
    void t18_deveRemoverEmCascataLinksSessionsLimitsAlertsESettingRequests() throws Exception {
        try (Connection conn = openConnection()) {
            long childId = inserirFilho(conn, "Ana", 10);

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO links(child_id, platform, account_label) VALUES(?, ?, ?)")) {
                ps.setLong(1, childId);
                ps.setString(2, "Nintendo Switch");
                ps.setString(3, "Conta Ana");
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO usage_sessions(child_id, platform, date, minutes) VALUES(?, ?, ?, ?)")) {
                ps.setLong(1, childId);
                ps.setString(2, "Nintendo Switch");
                ps.setString(3, "2026-11-27");
                ps.setInt(4, 25);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO limits(child_id, platform, minutes) VALUES(?, ?, ?)")) {
                ps.setLong(1, childId);
                ps.setString(2, "Nintendo Switch");
                ps.setInt(3, 60);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO alerts(child_id, platform, message, created_at, dedupe_key) VALUES(?, ?, ?, ?, ?)")) {
                ps.setLong(1, childId);
                ps.setString(2, "Nintendo Switch");
                ps.setString(3, "Alerta teste");
                ps.setString(4, "2026-11-27 22:20:00");
                ps.setString(5, "cascade|" + childId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO setting_requests(child_id, platform, setting, value, status, created_at) VALUES(?, ?, ?, ?, ?, ?)")) {
                ps.setLong(1, childId);
                ps.setString(2, "Nintendo Switch");
                ps.setString(3, "Limite diário (minutos)");
                ps.setString(4, "60");
                ps.setString(5, "PENDENTE");
                ps.setString(6, "2026-11-27 22:20:00");
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM children WHERE id = ?")) {
                ps.setLong(1, childId);
                assertEquals(1, ps.executeUpdate());
            }

            String[] childTables = {"links", "usage_sessions", "limits", "alerts", "setting_requests"};
            for (String tbl : childTables) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT COUNT(*) FROM " + tbl + " WHERE child_id = ?")) {
                    ps.setLong(1, childId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertTrue(rs.next());
                        assertEquals(0, rs.getInt(1), "Sobrou registro órfão em " + tbl);
                    }
                }
            }
        }
    }

    @Test
    void t19_deveSomarSessoesEmLoteSobConcorrenciaLeve() throws Exception {
        long childId;
        try (Connection conn = openConnection()) {
            childId = inserirFilho(conn, "Bia", 13);
        }

        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            int totalInserts = 40;
            Callable<Void> task = () -> {
                try (Connection conn = openConnection();
                     PreparedStatement ps = conn.prepareStatement(
                             "INSERT INTO usage_sessions(child_id, platform, date, minutes, source) VALUES(?, ?, ?, ?, ?)")) {
                    ps.setLong(1, childId);
                    ps.setString(2, "Nintendo Switch");
                    ps.setString(3, "2026-11-27");
                    ps.setInt(4, 5);
                    ps.setString(5, "SIMULADO");
                    ps.executeUpdate();
                }
                return null;
            };

            var futures = new Future<?>[totalInserts];
            for (int i = 0; i < totalInserts; i++) {
                futures[i] = pool.submit(task);
            }
            for (Future<?> f : futures) {
                f.get();
            }
        } finally {
            pool.shutdown();
        }

        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COALESCE(SUM(minutes), 0) FROM usage_sessions WHERE child_id = ? AND platform = ? AND date = ?")) {
            ps.setLong(1, childId);
            ps.setString(2, "Nintendo Switch");
            ps.setString(3, "2026-11-27");
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(200, rs.getInt(1));
            }
        }
    }

    @Test
    void t20_deveSuportarCicloRepetidoDeAbrirEFecharConexoes() throws Exception {
        for (int i = 0; i < 25; i++) {
            try (Connection conn = openConnection();
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT 1")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1));
            }
        }
    }

    private long inserirFilho(Connection conn, String name, int age) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO children(name, age) VALUES(?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setInt(2, age);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                assertTrue(keys.next());
                return keys.getLong(1);
            }
        }
    }
}