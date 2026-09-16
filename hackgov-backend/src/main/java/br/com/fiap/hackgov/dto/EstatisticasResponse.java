package br.com.fiap.hackgov.dto;

import java.util.List;

/**
 * Relatorio estatistico das solicitacoes, usado no painel do gestor e no
 * relatorio da Parte 4 (estruturas de dados avancadas + estatisticas).
 *
 * "itens" e uma lista generica de {rotulo, total} para poder desenhar tanto
 * o grafico por categoria quanto o grafico por prioridade/status com o
 * mesmo componente no front-end.
 */
public record EstatisticasResponse(
        long totalSolicitacoes,
        List<ItemEstatistica> porCategoria,
        List<ItemEstatistica> porPrioridade,
        List<ItemEstatistica> porStatus,
        Double tempoMedioResolucaoDias // null se nenhuma solicitacao foi concluida ainda
) {
    public record ItemEstatistica(String rotulo, long total) {
    }
}
