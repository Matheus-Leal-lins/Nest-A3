package br.com.nest.repository;

import org.junit.jupiter.api.Test;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

class NestDomainIntegrationTest extends BaseIntegrationTest {

    @Test
    void t21_deveCriarEAtualizarSolicitacaoDeConfiguracao() throws Exception {
        try (Connection conn = openConnection()) {
            long childId = inserirFilho(conn, "Ana", 10, null);
            long requestId;

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO setting_requests(child_id, platform, setting, value, status, created_at) "
                            + "VALUES(?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, childId);
                ps.setString(2, "Nintendo Switch");
                ps.setString(3, "Limite diário (minutos)");
                ps.setString(4, "60");
                ps.setString(5, "PENDENTE");
                ps.setString(6, "2026-11-27 22:45:00");
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    assertTrue(keys.next());
                    requestId = keys.getLong(1);
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE setting_requests SET status = ? WHERE id = ?")) {
                ps.setString(1, "APLICADO");
                ps.setLong(2, requestId);
                assertEquals(1, ps.executeUpdate());
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT status, value FROM setting_requests WHERE id = ?")) {
                ps.setLong(1, requestId);
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals("APLICADO", rs.getString("status"));
                    assertEquals("60", rs.getString("value"));
                }
            }
        }
    }

    @Test
    void t22_deveAtualizarLimiteSemDuplicarChaveComposta() throws Exception {
        try (Connection conn = openConnection()) {
            long childId = inserirFilho(conn, "Bia", 13, null);

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO limits(child_id, platform, minutes) VALUES(?, ?, ?)")) {
                ps.setLong(1, childId);
                ps.setString(2, "Todas");
                ps.setInt(3, 180);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO limits(child_id, platform, minutes) VALUES(?, ?, ?) "
                            + "ON CONFLICT(child_id, platform) DO UPDATE SET minutes = excluded.minutes")) {
                ps.setLong(1, childId);
                ps.setString(2, "Todas");
                ps.setInt(3, 120);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) AS total, MAX(minutes) AS minutos FROM limits WHERE child_id = ? AND platform = ?")) {
                ps.setLong(1, childId);
                ps.setString(2, "Todas");
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(1, rs.getInt("total"));
                    assertEquals(120, rs.getInt("minutos"));
                }
            }
        }
    }

    @Test
    void t23_deveIsolarFilhosPorResponsavel() throws Exception {
        try (Connection conn = openConnection()) {
            long guardian1 = inserirResponsavel(conn, "snil_1");
            long guardian2 = inserirResponsavel(conn, "snil_2");

            inserirFilho(conn, "Ana", 10, guardian1);
            inserirFilho(conn, "Bia", 13, guardian1);
            inserirFilho(conn, "Lucas", 8, guardian2);

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM children WHERE guardian_id = ?")) {
                ps.setLong(1, guardian1);
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(2, rs.getInt(1));
                }
            }
        }
    }

    @Test
    void t24_deveSetarGuardianIdParaNullAoExcluirResponsavel() throws Exception {
        try (Connection conn = openConnection()) {
            long guardianId = inserirResponsavel(conn, "snil_temp");
            inserirFilho(conn, "Lucas", 8, guardianId);

            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM guardians WHERE id = ?")) {
                ps.setLong(1, guardianId);
                assertEquals(1, ps.executeUpdate());
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT guardian_id FROM children WHERE name = ?")) {
                ps.setString(1, "Lucas");
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertNull(rs.getObject("guardian_id"));
                }
            }
        }
    }

    @Test
    void t25_deveMarcarAlertaComoLido() throws Exception {
        try (Connection conn = openConnection()) {
            long childId = inserirFilho(conn, "Ana", 10, null);
            String dedupeKey = childId + "|Apple Screen Time|2026-11-27|LIMITE";

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO alerts(child_id, platform, message, created_at, is_read, dedupe_key) "
                            + "VALUES(?, ?, ?, ?, 0, ?)")) {
                ps.setLong(1, childId);
                ps.setString(2, "Apple Screen Time");
                ps.setString(3, "Uso acima do limite no iPad.");
                ps.setString(4, "2026-11-27 22:45:00");
                ps.setString(5, dedupeKey);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE alerts SET is_read = 1 WHERE dedupe_key = ?")) {
                ps.setString(1, dedupeKey);
                assertEquals(1, ps.executeUpdate());
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM alerts WHERE child_id = ? AND is_read = 0")) {
                ps.setLong(1, childId);
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(0, rs.getInt(1));
                }
            }
        }
    }

    @Test
    void t26_deveIgnorarAlertaDuplicadoComMesmoDedupeKey() throws Exception {
        try (Connection conn = openConnection()) {
            long childId = inserirFilho(conn, "Ana", 10, null);
            String dedupeKey = childId + "|Nintendo Switch|2026-11-27|LIMITE";

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT OR IGNORE INTO alerts(child_id, platform, message, created_at, is_read, dedupe_key) "
                            + "VALUES(?, ?, ?, ?, 0, ?)")) {
                ps.setLong(1, childId);
                ps.setString(2, "Nintendo Switch");
                ps.setString(3, "Alerta 1");
                ps.setString(4, "2026-11-27 22:45:00");
                ps.setString(5, dedupeKey);
                assertEquals(1, ps.executeUpdate());
                assertEquals(0, ps.executeUpdate());
            }
        }
    }

    @Test
    void t27_deveAtualizarStatusESincronizacaoDePlataformaVinculada() throws Exception {
        try (Connection conn = openConnection()) {
            long childId = inserirFilho(conn, "Bia", 13, null);
            long linkId;

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO links(child_id, platform, account_label) VALUES(?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, childId);
                ps.setString(2, "Google Family Link");
                ps.setString(3, "bia.demo@exemplo");
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    assertTrue(keys.next());
                    linkId = keys.getLong(1);
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE links SET status = ?, last_sync = ? WHERE id = ?")) {
                ps.setString(1, "SINCRONIZADO");
                ps.setString(2, "2026-11-27 22:47:00");
                ps.setLong(3, linkId);
                assertEquals(1, ps.executeUpdate());
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT status, last_sync FROM links WHERE id = ?")) {
                ps.setLong(1, linkId);
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals("SINCRONIZADO", rs.getString("status"));
                    assertEquals("2026-11-27 22:47:00", rs.getString("last_sync"));
                }
            }
        }
    }

    @Test
    void t28_deveCalcularUsoDiarioPorPlataformaESource() throws Exception {
        try (Connection conn = openConnection()) {
            long childId = inserirFilho(conn, "Ana", 10, null);

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO usage_sessions(child_id, platform, date, minutes, source) VALUES(?, ?, ?, ?, ?)")) {
                ps.setLong(1, childId);
                ps.setString(2, "Nintendo Switch");
                ps.setString(3, "2026-11-27");
                ps.setInt(4, 35);
                ps.setString(5, "MANUAL");
                ps.executeUpdate();

                ps.setInt(4, 25);
                ps.setString(5, "SIMULADO");
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT SUM(minutes) FROM usage_sessions WHERE child_id = ? AND platform = ? AND date = ?")) {
                ps.setLong(1, childId);
                ps.setString(2, "Nintendo Switch");
                ps.setString(3, "2026-11-27");
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(60, rs.getInt(1));
                }
            }
        }
    }

    @Test
    void t29_deveDetectarQuandoUsoUltrapassaLimiteConfigurado() throws Exception {
        try (Connection conn = openConnection()) {
            long childId = inserirFilho(conn, "Bia", 13, null);

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO limits(child_id, platform, minutes) VALUES(?, ?, ?)")) {
                ps.setLong(1, childId);
                ps.setString(2, "Nintendo Switch");
                ps.setInt(3, 60);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO usage_sessions(child_id, platform, date, minutes) VALUES(?, ?, ?, ?)")) {
                ps.setLong(1, childId);
                ps.setString(2, "Nintendo Switch");
                ps.setString(3, "2026-11-27");
                ps.setInt(4, 85);
                ps.executeUpdate();
            }

            String sql = "SELECT COALESCE(SUM(u.minutes), 0) > l.minutes AS excedeu "
                    + "FROM limits l LEFT JOIN usage_sessions u "
                    + "ON u.child_id = l.child_id AND u.platform = l.platform AND u.date = ? "
                    + "WHERE l.child_id = ? AND l.platform = ? GROUP BY l.minutes";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, "2026-11-27");
                ps.setLong(2, childId);
                ps.setString(3, "Nintendo Switch");
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(1, rs.getInt("excedeu"));
                }
            }
        }
    }

    @Test
    void t30_devePreservarDadosEntreMultiplasReconexoesNoMesmoBanco() throws Exception {
        try (Connection conn1 = openConnection()) {
            inserirResponsavel(conn1, "snil_persistente");
        }

        try (Connection conn2 = openConnection();
             PreparedStatement ps = conn2.prepareStatement(
                     "SELECT COUNT(*) FROM guardians WHERE username = ?")) {
            ps.setString(1, "snil_persistente");
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1));
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
}