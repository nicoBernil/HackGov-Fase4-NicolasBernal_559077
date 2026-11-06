package br.com.fiap.hackgov.dto;

import br.com.fiap.hackgov.model.StatusSolicitacao;

/**
 * Um status possivel de solicitacao, para preencher o <select> do gestor
 * na hora de mudar o status de uma solicitacao.
 */
public record StatusResponse(Long id, String nome) {
    public static StatusResponse de(StatusSolicitacao s) {
        return new StatusResponse(s.getId(), s.getNome());
    }
}
