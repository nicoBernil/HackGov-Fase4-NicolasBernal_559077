package br.com.fiap.hackgov.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Dados enviados pelo gestor ao mudar o status de uma solicitacao.
 */
public record AlterarStatusRequest(

        @NotNull(message = "Selecione o novo status")
        Long idNovoStatus,

        @NotNull(message = "Informe quem esta realizando a alteracao")
        Long idGestor,

        String observacao
) {
}
