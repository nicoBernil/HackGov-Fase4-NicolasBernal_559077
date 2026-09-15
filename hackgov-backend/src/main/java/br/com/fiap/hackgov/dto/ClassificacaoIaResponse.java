package br.com.fiap.hackgov.dto;

/**
 * Sugestao de categoria e prioridade gerada a partir da descricao do
 * cidadao. O campo "geradoPorIa" diz se veio da IA generativa (Gemini) ou do
 * Plano B por palavras-chave - util para mostrar na tela (e no video pitch)
 * qual caminho foi usado.
 */
public record ClassificacaoIaResponse(
        String categoriaSugerida,
        Long idCategoriaSugerida,
        String prioridadeSugerida,
        String justificativa,
        boolean geradoPorIa
) {
}
