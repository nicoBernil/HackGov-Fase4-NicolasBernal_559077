package br.com.fiap.hackgov.service;

import br.com.fiap.hackgov.dto.AnaliseEstatisticaResponse;
import br.com.fiap.hackgov.dto.AnaliseEstatisticaResponse.Correlacao;
import br.com.fiap.hackgov.dto.AnaliseEstatisticaResponse.PrevisaoPrazo;
import br.com.fiap.hackgov.dto.AnaliseEstatisticaResponse.SolicitacaoParada;
import br.com.fiap.hackgov.model.Solicitacao;
import br.com.fiap.hackgov.repository.SolicitacaoRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.function.ToDoubleFunction;

/**
 * ESTATISTICA APLICADA - correlacao e regressao (Visao de Futuro, Parte 3).
 *
 * O relatorio basico (SolicitacaoService.gerarEstatisticas) so CONTA as
 * solicitacoes. Este servico vai alem: mede se duas variaveis andam juntas
 * (correlacao de Pearson) e ajusta uma reta (regressao linear simples pelo
 * metodo dos minimos quadrados) para PREVER valores e achar excecoes.
 *
 * Tres analises, todas com dados reais da tabela SOLICITACAO:
 *   A) prioridade x dias ate concluir  (so as concluidas)
 *      -> preve o prazo de cada prioridade e mostra se as urgentes demoram mais.
 *   B) dias em aberto x etapa do fluxo  (so as abertas)
 *      -> mostra a "velocidade" do atendimento e aponta as solicitacoes paradas.
 *   C) tamanho da descricao x prioridade  (todas)
 *      -> auditoria da IA: a prioridade depende de quanto o cidadao escreve?
 *
 * Formulas (n pares x,y):
 *   r          = Sxy / raiz(Sxx * Syy)
 *   inclinacao = Sxy / Sxx
 *   intercepto = media(y) - inclinacao * media(x)
 *   R2         = r * r   (fracao da variacao de y explicada pela reta)
 * onde Sxy = soma((x - media x) * (y - media y)), Sxx e Syy analogos.
 *
 * Complexidade: O(n) por analise - uma passada para as medias e outra para
 * as somas. Roda 100% em Java, sem biblioteca externa.
 */
@Service
public class AnaliseEstatisticaService {

    /** Abaixo disso a correlacao nao tem sentido estatistico. */
    static final int AMOSTRA_MINIMA = 3;

    /** Quantas etapas atras do esperado a solicitacao precisa estar para virar alerta. */
    static final double LIMIAR_ATRASO_ETAPAS = 0.5;

    private final SolicitacaoRepository solicitacaoRepository;

    public AnaliseEstatisticaService(SolicitacaoRepository solicitacaoRepository) {
        this.solicitacaoRepository = solicitacaoRepository;
    }

    public AnaliseEstatisticaResponse gerar() {
        return gerar(solicitacaoRepository.findAll(), LocalDateTime.now());
    }

    /** Versao com "agora" como parametro, para os testes nao dependerem do relogio. */
    AnaliseEstatisticaResponse gerar(List<Solicitacao> todas, LocalDateTime agora) {
        List<Solicitacao> concluidas = todas.stream().filter(s -> s.getDataConclusao() != null).toList();
        List<Solicitacao> abertas = todas.stream().filter(s -> s.getDataConclusao() == null).toList();

        // A) prioridade x dias ate concluir
        Regressao prazo = regressao(concluidas,
                s -> pesoPrioridade(s.getPrioridade()),
                s -> dias(s.getDataAbertura(), s.getDataConclusao()));

        // B) dias em aberto x etapa do fluxo
        Regressao fluxo = regressao(abertas,
                s -> dias(s.getDataAbertura(), agora),
                s -> s.getStatus().getOrdem());

        // C) tamanho da descricao x prioridade
        Regressao descricao = regressao(todas,
                s -> s.getDescricao() == null ? 0 : s.getDescricao().length(),
                s -> pesoPrioridade(s.getPrioridade()));

        List<Correlacao> correlacoes = List.of(
                correlacao("PRIORIDADE_X_PRAZO", "Prioridade x tempo de conclusao",
                        "prioridade (1=BAIXA, 2=MEDIA, 3=ALTA)", "dias ate concluir", prazo,
                        prazo == null ? null : String.format(
                                "Cada nivel a mais de prioridade soma %.1f dias ao prazo de conclusao.",
                                prazo.inclinacao())),
                correlacao("IDADE_X_ETAPA", "Dias em aberto x etapa do fluxo",
                        "dias em aberto", "etapa (1=Recebido ... 3=Em Andamento)", fluxo,
                        fluxo == null ? null : String.format(
                                "Cada dia em aberto avanca, em media, %.2f etapa (1 etapa a cada %.1f dias).",
                                fluxo.inclinacao(), fluxo.inclinacao() > 0 ? 1 / fluxo.inclinacao() : 0.0)),
                correlacao("DESCRICAO_X_PRIORIDADE", "Tamanho da descricao x prioridade",
                        "caracteres na descricao", "prioridade (1 a 3)", descricao,
                        descricao == null ? null : String.format(
                                "Cada 10 caracteres a mais na descricao sobem a prioridade media em %.2f nivel.",
                                descricao.inclinacao() * 10))
        );

        return new AnaliseEstatisticaResponse(correlacoes, preverPrazos(prazo), acharParadas(abertas, todas, fluxo, agora));
    }

    // ------------------------------------------------------------------
    // Uso das retas para decisao
    // ------------------------------------------------------------------

