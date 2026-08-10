package br.com.fiap.hackgov.dto;

import br.com.fiap.hackgov.model.Usuario;

/**
 * Dados que a API devolve sobre um usuario.
 *
 * Repare que aqui NAO existe senha nem hash. Nunca devolvemos a senha para a
 * tela. So mandamos o necessario: id, nome, e-mail e perfil (para o front-end
 * saber se deve abrir a area do cidadao ou a do gestor).
 */
public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        String perfil
) {
    // Metodo de conveniencia: transforma um Usuario (do banco) neste response.
    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil().name()
        );
    }
}
