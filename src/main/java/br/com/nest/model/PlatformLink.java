package br.com.nest.model;

/** Vínculo local entre um perfil e um controle parental. Nenhuma conta externa é realmente conectada no MVP. */
public record PlatformLink(int id, int childId, String platform, String accountLabel, String status, String lastSync) {}
