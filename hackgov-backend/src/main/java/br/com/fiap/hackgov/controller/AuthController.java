package br.com.fiap.hackgov.controller;

import br.com.fiap.hackgov.dto.CadastroRequest;
import br.com.fiap.hackgov.dto.LoginRequest;
import br.com.fiap.hackgov.dto.UsuarioResponse;
import br.com.fiap.hackgov.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * A "porta de entrada" da API para autenticacao.
 *
 * @RestController = esta classe responde a chamadas HTTP e devolve JSON.
 * @RequestMapping("/api") = todos os enderecos aqui comecam com /api.
 *
 * @Valid faz o Spring aplicar as regras de validacao dos DTOs automaticamente.
 */
@RestController
@RequestMapping("/api")
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // POST http://localhost:8080/api/cadastro
    @PostMapping("/cadastro")
    public ResponseEntity<UsuarioResponse> cadastrar(@Valid @RequestBody CadastroRequest dados) {
        UsuarioResponse criado = usuarioService.cadastrar(dados);
        // 201 = "Created" (recurso criado com sucesso)
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    // POST http://localhost:8080/api/login
    @PostMapping("/login")
    public ResponseEntity<UsuarioResponse> login(@Valid @RequestBody LoginRequest dados) {
        UsuarioResponse usuario = usuarioService.login(dados);
        // 200 = "OK"
        return ResponseEntity.ok(usuario);
    }
}
