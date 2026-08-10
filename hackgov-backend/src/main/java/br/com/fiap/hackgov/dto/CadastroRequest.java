package br.com.fiap.hackgov.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Dados que o cidadao envia ao se cadastrar.
 *
 * Isto e um "record": uma forma curta de criar uma classe so para guardar
 * dados. As anotacoes (@NotBlank, @Email...) sao as REGRAS DE VALIDACAO.
 * Se o usuario nao preencher um campo obrigatorio, a API recusa antes mesmo
 * de chegar no banco.
 */
public record CadastroRequest(

        @NotBlank(message = "O nome e obrigatorio")
        String nome,

        @NotBlank(message = "O CPF e obrigatorio")
        @Pattern(regexp = "\\d{11}", message = "O CPF deve ter 11 numeros, sem pontos ou tracos")
        String cpf,

        @NotBlank(message = "O e-mail e obrigatorio")
        @Email(message = "E-mail invalido")
        String email,

        @NotBlank(message = "A senha e obrigatoria")
        @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres")
        String senha
) {
}
