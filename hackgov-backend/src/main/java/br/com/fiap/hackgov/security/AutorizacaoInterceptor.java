package br.com.fiap.hackgov.security;

import br.com.fiap.hackgov.exception.AcessoNegadoException;
import br.com.fiap.hackgov.model.Usuario;
import br.com.fiap.hackgov.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * CONTROLE DE ACESSO (Parte 5 - governanca).
 *
 * Este projeto nao usa o Spring Security completo (so o BCrypt, ver
 * SegurancaConfig), entao a identificacao de quem esta chamando a API e
 * feita por um cabecalho simples: X-Usuario-Id, enviado pelo front-end em
 * toda chamada depois do login (ver hackgovApi.js -> definirSessao()).
 *
 * O que este interceptor faz em toda requisicao para /api/** (exceto as
 * rotas publicas configuradas em WebConfig):
 *   1. Exige o cabecalho X-Usuario-Id e confirma que o usuario existe.
 *   2. Disponibiliza o usuario autenticado como atributo da requisicao
 *      ("usuarioAutenticado"), para os controllers fazerem verificacoes
 *      extras (ex.: um cidadao so pode ver as PROPRIAS solicitacoes).
 *   3. Se o metodo do controller tiver @ExigePerfil(...), confirma que o
 *      perfil do usuario autenticado esta na lista permitida - por
 *      exemplo, so o GESTOR pode listar todas as solicitacoes ou mudar
 *      o status de uma solicitacao.
 *
 * Qualquer falha aqui lanca AcessoNegadoException, que o
 * GlobalExceptionHandler converte em HTTP 403.
 */
@Component
public class AutorizacaoInterceptor implements HandlerInterceptor {

    private final UsuarioRepository usuarioRepository;

    public AutorizacaoInterceptor(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // So se aplica a metodos de controller (ignora recursos estaticos etc.).
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        String cabecalhoId = request.getHeader("X-Usuario-Id");
        if (cabecalhoId == null || cabecalhoId.isBlank()) {
            throw new AcessoNegadoException("Autenticacao necessaria. Faca login novamente.");
        }

        Long idUsuario;
        try {
            idUsuario = Long.valueOf(cabecalhoId.trim());
        } catch (NumberFormatException e) {
            throw new AcessoNegadoException("Autenticacao invalida.");
        }

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new AcessoNegadoException("Usuario nao encontrado. Faca login novamente."));

        request.setAttribute("usuarioAutenticado", usuario);

        ExigePerfil exigePerfil = handlerMethod.getMethodAnnotation(ExigePerfil.class);
        if (exigePerfil != null) {
            boolean temPermissao = Arrays.asList(exigePerfil.value()).contains(usuario.getPerfil());
            if (!temPermissao) {
                throw new AcessoNegadoException(
                        "Esta operacao e restrita ao(s) perfil(is): " + Arrays.toString(exigePerfil.value()));
            }
        }

        return true;
    }

    /** Helper para os controllers lerem o usuario autenticado sem repetir o cast. */
    public static Usuario usuarioAutenticado(HttpServletRequest request) {
        Object usuario = request.getAttribute("usuarioAutenticado");
        if (usuario == null) {
            throw new AcessoNegadoException("Autenticacao necessaria. Faca login novamente.");
        }
        return (Usuario) usuario;
    }
}
