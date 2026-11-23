package br.com.nest.model;

/** Alerta gerado pelo Nest (ex.: limite diário excedido). */
public record Alert(int id, int childId, String childName, String platform, String message, String createdAt, boolean read) {}
