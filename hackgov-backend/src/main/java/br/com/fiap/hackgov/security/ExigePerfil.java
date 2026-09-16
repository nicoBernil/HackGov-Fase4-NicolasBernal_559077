package br.com.fiap.hackgov.security;

import br.com.fiap.hackgov.model.Perfil;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca um metodo de controller que so pode ser executado por usuarios com
 * um dos perfis informados. Quem aplica a regra e o AutorizacaoInterceptor.
 *
 * Exemplo: @ExigePerfil(Perfil.GESTOR) em cima de um metodo que so o
 * gestor publico pode chamar (ex.: alterar o status de uma solicitacao).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExigePerfil {
    Perfil[] value();
}
