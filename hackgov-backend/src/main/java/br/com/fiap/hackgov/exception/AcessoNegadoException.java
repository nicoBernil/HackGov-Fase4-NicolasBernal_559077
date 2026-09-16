package br.com.fiap.hackgov.exception;

/**
 * Lancada pelo controle de acesso (Parte 5) quando o usuario nao esta
 * autenticado ou nao tem o perfil necessario para a operacao, ou quando
 * um cidadao tenta acessar dados de outro cidadao.
 */
public class AcessoNegadoException extends RuntimeException {
    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
