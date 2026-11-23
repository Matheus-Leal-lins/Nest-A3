package br.com.nest.service;
import br.com.nest.model.UsageSession;
import java.util.ArrayList;
import java.util.List;
public class UsageReportService {
    private final List<UsageSession> sessions = new ArrayList<>();

    public void addSession(UsageSession session) {
        sessions.add(session);
    }

    public int totalMinutes(int childId) {
        return sessions.stream()
                .filter(s -> s.getChildId() == childId)
                .mapToInt(UsageSession::getMinutes)
                .sum();
    }
}
