package br.com.fiap.hackgov.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuracao de objetos compartilhados pela aplicacao.
 *
 * Aqui criamos um "PasswordEncoder" usando BCrypt. Ele faz duas coisas:
 *   - encode(senha)  -> gera o hash para salvar no banco
 *   - matches(senha, hash) -> confere se a senha digitada bate com o hash
 *
 * BCrypt e seguro porque o mesmo texto gera hashes diferentes a cada vez e e
 * proposital lento, dificultando ataques de forca bruta.
 */
@Configuration
public class SegurancaConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
