package br.com.fiap.hackgov.model;

/**
 * Define os dois tipos de usuario do sistema.
 * Usar um "enum" (em vez de texto solto) evita erros de digitacao tipo
 * "GESTORR" ou "cidadao" e deixa o codigo mais seguro.
 */
public enum Perfil {
    CIDADAO,
    GESTOR
}
