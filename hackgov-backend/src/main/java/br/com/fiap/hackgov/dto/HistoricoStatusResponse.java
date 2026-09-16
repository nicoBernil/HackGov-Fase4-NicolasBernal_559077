package br.com.fiap.hackgov.dto;

import br.com.fiap.hackgov.model.HistoricoStatus;

import java.time.format.DateTimeFormatter;

/**
 * Uma linha do historico de status de uma solicitacao, pronta para exibir
 * na tela (nomes ja resolvidos, sem expor os objetos completos de status/usuario).
 */
public record HistoricoStatusResponse(
        Long id,
        String status,
        String responsavel,
        String observacao,
        String dataAlteracao
) {
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static HistoricoStatusResponse de(HistoricoStatus h) {
        return new HistoricoStatusResponse(
                h.getId(),
                h.getStatus().getNome(),
                h.getAtor().getNome(),
                h.getObservacao(),
                h.getDataAlteracao().format(FORMATO)
        );
    }
}
