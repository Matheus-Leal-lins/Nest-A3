package br.com.nest.model;

/** Solicitação de alteração de configuração. No MVP ela é apenas registrada (status SIMULADO). */
public record SettingRequest(int id, int childId, String platform, String setting, String value, String status, String createdAt) {}
