package br.com.nest.service;
import br.com.nest.model.ChildProfile;
import br.com.nest.model.UsageSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

public class UsageReportIT {
    private UsageReportService service;
    private ChildProfile ana;
    @BeforeEach
    void setUp() {
        service = new UsageReportService();
        ana = new ChildProfile(1, "Ana", 12);
    }

    @Test
    void deveSomarTempoDeDuasSessoesDaMesmaCrianca() {
        service.addSession(new UsageSession(1, ana.getId(), "Nintendo", LocalDate.now(), 60));
        service.addSession(new UsageSession(2, ana.getId(), "Nintendo", LocalDate.now(), 45));

        int total = service.totalMinutes(ana.getId());

        assertEquals(105, total);
    }

    @Test
    void naoDeveSomarSessoesDeOutraCrianca() {
        var bia = new ChildProfile(2, "Bia", 10);
        service.addSession(new UsageSession(1, ana.getId(), "Nintendo", LocalDate.now(), 60));
        service.addSession(new UsageSession(2, bia.getId(), "Nintendo", LocalDate.now(), 30));

        assertEquals(60, service.totalMinutes(ana.getId()));
    }

    @Test
    void sessaoInvalidaNaoDeveEntrarNoRelatorio() {
        assertThrows(IllegalArgumentException.class,
            () -> new UsageSession(1, ana.getId(), "Nintendo", LocalDate.now(), -10));

        assertEquals(0, service.totalMinutes(ana.getId()));
    }
}
