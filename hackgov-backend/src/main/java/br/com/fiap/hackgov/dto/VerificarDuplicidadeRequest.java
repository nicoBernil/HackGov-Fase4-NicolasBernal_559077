package br.com.fiap.hackgov.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * O que o front-end envia para checar se ja existe uma solicitacao parecida
 * ANTES de o cidadao registrar a nova (Fase 5 - deteccao de duplicadas).
 *
 * Titulo, logradouro e categoria sao opcionais: a checagem funciona com o
 * que o cidadao ja tiver preenchido, mas fica mais precisa com tudo.
 */
public record VerificarDuplicidadeRequest(

        String titulo,

        @NotBlank(message = "A descricao e obrigatoria")
        @Size(min = 10, message = "Descreva o problema com pelo menos 10 caracteres")
        String descricao,

        String logradouro,

        @NotBlank(message = "O bairro e obrigatorio")
        String bairro,

        @NotBlank(message = "A cidade e obrigatoria")
        String cidade,

        Long idCategoria
) {
}
