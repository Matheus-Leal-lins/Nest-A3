package br.com.nest.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.com.nest.model.Alert;
import br.com.nest.model.ChildProfile;
import br.com.nest.model.PlatformLink;
import br.com.nest.model.SettingRequest;
import br.com.nest.model.UsageSession;
import br.com.nest.repository.Database;


public class NestService {
    public static final String ALL = "Todas";
    public static final String SETTING_DAILY_LIMIT = "Limite diário (minutos)";
    public static final String SETTING_QUIET_HOURS = "Horário de descanso";
    public static final String STATUS_SIMULATED = "SIMULADO";

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final int guardianId;

    public NestService(int guardianId) {
        this.guardianId = guardianId;
    }

    public static String now() {
        return LocalDateTime.now().format(TS);
    }



    public ChildProfile addChild(String name, int age) {
        ChildProfile v = new ChildProfile(0, name, age);
        try (Connection c = Database.connect();
             PreparedStatement p = c.prepareStatement("INSERT INTO children(name,age,guardian_id) VALUES(?,?,?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1, v.name());
            p.setInt(2, v.age());
            p.setInt(3, guardianId);
            p.executeUpdate();
            try (ResultSet r = p.getGeneratedKeys()) {
                return new ChildProfile(r.next() ? r.getInt(1) : 0, v.name(), v.age());
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Não foi possível salvar perfil.", e);
        }
    }

    public List<ChildProfile> listChildren() {
        List<ChildProfile> out = new ArrayList<>();
        try (Connection c = Database.connect();
             PreparedStatement p = c.prepareStatement("SELECT id,name,age FROM children WHERE guardian_id=? ORDER BY name")) {
            p.setInt(1, guardianId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) out.add(new ChildProfile(r.getInt("id"), r.getString("name"), r.getInt("age")));
            }
            return out;
        } catch (SQLException e) {
            throw new IllegalStateException("Não foi possível listar perfis.", e);
        }
    }

    public void removeChild(int childId) {
        requireChild(childId);
        execute("DELETE FROM children WHERE id=?", "Falha ao remover perfil.", childId);
    }


    public PlatformLink linkPlatform(int childId, String platform, String accountLabel) {
        requireChild(childId);
        if (platform == null || platform.isBlank() || accountLabel == null || accountLabel.isBlank())
            throw new IllegalArgumentException("Plataforma e rótulo da conta são obrigatórios.");
        String plat = platform.trim();
        for (PlatformLink l : listLinks(childId))
            if (l.platform().equalsIgnoreCase(plat))
                throw new IllegalArgumentException("Este perfil já possui vínculo com " + plat + ".");
        try (Connection c = Database.connect();
             PreparedStatement p = c.prepareStatement(
                     "INSERT INTO links(child_id,platform,account_label,status) VALUES(?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            p.setInt(1, childId);
            p.setString(2, plat);
            p.setString(3, accountLabel.trim());
            p.setString(4, STATUS_SIMULATED);
            p.executeUpdate();
            try (ResultSet r = p.getGeneratedKeys()) {
                return new PlatformLink(r.next() ? r.getInt(1) : 0, childId, plat, accountLabel.trim(), STATUS_SIMULATED, null);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao registrar vínculo.", e);
        }
    }

    public List<PlatformLink> listLinks(int childId) {
        requireChild(childId);
        List<PlatformLink> out = new ArrayList<>();
        try (Connection c = Database.connect();
             PreparedStatement p = c.prepareStatement(
                     "SELECT id,child_id,platform,account_label,status,last_sync FROM links WHERE child_id=? ORDER BY platform")) {
            p.setInt(1, childId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next())
                    out.add(new PlatformLink(r.getInt("id"), r.getInt("child_id"), r.getString("platform"),
                            r.getString("account_label"), r.getString("status"), r.getString("last_sync")));
            }
            return out;
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao listar vínculos.", e);
        }
    }

    public void unlink(int linkId) {
        execute("DELETE FROM links WHERE id=? AND child_id IN (SELECT id FROM children WHERE guardian_id=" + guardianId + ")",
                "Falha ao remover vínculo.", linkId);
    }

    public void touchSync(int linkId) {
        execute("UPDATE links SET last_sync=? WHERE id=? AND child_id IN (SELECT id FROM children WHERE guardian_id=" + guardianId + ")",
                "Falha ao atualizar sincronização.", now(), linkId);
    }



    public void addUsage(int childId, String platform, LocalDate date, int minutes) {
        addUsage(childId, platform, date, minutes, "MANUAL");
    }

    public void addUsage(int childId, String platform, LocalDate date, int minutes, String source) {
        requireChild(childId);
        UsageSession v = new UsageSession(0, childId, platform, date, minutes, source);
        try (Connection c = Database.connect();
             PreparedStatement p = c.prepareStatement(
                     "INSERT INTO usage_sessions(child_id,platform,date,minutes,source) VALUES(?,?,?,?,?)")) {
            p.setInt(1, childId);
            p.setString(2, v.platform());
            p.setString(3, date.toString());
            p.setInt(4, minutes);
            p.setString(5, v.source());
            p.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao registrar uso.", e);
        }
        checkLimits(childId, v.platform(), date);
    }


    public void replaceSimulatedUsage(int childId, String platform, LocalDate date, int minutes) {
        requireChild(childId);
        execute("DELETE FROM usage_sessions WHERE child_id=? AND platform=? AND date=? AND source='SIMULADO'",
                "Falha ao atualizar uso simulado.", childId, platform, date.toString());
        if (minutes > 0) addUsage(childId, platform, date, minutes, "SIMULADO");
    }

    public int totalMinutes(int childId, LocalDate date) {
        return totalMinutes(childId, ALL, date);
    }

    public int totalMinutes(int childId, String platform, LocalDate date) {
        boolean all = platform == null || ALL.equals(platform);
        String sql = "SELECT COALESCE(SUM(minutes),0) FROM usage_sessions WHERE child_id=? AND date=?" + (all ? "" : " AND platform=?");
        try (Connection c = Database.connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, childId);
            p.setString(2, date.toString());
            if (!all) p.setString(3, platform);
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? r.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao gerar relatório.", e);
        }
    }


    public Map<LocalDate, Integer> minutesByDay(int childId, LocalDate from, LocalDate to) {
        Map<LocalDate, Integer> out = new LinkedHashMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) out.put(d, 0);
        try (Connection c = Database.connect();
             PreparedStatement p = c.prepareStatement(
                     "SELECT date,SUM(minutes) FROM usage_sessions WHERE child_id=? AND date BETWEEN ? AND ? GROUP BY date")) {
            p.setInt(1, childId);
            p.setString(2, from.toString());
            p.setString(3, to.toString());
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) out.put(LocalDate.parse(r.getString(1)), r.getInt(2));
            }
            return out;
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao gerar relatório por dia.", e);
        }
    }


    public Map<String, Integer> minutesByPlatform(int childId, LocalDate from, LocalDate to) {
        Map<String, Integer> out = new LinkedHashMap<>();
        try (Connection c = Database.connect();
             PreparedStatement p = c.prepareStatement(
                     "SELECT platform,SUM(minutes) AS m FROM usage_sessions WHERE child_id=? AND date BETWEEN ? AND ? "
                             + "GROUP BY platform ORDER BY m DESC")) {
            p.setInt(1, childId);
            p.setString(2, from.toString());
            p.setString(3, to.toString());
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) out.put(r.getString(1), r.getInt(2));
            }
            return out;
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao gerar relatório por plataforma.", e);
        }
    }

    public List<UsageSession> listSessions(int childId, String platform, LocalDate from, LocalDate to) {
        boolean all = platform == null || ALL.equals(platform);
        String sql = "SELECT id,child_id,platform,date,minutes,source FROM usage_sessions WHERE child_id=? AND date BETWEEN ? AND ?"
                + (all ? "" : " AND platform=?") + " ORDER BY date DESC,platform,id DESC";
        List<UsageSession> out = new ArrayList<>();
        try (Connection c = Database.connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, childId);
            p.setString(2, from.toString());
            p.setString(3, to.toString());
            if (!all) p.setString(4, platform);
            try (ResultSet r = p.executeQuery()) {
                while (r.next())
                    out.add(new UsageSession(r.getInt("id"), r.getInt("child_id"), r.getString("platform"),
                            LocalDate.parse(r.getString("date")), r.getInt("minutes"), r.getString("source")));
            }
            return out;
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao listar uso.", e);
        }
    }




    public List<Alert> listAlerts(Integer childId) {
        String sql = "SELECT a.id,a.child_id,c.name,a.platform,a.message,a.created_at,a.is_read FROM alerts a "
                + "JOIN children c ON c.id=a.child_id WHERE c.guardian_id=?" + (childId != null ? " AND a.child_id=?" : "")
                + " ORDER BY a.created_at DESC,a.id DESC";
        List<Alert> out = new ArrayList<>();
        try (Connection c = Database.connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, guardianId);
            if (childId != null) p.setInt(2, childId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next())
                    out.add(new Alert(r.getInt(1), r.getInt(2), r.getString(3), r.getString(4), r.getString(5),
                            r.getString(6), r.getInt(7) == 1));
            }
            return out;
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao listar alertas.", e);
        }
    }

    public int unreadAlerts(Integer childId) {
        int n = 0;
        for (Alert a : listAlerts(childId)) if (!a.read()) n++;
        return n;
    }

    public void markAllRead(Integer childId) {
        String sql = "UPDATE alerts SET is_read=1 WHERE child_id IN (SELECT id FROM children WHERE guardian_id=?)"
                + (childId != null ? " AND child_id=?" : "");
        try (Connection c = Database.connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, guardianId);
            if (childId != null) p.setInt(2, childId);
            p.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao atualizar alertas.", e);
        }
    }


    private void checkLimits(int childId, String platform, LocalDate date) {
        for (String scope : new String[]{platform, ALL}) {
            Integer limit = dailyLimit(childId, scope);
            if (limit == null) continue;
            int used = totalMinutes(childId, scope, date);
            if (used <= limit) continue;
            String who = childName(childId);
            String where = ALL.equals(scope) ? "no total de todas as plataformas" : "em " + scope;
            String msg = who + " excedeu o limite diário de " + limit + " min " + where + " em "
                    + date.format(BR_DATE) + " (uso: " + used + " min).";
            execute("INSERT OR IGNORE INTO alerts(child_id,platform,message,created_at,dedupe_key) VALUES(?,?,?,?,?)",
                    "Falha ao registrar alerta.", childId, scope, msg, now(), childId + "|" + scope + "|" + date + "|LIMITE");
        }
    }


    public Integer dailyLimit(int childId, String platform) {
        try (Connection c = Database.connect();
             PreparedStatement p = c.prepareStatement("SELECT minutes FROM limits WHERE child_id=? AND platform=?")) {
            p.setInt(1, childId);
            p.setString(2, platform);
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? r.getInt(1) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao consultar limite.", e);
        }
    }


    public SettingRequest requestSetting(int childId, String platform, String setting, String value) {
        requireChild(childId);
        if (platform == null || platform.isBlank()) throw new IllegalArgumentException("Escolha a plataforma.");
        if (setting == null || setting.isBlank()) throw new IllegalArgumentException("Escolha a configuração.");
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Informe o valor.");
        String val = value.trim();
        if (SETTING_DAILY_LIMIT.equals(setting)) {
            int minutes;
            try {
                minutes = Integer.parseInt(val);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("O limite deve ser um número inteiro de minutos.");
            }
            if (minutes <= 0 || minutes > 1440) throw new IllegalArgumentException("O limite deve estar entre 1 e 1440 minutos.");
            execute("INSERT INTO limits(child_id,platform,minutes) VALUES(?,?,?) "
                            + "ON CONFLICT(child_id,platform) DO UPDATE SET minutes=excluded.minutes",
                    "Falha ao salvar limite.", childId, platform, minutes);
            val = String.valueOf(minutes);
        }
        String ts = now();
        try (Connection c = Database.connect();
             PreparedStatement p = c.prepareStatement(
                     "INSERT INTO setting_requests(child_id,platform,setting,value,status,created_at) VALUES(?,?,?,?,?,?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            p.setInt(1, childId);
            p.setString(2, platform);
            p.setString(3, setting);
            p.setString(4, val);
            p.setString(5, STATUS_SIMULATED);
            p.setString(6, ts);
            p.executeUpdate();
            try (ResultSet r = p.getGeneratedKeys()) {
                return new SettingRequest(r.next() ? r.getInt(1) : 0, childId, platform, setting, val, STATUS_SIMULATED, ts);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao registrar solicitação.", e);
        }
    }

    public List<SettingRequest> listRequests(int childId) {
        requireChild(childId);
        List<SettingRequest> out = new ArrayList<>();
        try (Connection c = Database.connect();
             PreparedStatement p = c.prepareStatement("SELECT id,child_id,platform,setting,value,status,created_at "
                     + "FROM setting_requests WHERE child_id=? ORDER BY id DESC")) {
            p.setInt(1, childId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next())
                    out.add(new SettingRequest(r.getInt(1), r.getInt(2), r.getString(3), r.getString(4), r.getString(5),
                            r.getString(6), r.getString(7)));
            }
            return out;
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao listar solicitações.", e);
        }
    }



    private void requireChild(int childId) {
        try (Connection c = Database.connect();
             PreparedStatement p = c.prepareStatement("SELECT 1 FROM children WHERE id=? AND guardian_id=?")) {
            p.setInt(1, childId);
            p.setInt(2, guardianId);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) throw new IllegalArgumentException("Perfil não encontrado.");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao consultar perfil.", e);
        }
    }

    private String childName(int childId) {
        try (Connection c = Database.connect(); PreparedStatement p = c.prepareStatement("SELECT name FROM children WHERE id=?")) {
            p.setInt(1, childId);
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? r.getString(1) : "Perfil";
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao consultar perfil.", e);
        }
    }

    private void execute(String sql, String errorMessage, Object... params) {
        try (Connection c = Database.connect(); PreparedStatement p = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) p.setObject(i + 1, params[i]);
            p.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(errorMessage, e);
        }
    }
}
