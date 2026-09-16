package br.com.fiap.hackgov.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registra o AutorizacaoInterceptor em todas as rotas /api/**, exceto as
 * que precisam ficar publicas (login, cadastro e a lista de categorias,
 * que e so um combo do formulario e nao expoe dado pessoal nenhum).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AutorizacaoInterceptor autorizacaoInterceptor;

    public WebConfig(AutorizacaoInterceptor autorizacaoInterceptor) {
        this.autorizacaoInterceptor = autorizacaoInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(autorizacaoInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/login", "/api/cadastro", "/api/categorias", "/api/status");
    }
}
