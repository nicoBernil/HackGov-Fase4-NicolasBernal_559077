package br.com.fiap.hackgov.dto;

import java.util.List;

/**
 * Relatorio estatistico AVANCADO (Visao de Futuro - Parte 3): correlacao e
 * regressao linear sobre os dados reais das solicitacoes, para apoiar a
 * tomada de decisao do gestor.
 *
 * - correlacoes: cada analise traz o coeficiente de Pearson (r), a reta de
 *   regressao (y = intercepto + inclinacao * x), o R2 e uma leitura em
 *   portugues do que o numero significa para a gestao.
 * - previsaoPorPrioridade: prazo previsto de conclusao (em dias) para cada
 *   nivel de prioridade, calculado pela reta de regressao.
 * - solicitacoesParadas: solicitacoes abertas que estao pelo menos meia
 *   etapa ATRAS do que a regressao esperava para a idade delas.
 */
public record AnaliseEstatisticaResponse(
        List<Correlacao> correlacoes,
        List<PrevisaoPrazo> previsaoPorPrioridade,
        List<SolicitacaoParada> solicitacoesParadas
) {

    /** r, intercepto, inclinacao e r2 ficam null quando nao ha dados suficientes. */
    public record Correlacao(
            String codigo,
            String titulo,
            String variavelX,
            String variavelY,
            int amostra,
            Double r,
            Double intercepto,
            Double inclinacao,
            Double r2,
            String forca,
            String leitura
    ) {
    }

    public record PrevisaoPrazo(String prioridade, double diasPrevistos) {
    }

    public record SolicitacaoParada(
            Long id,
            String protocolo,
            String titulo,
            String statusAtual,
            String statusEsperado,
            double diasEmAberto
    ) {
    }
}
