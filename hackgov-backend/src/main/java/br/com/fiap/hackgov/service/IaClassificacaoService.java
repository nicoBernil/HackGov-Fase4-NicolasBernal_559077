package br.com.fiap.hackgov.service;

import br.com.fiap.hackgov.dto.ClassificacaoIaResponse;
import br.com.fiap.hackgov.model.Categoria;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Servico responsavel por sugerir CATEGORIA e PRIORIDADE de uma solicitacao a
 * partir do texto livre digitado pelo cidadao.
 *
 * A classificacao e 100% local: nao chama nenhuma API externa, nao precisa de
 * chave nem de internet. O texto digitado e comparado com palavras-chave
 * tipicas de cada tipo de ocorrencia urbana (ex.: "poste", "buraco", "lixo")
 * e com termos que indicam urgencia (ex.: "risco", "crianca", "acidente").
 *
 * Isso e um classificador baseado em regras - uma tecnica classica de
 * Inteligencia Artificial conhecida como "sistema especialista": em vez de a
 * pessoa escolher manualmente a categoria e a prioridade, o sistema LE a
 * descricao e decide sozinho, com base em conhecimento especialista
 * codificado nas regras abaixo.
 *
 * Vantagem para a demonstracao: nunca falha por falta de internet, nao tem
 * custo e nao depende de nenhum servico de terceiros no dia da apresentacao.
 */
@Service
public class IaClassificacaoService {

    public ClassificacaoIaResponse classificar(String descricao, List<Categoria> categoriasDisponiveis) {
        String texto = descricao == null ? "" : descricao.toLowerCase(Locale.ROOT);

        String trechoCategoria = achaTrechoCategoriaPorTexto(texto);
        Categoria escolhida = categoriasDisponiveis.stream()
                .filter(c -> c.getNome().toLowerCase(Locale.ROOT).contains(trechoCategoria))
                .findFirst()
                .orElse(categoriasDisponiveis.isEmpty() ? null : categoriasDisponiveis.get(0));

        String prioridade = achaPrioridadePorTexto(texto);

        return new ClassificacaoIaResponse(
                escolhida != null ? escolhida.getNome() : null,
                escolhida != null ? escolhida.getId() : null,
                prioridade,
                "Classificado automaticamente com base no texto da descricao.",
                true
        );
    }

    // OBS: cada trecho retornado aqui precisa ser um pedaco do NOME real da
    // categoria no banco (ver dados_iniciais.sql), senao o filtro por
    // "contains" abaixo nunca acha a categoria certa e cai no fallback (a
    // primeira categoria em ordem alfabetica) - era o que acontecia antes
    // com buraco/lixo/transito.
    private String achaTrechoCategoriaPorTexto(String texto) {
        if (contemAlguma(texto, "poste", "luz", "ilumina", "lampada", "lâmpada")) return "ilumina"; // Iluminacao Publica
        if (contemAlguma(texto, "buraco", "asfalto", "calcada", "calçada", "via")) return "buraco"; // Buraco na Via
        if (contemAlguma(texto, "lixo", "entulho", "descarte", "limpeza")) return "lixo"; // Coleta de Lixo
        if (contemAlguma(texto, "arvore", "árvore", "poda", "galho", "praca", "praça")) return "arboriz"; // Arborizacao e Pracas
        if (contemAlguma(texto, "esgoto", "vazamento", "agua", "água", "saneamento")) return "saneamento"; // Saneamento e Esgoto
        if (contemAlguma(texto, "semaforo", "semáforo", "sinaliza", "placa", "transito", "trânsito", "faixa de pedestre"))
            return "sinaliza"; // Sinalizacao e Transito
        return "outros"; // cai na categoria "Outros" em vez de sortear a primeira em ordem alfabetica
    }

    private String achaPrioridadePorTexto(String texto) {
        if (contemAlguma(texto, "risco", "perigo", "urgente", "grave", "crianca", "criança",
                "idoso", "acidente", "exposto", "vazamento de gas")) {
            return "ALTA";
        }
        if (contemAlguma(texto, "pequeno", "estetico", "estético", "leve")) {
            return "BAIXA";
        }
        return "MEDIA";
    }

    private boolean contemAlguma(String texto, String... termos) {
        for (String termo : termos) {
            if (texto.contains(termo)) return true;
        }
        return false;
    }
}
