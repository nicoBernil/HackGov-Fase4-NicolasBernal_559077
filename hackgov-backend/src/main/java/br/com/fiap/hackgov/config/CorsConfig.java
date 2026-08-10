package br.com.fiap.hackgov.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Libera o CORS para o front-end em React.
 *
 * Por seguranca, o navegador bloqueia chamadas de um site (ex.: o React em
 * localhost:5173) para outro endereco (ex.: a API em localhost:8080), a menos
 * que a API autorize. Aqui autorizamos os enderecos tipicos do React em
 * desenvolvimento (Vite usa 5173; Create React App usa 3000).
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173", "http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
