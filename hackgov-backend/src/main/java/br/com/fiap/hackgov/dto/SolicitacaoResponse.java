package br.com.fiap.hackgov.dto;

import br.com.fiap.hackgov.model.Solicitacao;

import java.time.format.DateTimeFormatter;

/**
 * Dados de uma solicitacao enviados para o front-end.
 *
 * Aqui "achatamos" as ligacoes: em vez de devolver o objeto Categoria inteiro,
 * devolvemos so o nome dela. Isso deixa o JSON simples e facil de exibir na tela.
 */
public record SolicitacaoResponse(
        Long id,
        String protocolo,
        String titulo,
        String descricao,
        String categoria,
        String status,
        String prioridade,
        String logradouro,
        String bairro,
        String cidade,
        String cidadao,
        String dataAbertura
) {
    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static SolicitacaoResponse de(Solicitacao s) {
        return new SolicitacaoResponse(
                s.getId(),
                s.getProtocolo(),
                s.getTitulo(),
                s.getDescricao(),
                s.getCategoria().getNome(),
                s.getStatus().getNome(),
                s.getPrioridade(),
                s.getLogradouro(),
                s.getBairro(),
                s.getCidade(),
                s.getCidadao().getNome(),
                s.getDataAbertura().format(FORMATO)
        );
    }
}