    /** Prazo previsto = intercepto + inclinacao * peso da prioridade (nunca negativo). */
    private List<PrevisaoPrazo> preverPrazos(Regressao prazo) {
        if (prazo == null) return List.of();
        List<PrevisaoPrazo> previsoes = new ArrayList<>();
        for (String prioridade : List.of("BAIXA", "MEDIA", "ALTA")) {
            double dias = prazo.intercepto() + prazo.inclinacao() * pesoPrioridade(prioridade);
            previsoes.add(new PrevisaoPrazo(prioridade, Math.max(0, arredondar(dias, 1))));
        }
        return previsoes;
    }

    /**
     * Solicitacao parada = etapa atual pelo menos 0,5 abaixo da etapa que a
     * reta preve para a idade dela (residuo negativo grande).
     */
    private List<SolicitacaoParada> acharParadas(List<Solicitacao> abertas, List<Solicitacao> todas,
                                                 Regressao fluxo, LocalDateTime agora) {
        if (fluxo == null || fluxo.inclinacao() <= 0) return List.of();

        TreeMap<Integer, String> nomePorOrdem = new TreeMap<>();
        todas.forEach(s -> nomePorOrdem.put(s.getStatus().getOrdem(), s.getStatus().getNome()));
        int maiorOrdem = nomePorOrdem.lastKey();

        List<SolicitacaoParada> paradas = new ArrayList<>();
        for (Solicitacao s : abertas) {
            double idade = dias(s.getDataAbertura(), agora);
            double esperada = fluxo.intercepto() + fluxo.inclinacao() * idade;
            int atual = s.getStatus().getOrdem();
            if (esperada - atual >= LIMIAR_ATRASO_ETAPAS) {
                int ordemEsperada = (int) Math.min(maiorOrdem, Math.max(atual + 1, Math.round(esperada)));
                paradas.add(new SolicitacaoParada(s.getId(), s.getProtocolo(), s.getTitulo(),
                        s.getStatus().getNome(), nomePorOrdem.getOrDefault(ordemEsperada, "etapa " + ordemEsperada),
                        arredondar(idade, 1)));
            }
        }
        paradas.sort(Comparator.comparingDouble(SolicitacaoParada::diasEmAberto).reversed());
        return paradas;
    }

    // ------------------------------------------------------------------
    // Matematica (static para poder testar isoladamente)
    // ------------------------------------------------------------------

    /** Resultado da regressao linear simples y = intercepto + inclinacao * x. */
    record Regressao(int n, double r, double intercepto, double inclinacao, double r2) {
    }

    static Regressao regressao(List<Solicitacao> lista, ToDoubleFunction<Solicitacao> fx, ToDoubleFunction<Solicitacao> fy) {
        double[] x = lista.stream().mapToDouble(fx).toArray();
        double[] y = lista.stream().mapToDouble(fy).toArray();
        return regressaoLinear(x, y);
    }

    /** Minimos quadrados + Pearson. Devolve null se n < 3 ou se x ou y nao variam. */
    static Regressao regressaoLinear(double[] x, double[] y) {
        int n = x.length;
        if (n < AMOSTRA_MINIMA || y.length != n) return null;

        double mediaX = 0, mediaY = 0;
        for (int i = 0; i < n; i++) {
            mediaX += x[i];
            mediaY += y[i];
        }
        mediaX /= n;
        mediaY /= n;

        double sxy = 0, sxx = 0, syy = 0;
        for (int i = 0; i < n; i++) {
            double dx = x[i] - mediaX;
            double dy = y[i] - mediaY;
            sxy += dx * dy;
            sxx += dx * dx;
            syy += dy * dy;
        }
        if (sxx == 0 || syy == 0) return null;

        double r = sxy / Math.sqrt(sxx * syy);
        double inclinacao = sxy / sxx;
        double intercepto = mediaY - inclinacao * mediaX;
        return new Regressao(n, r, intercepto, inclinacao, r * r);
    }

    /** Classificacao usual do |r|: fraca < 0,3 <= moderada < 0,7 <= forte. */
    static String forca(double r) {
        double abs = Math.abs(r);
        String intensidade = abs < 0.3 ? "fraca" : abs < 0.7 ? "moderada" : "forte";
        return intensidade + (r >= 0 ? " positiva" : " negativa");
    }

    static double pesoPrioridade(String prioridade) {
        if (prioridade == null) return 2;
        return switch (prioridade.toUpperCase()) {
            case "BAIXA" -> 1;
            case "ALTA" -> 3;
            default -> 2; // MEDIA
        };
    }

    static double dias(LocalDateTime inicio, LocalDateTime fim) {
        return Duration.between(inicio, fim).toMinutes() / 1440.0;
    }

    private static Correlacao correlacao(String codigo, String titulo, String varX, String varY,
                                         Regressao reg, String leitura) {
        if (reg == null) {
            return new Correlacao(codigo, titulo, varX, varY, 0, null, null, null, null,
                    "sem dados", "Dados insuficientes: sao necessarias pelo menos 3 solicitacoes com valores diferentes.");
        }
        return new Correlacao(codigo, titulo, varX, varY, reg.n(),
                arredondar(reg.r(), 3), arredondar(reg.intercepto(), 3), arredondar(reg.inclinacao(), 3),
                arredondar(reg.r2(), 3), forca(reg.r()), leitura);
    }

    private static double arredondar(double valor, int casas) {
        double fator = Math.pow(10, casas);
        return Math.round(valor * fator) / fator;
    }
}
