package br.com.fiap.hackgov.dto;

import br.com.fiap.hackgov.model.Categoria;

/**
 * Categoria enviada para o front-end (para montar o menu suspenso).
 */
public record CategoriaResponse(Long id, String nome) {

    public static CategoriaResponse de(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNome());
    }
}
