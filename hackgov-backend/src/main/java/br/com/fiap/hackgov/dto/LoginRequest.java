package br.com.fiap.hackgov.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Dados que o usuario envia ao fazer login.
 */
public record LoginRequest(

        @NotBlank(message = "O e-mail e obrigatorio")
        String email,

        @NotBlank(message = "A senha e obrigatoria")
        String senha
) {
}
