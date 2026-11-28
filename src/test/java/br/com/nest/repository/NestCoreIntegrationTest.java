package br.com.nest.repository;

import org.junit.jupiter.api.Test;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

class NestCoreIntegrationTest extends BaseIntegrationTest {

    @Test
    void t01_deveCriarTodasAsTabelasDoEsquema() throws Exception {
        String[] expectedTables = {
                "guardians", "children", "links",
                "usage_sessions", "alerts", "limits", "setting_requests"
        };

        try (Connection conn = openConnection()) {
            for (String table : expectedTables) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = ?")) {
                    ps.setString(1, table);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertTrue(rs.next());
                        assertEquals(1, rs.getInt(1), "Tabela ausente: " + table);
                    }
                }
            }
        }
    }

    @Test
    void t02_deveManterForeignKeysHabilitadas() throws Exception {
        try (Connection conn = openConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA foreign_keys")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    void t03_deveCadastrarResponsavelComSaltEHash() throws Exception {
        try (Connection conn = openConnection()) {
            long id = inserirResponsavel(conn, "snil");
            assertTrue(id > 0);

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT username, salt, hash FROM guardians WHERE id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals("snil", rs.getString("username"));
                    assertEquals("salt_teste", rs.getString("salt"));
                    assertEquals("hash_teste", rs.getString("hash"));
                }
            }
        }
    }

    @Test
    void t04_deveCadastrarFilhoVinculadoAoResponsavel() throws Exception {
        try (Connection conn = openConnection()) {
            long guardianId = inserirResponsavel(conn, "snil_resp");
            long childId = inserirFilho(conn, "Ana", 10, guardianId);
            assertTrue(childId > 0);

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT name, age, guardian_id FROM children WHERE id = ?")) {
                ps.setLong(1, childId);
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals("Ana", rs.getString("name"));
                    assertEquals(10, rs.getInt("age"));
                    assertEquals(guardianId, rs.getLong("guardian_id"));
                }
            }
        }
    }

    @Test
    void t05_deveVincularMultiplasPlataformasAoMesmoFilho() throws Exception {
        try (Connection conn = openConnection()) {
            long childId = inserirFilho(conn, "Bia", 13, null);

            inserirVinculo(conn, childId, "Google Family Link", "bia.demo@exemplo", "SIMULADO", "2026-11-27 22:00:00");
            inserirVinculo(conn, childId, "Nintendo Switch", "Perfil Bia (Switch)", "SIMULADO", "2026-11-27 22:01:00");

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM links WHERE child_id = ?")) {
                ps.setLong(1, childId);
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(2, rs.getInt(1));
                }
            }
        }
    }

    @Test
    void t06_deveAplicarValoresPadraoEmLinksEUsageSessions() throws Exception {
        try (Connection conn = openConnection()) {
            long childId = inserirFilho(conn, "Ana", 10, null);

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO links(child_id, platform, account_label) VALUES(?, ?, ?)")) {
                ps.setLong(1, childId);
                ps.setString(2, "Apple Screen Time");
                ps.setString(3, "iPad da Ana");
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO usage_sessions(child_id, platform, date, minutes) VALUES(?, ?, ?, ?)")) {
                ps.setLong(1, childId);
                ps.setString(2, "Apple Screen Time");
                ps.setString(3, "2026-11-27");
                ps.setInt(4, 30);
                ps.executeUpdate();
            }

            try (Statement st = conn.createStatement();
                 ResultSet rsLink = st.executeQuery("SELECT status, last_sync FROM links LIMIT 1");
                 ResultSet rsSess = st.executeQuery("SELECT source FROM usage_sessions LIMIT 1")) {
                assertTrue(rsLink.next());
                assertEquals("SIMULADO", rsLink.getString("status"));
                assertNull(rsLink.getString("last_sync"));

                assertTrue(rsSess.next());
                assertEquals("MANUAL", rsSess.getString("source"));
            }
        }
    }

    @Test
    void t07_deveRegistrarLimiteDiarioPorPlataforma() throws Exception {
        try (Connection conn = openConnection()) {
            long childId = inserirFilho(conn, "Ana", 10, null);

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO limits(child_id, platform, minutes) VALUES(?, ?, ?)")) {
                ps.setLong(1, childId);
                ps.setString(2, "Nintendo Switch");
                ps.setInt(3, 60);
                assertEquals(1, ps.executeUpdate());
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT minutes FROM limits WHERE child_id = ? AND platform = ?")) {
                ps.setLong(1, childId);
                ps.setString(2, "Nintendo Switch");
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(60, rs.getInt("minutes"));
                }
            }
        }
    }

    @Test
    void t08_deveRegistrarSessoesDeUsoEBuscarPorData() throws Exception {
        try (Connection conn = openConnection()) {
            long childId = inserirFilho(conn, "Bia", 13, null);

            inserirSessao(conn, childId, "Nintendo Switch", "2026-11-26", 40, "MANUAL");
            inserirSessao(conn, childId, "Nintendo Switch", "2026-11-27", 55, "SIMULADO");

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT SUM(minutes) FROM usage_sessions WHERE child_id = ? AND date = ?")) {
                ps.setLong(1, childId);
                ps.setString(2, "2026-11-27");
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(55, rs.getInt(1));
                }
            }
        }
    }

    @Test
    void t09_deveCriarAlertaComIsReadPadraoZero() throws Exception {
        try (Connection conn = openConnection()) {
            long childId = inserirFilho(conn, "Ana", 10, null);

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO alerts(child_id, platform, message, created_at, dedupe_key) VALUES(?, ?, ?, ?, ?)")) {
                ps.setLong(1, childId);
                ps.setString(2, "Nintendo Switch");
                ps.setString(3, "Limite excedido em 27/11/2026.");
                ps.setString(4, "2026-11-27 22:10:00");
                ps.setString(5, childId + "|Nintendo Switch|2026-11-27|LIMITE");
                assertEquals(1, ps.executeUpdate());
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT is_read FROM alerts WHERE child_id = ?")) {
                ps.setLong(1, childId);
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(0, rs.getInt("is_read"));
                }
            }
        }
    }

    @Test
    void t10_deveCriarSolicitacaoEmSettingRequests() throws Exception {
        try (Connection conn = openConnection()) {
            long childId = inserirFilho(conn, "Bia", 13, null);

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO setting_requests(child_id, platform, setting, value, status, created_at) "
                            + "VALUES(?, ?, ?, ?, ?, ?)")) {
                ps.setLong(1, childId);
                ps.setString(2, "Todas");
                ps.setString(3, "Limite diário (minutos)");
                ps.setString(4, "120");
                ps.setString(5, "PENDENTE");
                ps.setString(6, "2026-11-27 22:15:00");
                assertEquals(1, ps.executeUpdate());
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM setting_requests WHERE child_id = ? AND status = 'PENDENTE'")) {
                ps.setLong(1, childId);
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(1, rs.getInt(1));
                }
            }
        }
    }

    private long inserirResponsavel(Connection conn, String username) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO guardians(username, salt, hash) VALUES(?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, "salt_teste");
            ps.setString(3, "hash_teste");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                assertTrue(keys.next());
                return keys.getLong(1);
            }
        }
    }

    private long inserirFilho(Connection conn, String name, int age, Long guardianId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO children(name, age, guardian_id) VALUES(?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setInt(2, age);
            if (guardianId == null) {
                ps.setNull(3, Types.INTEGER);
            } else {
                ps.setLong(3, guardianId);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                assertTrue(keys.next());
                return keys.getLong(1);
            }
        }
    }

    private void inserirVinculo(Connection conn, long childId, String platform, String label, String status, String lastSync) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO links(child_id, platform, account_label, status, last_sync) VALUES(?, ?, ?, ?, ?)")) {
            ps.setLong(1, childId);
            ps.setString(2, platform);
            ps.setString(3, label);
            ps.setString(4, status);
            ps.setString(5, lastSync);
            ps.executeUpdate();
        }
    }

    private void inserirSessao(Connection conn, long childId, String platform, String date, int minutes, String source) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO usage_sessions(child_id, platform, date, minutes, source) VALUES(?, ?, ?, ?, ?)")) {
            ps.setLong(1, childId);
            ps.setString(2, platform);
            ps.setString(3, date);
            ps.setInt(4, minutes);
            ps.setString(5, source);
            ps.executeUpdate();
        }
    }
}