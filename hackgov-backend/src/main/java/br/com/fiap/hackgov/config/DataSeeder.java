package br.com.fiap.hackgov.config;

import br.com.fiap.hackgov.model.Perfil;
import br.com.fiap.hackgov.model.Usuario;
import br.com.fiap.hackgov.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Roda automaticamente toda vez que a aplicacao sobe.
 *
 * Aqui criamos um GESTOR de exemplo (caso ainda nao exista), ja com a senha
 * criptografada. Assim voce consegue testar o login de gestor sem precisar
 * inserir hash na mao no banco.
 *
 *   Login do gestor de exemplo:
 *     e-mail: gestor@hackgov.gov.br
 *     senha : gestor123
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        String emailGestor = "gestor@hackgov.gov.br";

        if (!usuarioRepository.existsByEmail(emailGestor)) {
            Usuario gestor = new Usuario(
                    "Gestor Exemplo",
                    "00000000000",
                    emailGestor,
                    passwordEncoder.encode("gestor123"),
                    Perfil.GESTOR
            );
            usuarioRepository.save(gestor);
            System.out.println(">>> Gestor de exemplo criado: " + emailGestor + " / senha: gestor123");
        }
    }
}
