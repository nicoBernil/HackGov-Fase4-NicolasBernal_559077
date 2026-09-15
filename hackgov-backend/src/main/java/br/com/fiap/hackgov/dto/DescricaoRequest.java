package br.com.fiap.hackgov.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo enviado pelo front-end ao pedir uma sugestao de classificacao por IA
 * ANTES de enviar o formulario completo de solicitacao.
 */
public record DescricaoRequest(

        @NotBlank(message = "A descricao e obrigatoria")
        @Size(min = 10, message = "Descreva o problema com pelo menos 10 caracteres")
        String descricao
) {
}
