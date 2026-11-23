package br.com.nest.model;

import java.time.LocalDate;

/**
 * Tempo de uso de uma plataforma em um dia.
 * {@code source} indica a origem do dado: MANUAL (lançado pelo responsável) ou SIMULADO (sincronização de demonstração).
 */
public record UsageSession(int id, int childId, String platform, LocalDate date, int minutes, String source) {

    public UsageSession {
        if (platform == null || platform.isBlank()) throw new IllegalArgumentException("Informe a plataforma.");
        if (date == null) throw new IllegalArgumentException("Informe a data.");
        if (minutes < 0) throw new IllegalArgumentException("O tempo não pode ser negativo.");
        platform = platform.trim();
        if (source == null || source.isBlank()) source = "MANUAL";
    }

    public UsageSession(int id, int childId, String platform, LocalDate date, int minutes) {
        this(id, childId, platform, date, minutes, "MANUAL");
    }

    public int getChildId() {
        return childId;
    }

    public int getMinutes() {
        return minutes;
    }
}
