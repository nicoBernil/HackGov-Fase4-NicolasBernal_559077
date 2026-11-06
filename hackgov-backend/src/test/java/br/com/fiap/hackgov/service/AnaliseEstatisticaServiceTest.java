package br.com.fiap.hackgov.service;

import br.com.fiap.hackgov.dto.AnaliseEstatisticaResponse;
import br.com.fiap.hackgov.dto.AnaliseEstatisticaResponse.Correlacao;
import br.com.fiap.hackgov.model.Solicitacao;
import br.com.fiap.hackgov.model.StatusSolicitacao;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes da analise estatistica (correlacao e regressao). Usam os mesmos 20
 * registros do script dados_teste_estatisticas.sql, entao os numeros
 * esperados batem com o relatorio da Parte 3 (calculados tambem em Python,
 * na pasta analise/). Nao precisam do Oracle.
 *
 * Para rodar: na pasta hackgov-backend, execute  mvn test
 */
class AnaliseEstatisticaServiceTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 5, 12, 0);
    private static final double TOLERANCIA = 0.001;

    private static final StatusSolicitacao RECEBIDO = status("Recebido", 1);
    private static final StatusSolicitacao ANALISE = status("Em Analise", 2);
    private static final StatusSolicitacao ANDAMENTO = status("Em Andamento", 3);
    private static final StatusSolicitacao CONCLUIDO = status("Concluido", 4);

    private final AnaliseEstatisticaService servico = new AnaliseEstatisticaService(null);

    @Test
    void regressaoLinearDeUmaRetaPerfeita() {
        var reg = AnaliseEstatisticaService.regressaoLinear(new double[]{1, 2, 3, 4}, new double[]{3, 5, 7, 9});
        assertEquals(1.0, reg.r(), TOLERANCIA);
        assertEquals(2.0, reg.inclinacao(), TOLERANCIA);
        assertEquals(1.0, reg.intercepto(), TOLERANCIA);
    }

    @Test
    void semVariacaoOuComPoucosDadosNaoCalcula() {
        assertNull(AnaliseEstatisticaService.regressaoLinear(new double[]{1, 2}, new double[]{1, 2}));
        assertNull(AnaliseEstatisticaService.regressaoLinear(new double[]{2, 2, 2}, new double[]{1, 5, 9}));
    }

    @Test
    void prioridadeAltaDemoraMaisParaConcluir() {
        Correlacao a = analisar().correlacoes().get(0);
        assertEquals(10, a.amostra());
        assertEquals(0.688, a.r(), TOLERANCIA);
        assertEquals(3.0, a.inclinacao(), TOLERANCIA);
        assertEquals(-2.0, a.intercepto(), TOLERANCIA);
        assertEquals("moderada positiva", a.forca());
    }

    @Test
    void prevePrazoPorPrioridade() {
        var previsoes = analisar().previsaoPorPrioridade();
        assertEquals(1.0, previsoes.get(0).diasPrevistos(), TOLERANCIA); // BAIXA
        assertEquals(4.0, previsoes.get(1).diasPrevistos(), TOLERANCIA); // MEDIA
        assertEquals(7.0, previsoes.get(2).diasPrevistos(), TOLERANCIA); // ALTA
    }

    @Test
    void solicitacoesAbertasAvancamComOTempoEUmaEstaParada() {
        AnaliseEstatisticaResponse resposta = analisar();
        Correlacao b = resposta.correlacoes().get(1);
        assertEquals(0.863, b.r(), TOLERANCIA);
        assertEquals(0.554, b.inclinacao(), TOLERANCIA);

        assertEquals(1, resposta.solicitacoesParadas().size());
        var parada = resposta.solicitacoesParadas().get(0);
        assertEquals("HGSEED0015", parada.protocolo());
        assertEquals("Em Analise", parada.statusAtual());
        assertEquals("Em Andamento", parada.statusEsperado());
    }

    @Test
    void descricaoMaiorTendeAPrioridadeMaior() {
        Correlacao c = analisar().correlacoes().get(2);
        assertEquals(20, c.amostra());
        assertEquals(0.532, c.r(), TOLERANCIA);
        assertTrue(c.inclinacao() > 0);
    }

    // ------------------------------------------------------------------

    private AnaliseEstatisticaResponse analisar() {
        return servico.gerar(dadosDeTeste(), AGORA);
    }

    /** Os 20 registros do dados_teste_estatisticas.sql (dias em aberto e dias para concluir). */
    private static List<Solicitacao> dadosDeTeste() {
        List<Solicitacao> lista = new ArrayList<>();
        lista.add(nova(1, "ALTA", CONCLUIDO, 12, 10, 68));
        lista.add(nova(2, "BAIXA", CONCLUIDO, 20, 2, 56));
        lista.add(nova(3, "MEDIA", ANDAMENTO, 5, null, 42));
        lista.add(nova(4, "MEDIA", CONCLUIDO, 9, 3, 59));
        lista.add(nova(5, "ALTA", CONCLUIDO, 7, 6, 68));
        lista.add(nova(6, "BAIXA", RECEBIDO, 1, null, 55));
        lista.add(nova(7, "ALTA", CONCLUIDO, 15, 11, 69));
        lista.add(nova(8, "MEDIA", ANALISE, 3, null, 52));
        lista.add(nova(9, "MEDIA", CONCLUIDO, 10, 3, 42));
        lista.add(nova(10, "ALTA", ANDAMENTO, 4, null, 67));
        lista.add(nova(11, "MEDIA", CONCLUIDO, 8, 3, 37));
        lista.add(nova(12, "MEDIA", RECEBIDO, 2, null, 37));
        lista.add(nova(13, "ALTA", CONCLUIDO, 6, 5, 74));
        lista.add(nova(14, "BAIXA", RECEBIDO, 1, null, 50));
        lista.add(nova(15, "MEDIA", ANALISE, 4, null, 55));
        lista.add(nova(16, "ALTA", CONCLUIDO, 5, 4, 68));
        lista.add(nova(17, "MEDIA", ANDAMENTO, 3, null, 49));
        lista.add(nova(18, "BAIXA", RECEBIDO, 2, null, 59));
        lista.add(nova(19, "MEDIA", CONCLUIDO, 11, 5, 62));
        lista.add(nova(20, "BAIXA", RECEBIDO, 1, null, 53));
        return lista;
    }

    private static Solicitacao nova(long id, String prioridade, StatusSolicitacao status,
                                    int diasAberta, Integer diasParaConcluir, int tamanhoDescricao) {
        Solicitacao s = new Solicitacao();
        s.setId(id);
        s.setProtocolo(String.format("HGSEED%04d", id));
        s.setTitulo("Solicitacao de teste " + id);
        s.setDescricao("x".repeat(tamanhoDescricao));
        s.setPrioridade(prioridade);
        s.setStatus(status);
        s.setDataAbertura(AGORA.minusDays(diasAberta));
        if (diasParaConcluir != null) {
            s.setDataConclusao(AGORA.minusDays(diasAberta).plusDays(diasParaConcluir));
        }
        return s;
    }

    private static StatusSolicitacao status(String nome, int ordem) {
        StatusSolicitacao st = new StatusSolicitacao();
        st.setNome(nome);
        st.setOrdem(ordem);
        return st;
    }
}
