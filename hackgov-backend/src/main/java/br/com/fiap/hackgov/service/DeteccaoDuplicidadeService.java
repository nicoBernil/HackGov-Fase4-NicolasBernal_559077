package br.com.fiap.hackgov.service;

import br.com.fiap.hackgov.dto.SolicitacaoSimilarResponse;
import br.com.fiap.hackgov.dto.VerificarDuplicidadeRequest;
import br.com.fiap.hackgov.model.Solicitacao;
import br.com.fiap.hackgov.repository.SolicitacaoRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * IA - DETECCAO DE SOLICITACOES DUPLICADAS (Fase 5, Parte 3).
 *
 * PROBLEMA: varios cidadaos costumam reclamar do MESMO problema (o mesmo
 * buraco, o mesmo poste apagado). Cada reclamacao repetida vira uma nova
 * solicitacao que o gestor precisa ler, triar e responder - retrabalho.
 *
 * SOLUCAO: antes de enviar, o texto e o endereco da nova solicitacao sao
 * comparados com as solicitacoes AINDA ABERTAS do mesmo bairro/cidade. Se
 * alguma for muito parecida, o cidadao e avisado ("ja existe o protocolo X
 * sobre isso") e decide se ainda quer registrar.
 *
 * TECNICA (Processamento de Linguagem Natural - PLN):
 *   1. Normalizacao: minusculas, sem acento, sem pontuacao, sem "stopwords"
 *      (palavras sem significado como "de", "na", "o") e com um stemming
 *      simples (tira o plural: "buracos" -> "buraco").
 *   2. TF-IDF: cada texto vira um vetor de pesos. Palavras raras no bairro
 *      (ex.: "cratera") pesam mais que palavras comuns (ex.: "rua").
 *   3. Similaridade do cosseno entre os vetores (0 = nada a ver, 1 = igual).
 *   4. Nota final = 70% texto + 20% logradouro + 10% mesma categoria.
 *
 * DADOS USADOS: titulo, descricao, logradouro, bairro, cidade e categoria
 * das solicitacoes ja gravadas na tabela SOLICITACAO com DATA_CONCLUSAO nula
 * (ou seja, ainda em aberto). Nenhum dado pessoal do outro cidadao (nome,
 * descricao completa) e devolvido - so protocolo, titulo e status.
 *
 * COMPLEXIDADE: n = solicitacoes abertas, k = candidatas no mesmo bairro,
 * m = palavras por texto. Filtrar por bairro: O(n). Montar os vetores e
 * comparar: O(k * m). Ordenar o resultado: O(k log k).
 *
 * Assim como o IaClassificacaoService, roda 100% local: sem API externa,
 * sem chave, sem custo e sem depender de internet na apresentacao.
 */
@Service
public class DeteccaoDuplicidadeService {

    /** Abaixo desta nota (0 a 1) a solicitacao nao e considerada parecida. */
    static final double LIMIAR_SIMILARIDADE = 0.45;

    /** No maximo quantas solicitacoes parecidas mostrar ao cidadao. */
    static final int MAXIMO_RESULTADOS = 3;

    private static final double PESO_TEXTO = 0.70;
    private static final double PESO_ENDERECO = 0.20;
    private static final double PESO_CATEGORIA = 0.10;

    private static final Set<String> STOPWORDS = Set.of(
            "a", "o", "as", "os", "um", "uma", "uns", "umas", "de", "do", "da", "dos", "das",
            "em", "no", "na", "nos", "nas", "por", "pelo", "pela", "para", "pra", "com", "sem",
            "e", "ou", "que", "se", "ao", "aos", "ja", "esta", "estao", "tem", "ha", "foi",
            "muito", "muita", "mais", "minha", "meu", "nossa", "nosso", "aqui", "ali", "la",
            "frente", "perto", "proximo", "proxima", "casa", "numero", "n", "dia", "dias",
            "faz", "favor", "problema", "porque", "quando", "isso", "esse", "essa", "este");

    // Palavras de tipo de via: "Rua das Flores" e "R. das Flores" devem bater.
    private static final Set<String> TIPOS_DE_VIA = Set.of(
            "rua", "r", "avenida", "av", "travessa", "tv", "estrada", "estr", "rodovia", "rod",
            "alameda", "al", "praca", "pca", "largo", "beco", "via", "s", "sn");

    private final SolicitacaoRepository solicitacaoRepository;

    public DeteccaoDuplicidadeService(SolicitacaoRepository solicitacaoRepository) {
        this.solicitacaoRepository = solicitacaoRepository;
    }

    /**
     * Procura solicitacoes abertas parecidas com a que o cidadao esta
     * preenchendo. Devolve no maximo 3, da mais parecida para a menos.
     */
    public List<SolicitacaoSimilarResponse> buscarSimilares(VerificarDuplicidadeRequest dados) {
        String bairro = normalizar(dados.bairro());
        String cidade = normalizar(dados.cidade());

        // Candidatas: so as ABERTAS e do MESMO bairro/cidade. Uma solicitacao
        // concluida nao e duplicata - se o problema voltou, e um caso novo.
        List<Solicitacao> candidatas = solicitacaoRepository.findByDataConclusaoIsNull()
                .stream()
                .filter(s -> normalizar(s.getCidade()).equals(cidade))
                .filter(s -> normalizar(s.getBairro()).equals(bairro))
                .toList();

        if (candidatas.isEmpty()) {
            return List.of();
        }

        List<String> textoNova = tokenizar(juntar(dados.titulo(), dados.descricao()));
        List<List<String>> textosCandidatas = candidatas.stream()
                .map(s -> tokenizar(juntar(s.getTitulo(), s.getDescricao())))
                .toList();

        // O "vocabulario" do IDF e o proprio bairro: candidatas + a nova.
        List<List<String>> corpus = new ArrayList<>(textosCandidatas);
        corpus.add(textoNova);
        Map<String, Double> idf = calcularIdf(corpus);

        Map<String, Double> vetorNova = vetorTfIdf(textoNova, idf);
        Set<String> enderecoNova = tokensEndereco(dados.logradouro());

        List<SolicitacaoSimilarResponse> resultado = new ArrayList<>();
        for (int i = 0; i < candidatas.size(); i++) {
            Solicitacao candidata = candidatas.get(i);

            double texto = similaridadeCosseno(vetorNova, vetorTfIdf(textosCandidatas.get(i), idf));
            double endereco = jaccard(enderecoNova, tokensEndereco(candidata.getLogradouro()));
            double categoria = dados.idCategoria() != null
                    && dados.idCategoria().equals(candidata.getCategoria().getId()) ? 1.0 : 0.0;

            double nota = PESO_TEXTO * texto + PESO_ENDERECO * endereco + PESO_CATEGORIA * categoria;

            if (nota >= LIMIAR_SIMILARIDADE) {
                resultado.add(SolicitacaoSimilarResponse.de(candidata, (int) Math.round(nota * 100)));
            }
        }

        return resultado.stream()
                .sorted(Comparator.comparingInt(SolicitacaoSimilarResponse::similaridadePercentual).reversed())
                .limit(MAXIMO_RESULTADOS)
                .toList();
    }

    // ------------------------------------------------------------------
    // Funcoes de PLN. Sao "static" e sem acesso ao banco para poderem ser
    // testadas isoladamente (ver DeteccaoDuplicidadeServiceTest).
    // ------------------------------------------------------------------

    /** Minusculas, sem acento e so letras/numeros separados por espaco. */
    static String normalizar(String texto) {
        if (texto == null) return "";
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    /** Quebra o texto em palavras uteis: sem stopwords e sem plural. */
    static List<String> tokenizar(String texto) {
        List<String> tokens = new ArrayList<>();
        for (String palavra : normalizar(texto).split(" ")) {
            if (palavra.length() < 2 || STOPWORDS.contains(palavra)) continue;
            tokens.add(radical(palavra));
        }
        return tokens;
    }

    /** Stemming bem simples: "buracos" -> "buraco", "postes" -> "poste". */
    static String radical(String palavra) {
        if (palavra.length() > 4 && palavra.endsWith("s")) {
            return palavra.substring(0, palavra.length() - 1);
        }
        return palavra;
    }

    /** IDF suavizado: palavra presente em poucos textos ganha peso maior. */
    static Map<String, Double> calcularIdf(List<List<String>> corpus) {
        Map<String, Integer> emQuantosTextos = new HashMap<>();
        for (List<String> texto : corpus) {
            for (String termo : new HashSet<>(texto)) {
                emQuantosTextos.merge(termo, 1, Integer::sum);
            }
        }
        int total = corpus.size();
        Map<String, Double> idf = new HashMap<>();
        emQuantosTextos.forEach((termo, df) ->
                idf.put(termo, Math.log((1.0 + total) / (1.0 + df)) + 1.0));
        return idf;
    }

    /** TF-IDF: (vezes que a palavra aparece no texto) x (raridade no bairro). */
    static Map<String, Double> vetorTfIdf(List<String> tokens, Map<String, Double> idf) {
        Map<String, Double> vetor = new HashMap<>();
        for (String termo : tokens) {
            vetor.merge(termo, 1.0, Double::sum);
        }
        vetor.replaceAll((termo, tf) -> tf * idf.getOrDefault(termo, 1.0));
        return vetor;
    }

    /** Cosseno entre dois vetores: 1 = mesma direcao (textos iguais), 0 = nada em comum. */
    static double similaridadeCosseno(Map<String, Double> a, Map<String, Double> b) {
        if (a.isEmpty() || b.isEmpty()) return 0.0;
        double produto = 0.0;
        for (Map.Entry<String, Double> e : a.entrySet()) {
            produto += e.getValue() * b.getOrDefault(e.getKey(), 0.0);
        }
        double normaA = Math.sqrt(a.values().stream().mapToDouble(v -> v * v).sum());
        double normaB = Math.sqrt(b.values().stream().mapToDouble(v -> v * v).sum());
        return produto / (normaA * normaB);
    }

    /**
     * Palavras do logradouro sem o tipo de via ("Rua", "Av.") e sem o numero:
     * "Rua das Flores, 118" e "R. das Flores 120" sao o mesmo trecho de rua -
     * o cidadao costuma informar o numero da casa dele, nao o do buraco.
     */
    static Set<String> tokensEndereco(String logradouro) {
        Set<String> tokens = new HashSet<>();
        for (String palavra : normalizar(logradouro).split(" ")) {
            if (palavra.isEmpty() || palavra.matches("\\d+")
                    || TIPOS_DE_VIA.contains(palavra) || STOPWORDS.contains(palavra)) continue;
            tokens.add(palavra);
        }
        return tokens;
    }

    /** Jaccard: palavras em comum / total de palavras diferentes. */
    static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) return 0.0;
        Set<String> intersecao = new HashSet<>(a);
        intersecao.retainAll(b);
        Set<String> uniao = new HashSet<>(a);
        uniao.addAll(b);
        return (double) intersecao.size() / uniao.size();
    }

    private static String juntar(String titulo, String descricao) {
        return (titulo == null ? "" : titulo) + " " + (descricao == null ? "" : descricao);
    }
}
