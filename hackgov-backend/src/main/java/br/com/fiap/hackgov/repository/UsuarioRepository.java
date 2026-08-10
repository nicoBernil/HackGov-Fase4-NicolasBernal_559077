package br.com.fiap.hackgov.repository;

import br.com.fiap.hackgov.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * O Repository e quem conversa com a tabela USUARIO.
 *
 * O mais legal: nos NAO escrevemos SQL aqui. Por herdar de JpaRepository,
 * ja ganhamos de graca metodos como save(), findById(), findAll()...
 *
 * E ao declarar "findByEmail" e "existsByEmail", o Spring entende pelo NOME do
 * metodo qual consulta gerar. Isso se chama "query method".
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Busca um usuario pelo e-mail (usado no login).
    Optional<Usuario> findByEmail(String email);

    // Verifica se ja existe alguem com este e-mail (usado no cadastro).
    boolean existsByEmail(String email);

    // Verifica se ja existe alguem com este CPF (usado no cadastro).
    boolean existsByCpf(String cpf);
}
