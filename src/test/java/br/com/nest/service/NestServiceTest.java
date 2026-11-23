package br.com.nest.service;

import br.com.nest.model.Alert;
import br.com.nest.model.ChildProfile;
import br.com.nest.model.Platforms;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Testes de integração com SQLite real (banco temporário por teste). */
class NestServiceTest {
    private NestService service;
    private ChildProfile ana;

    @BeforeEach
    void setUp(@TempDir Path dir) {
        System.setProperty("nest.db", dir.resolve("nest-test.db").toString());
        int guardian = new AuthService().register("responsavel", "senha123").id();
        service = new NestService(guardian);
        ana = service.addChild("Ana", 12);
    }

    @Test
    void somaUsoDoDiaDeVariasPlataformas() {
        LocalDate hoje = LocalDate.now();
        service.addUsage(ana.id(), Platforms.NINTENDO, hoje, 60);
        service.addUsage(ana.id(), Platforms.GOOGLE, hoje, 45);
        assertEquals(105, service.totalMinutes(ana.id(), hoje));
        assertEquals(60, service.totalMinutes(ana.id(), Platforms.NINTENDO, hoje));
    }

    @Test
    void naoSomaUsoDeOutroPerfil() {
        ChildProfile bia = service.addChild("Bia", 10);
        service.addUsage(ana.id(), Platforms.NINTENDO, LocalDate.now(), 60);
        service.addUsage(bia.id(), Platforms.NINTENDO, LocalDate.now(), 30);
        assertEquals(60, service.totalMinutes(ana.id(), LocalDate.now()));
    }

    @Test
    void relatorioPorDiaPreencheDiasSemUsoComZero() {
        LocalDate hoje = LocalDate.now();
        service.addUsage(ana.id(), Platforms.APPLE, hoje, 20);
        Map<LocalDate, Integer> dias = service.minutesByDay(ana.id(), hoje.minusDays(6), hoje);
        assertEquals(7, dias.size());
        assertEquals(20, dias.get(hoje));
        assertEquals(0, dias.get(hoje.minusDays(3)));
    }

    @Test
    void impedeVinculoDuplicadoNoMesmoPerfil() {
        service.linkPlatform(ana.id(), Platforms.GOOGLE, "ana@exemplo");
        assertThrows(IllegalArgumentException.class, () -> service.linkPlatform(ana.id(), Platforms.GOOGLE, "outra"));
        assertEquals(1, service.listLinks(ana.id()).size());
    }

    @Test
    void limiteExcedidoGeraUmUnicoAlertaPorDia() {
        service.requestSetting(ana.id(), Platforms.NINTENDO, NestService.SETTING_DAILY_LIMIT, "60");
        service.addUsage(ana.id(), Platforms.NINTENDO, LocalDate.now(), 50);
        assertTrue(service.listAlerts(ana.id()).isEmpty());
        service.addUsage(ana.id(), Platforms.NINTENDO, LocalDate.now(), 30);
        service.addUsage(ana.id(), Platforms.NINTENDO, LocalDate.now(), 10);
        List<Alert> alertas = service.listAlerts(ana.id());
        assertEquals(1, alertas.size());
        assertEquals(1, service.unreadAlerts(ana.id()));
        service.markAllRead(ana.id());
        assertEquals(0, service.unreadAlerts(ana.id()));
    }

    @Test
    void solicitacaoDeConfiguracaoFicaMarcadaComoSimulada() {
        var r = service.requestSetting(ana.id(), Platforms.GOOGLE, NestService.SETTING_QUIET_HOURS, "21:00-07:00");
        assertEquals("SIMULADO", r.status());
        assertEquals(1, service.listRequests(ana.id()).size());
        assertThrows(IllegalArgumentException.class,
                () -> service.requestSetting(ana.id(), Platforms.GOOGLE, NestService.SETTING_DAILY_LIMIT, "abc"));
    }

    @Test
    void sincronizacaoSimuladaNaoDuplicaDados() {
        service.linkPlatform(ana.id(), Platforms.NINTENDO, "Switch da Ana");
        var demo = new DemoService(service);
        demo.sync(ana.id());
        int primeira = service.listSessions(ana.id(), NestService.ALL, LocalDate.now().minusDays(6), LocalDate.now()).size();
        demo.sync(ana.id());
        int segunda = service.listSessions(ana.id(), NestService.ALL, LocalDate.now().minusDays(6), LocalDate.now()).size();
        assertEquals(primeira, segunda);
    }

    @Test
    void responsavelNaoEnxergaPerfilDeOutro() {
        int outro = new AuthService().register("outrapessoa", "senha123").id();
        NestService s2 = new NestService(outro);
        assertTrue(s2.listChildren().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> s2.addUsage(ana.id(), Platforms.GOOGLE, LocalDate.now(), 10));
    }

    @Test
    void removerPerfilApagaOsDadosRelacionados() {
        service.linkPlatform(ana.id(), Platforms.GOOGLE, "ana@exemplo");
        service.addUsage(ana.id(), Platforms.GOOGLE, LocalDate.now(), 15);
        service.removeChild(ana.id());
        assertTrue(service.listChildren().isEmpty());
    }
}
