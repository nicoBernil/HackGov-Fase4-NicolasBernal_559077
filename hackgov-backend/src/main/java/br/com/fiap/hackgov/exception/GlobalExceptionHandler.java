package br.com.fiap.hackgov.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * "Porteiro de erros" da aplicacao.
 *
 * Em vez de devolver uma pagina de erro feia, capturamos as excecoes e
 * devolvemos um JSON simples com a mensagem, assim o front-end consegue
 * mostrar um alerta amigavel para o usuario.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Erros de regra de negocio (ex.: e-mail ja cadastrado, senha errada).
    // Devolve status 400 (Bad Request) com a mensagem.
    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<Map<String, String>> tratarRegraNegocio(RegraNegocioException ex) {
        Map<String, String> corpo = new HashMap<>();
        corpo.put("erro", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(corpo);
    }

    // Erros de validacao dos formularios (campos obrigatorios, e-mail invalido...).
    // Junta o nome do campo com a mensagem de erro de cada um.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> tratarValidacao(MethodArgumentNotValidException ex) {
        Map<String, String> erros = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(campo ->
                erros.put(campo.getField(), campo.getDefaultMessage())
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erros);
    }
}
