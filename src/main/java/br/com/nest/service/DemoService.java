package br.com.nest.service;

import br.com.nest.model.ChildProfile;
import br.com.nest.model.Platforms;
import br.com.nest.model.PlatformLink;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Simula a sincronização com os controles parentais. Os números são gerados localmente (determinísticos por
 * perfil/plataforma/dia) e marcados como SIMULADO: nenhum serviço externo é consultado.
 */
public class DemoService {
    private final NestService service;

    public DemoService(NestService service) {
        this.service = service;
    }

    /** Gera os últimos 7 dias de uso para cada plataforma vinculada ao perfil. Retorna quantas plataformas foram sincronizadas. */
    public int sync(int childId) {
        List<PlatformLink> links = service.listLinks(childId);
        LocalDate today = LocalDate.now();
        for (PlatformLink l : links) {
            for (int i = 6; i >= 0; i--) {
                LocalDate day = today.minusDays(i);
                Random rnd = new Random(Objects.hash(childId, l.platform(), day.toEpochDay()));
                int minutes = rnd.nextInt(10) < 2 ? 0 : 10 + rnd.nextInt(71); // alguns dias sem uso, senão 10–80 min
                service.replaceSimulatedUsage(childId, l.platform(), day, minutes);
            }
            service.touchSync(l.id());
        }
        return links.size();
    }

    /** Cria perfis de exemplo já vinculados, com limites e dados simulados, para demonstrar o MVP. */
    public void loadDemoFamily() {
        ChildProfile ana = service.addChild("Ana", 12);
        ChildProfile bia = service.addChild("Bia", 9);
        service.linkPlatform(ana.id(), Platforms.GOOGLE, "ana.demo@exemplo");
        service.linkPlatform(ana.id(), Platforms.NINTENDO, "Perfil Ana (Switch)");
        service.linkPlatform(ana.id(), Platforms.APPLE, "iPad da Ana");
        service.linkPlatform(bia.id(), Platforms.NINTENDO, "Perfil Bia (Switch)");
        service.linkPlatform(bia.id(), Platforms.GOOGLE, "bia.demo@exemplo");
        service.requestSetting(ana.id(), NestService.ALL, NestService.SETTING_DAILY_LIMIT, "180");
        service.requestSetting(ana.id(), Platforms.NINTENDO, NestService.SETTING_DAILY_LIMIT, "60");
        service.requestSetting(bia.id(), NestService.ALL, NestService.SETTING_DAILY_LIMIT, "120");
        sync(ana.id());
        sync(bia.id());
    }
}
