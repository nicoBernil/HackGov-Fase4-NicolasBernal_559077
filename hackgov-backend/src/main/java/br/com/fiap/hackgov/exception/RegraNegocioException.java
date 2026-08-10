package br.com.fiap.hackgov.exception;

/**
 * Excecao usada quando uma regra de negocio e violada.
 * Ex.: tentar cadastrar um e-mail que ja existe, ou senha errada no login.
 *
 * E uma "RuntimeException" simples: quando lancada no servico, o
 * GlobalExceptionHandler captura e devolve uma mensagem amigavel ao usuario.
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
