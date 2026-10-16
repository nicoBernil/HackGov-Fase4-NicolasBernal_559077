package br.com.fiap.hackgov.dto;

import br.com.fiap.hackgov.model.Solicitacao;

import java.time.format.DateTimeFormatter;

/**
 * Uma solicitacao aberta parecida com a que o cidadao esta registrando.
 *
 * PRIVACIDADE: de proposito NAO tem o nome do cidadao que abriu nem a
 * descricao completa - quem esta registrando so precisa saber que o
 * problema ja foi relatado (protocolo, titulo, local e status).
 */
public record SolicitacaoSimilarResponse(
        Long id,
        String protocolo,
        String titulo,
        String categoria,
        String status,
        String logradouro,
        String bairro,
        String dataAbertura,
        int similaridadePercentual
) {
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static SolicitacaoSimilarResponse de(Solicitacao s, int similaridadePercentual) {
        return new SolicitacaoSimilarResponse(
                s.getId(),
                s.getProtocolo(),
                s.getTitulo(),
                s.getCategoria().getNome(),
                s.getStatus().getNome(),
                s.getLogradouro(),
                s.getBairro(),
                s.getDataAbertura().format(FORMATO),
                similaridadePercentual
        );
    }
}
