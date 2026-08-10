package br.com.fiap.hackgov.controller;

import br.com.fiap.hackgov.dto.CategoriaResponse;
import br.com.fiap.hackgov.dto.NovaSolicitacaoRequest;
import br.com.fiap.hackgov.dto.SolicitacaoResponse;
import br.com.fiap.hackgov.service.SolicitacaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints das solicitacoes urbanas e das categorias.
 */
@RestController
@RequestMapping("/api")
public class SolicitacaoController {

    private final SolicitacaoService solicitacaoService;

    public SolicitacaoController(SolicitacaoService solicitacaoService) {
        this.solicitacaoService = solicitacaoService;
    }

    // GET /api/categorias  -> lista as categorias (para o formulario)
    @GetMapping("/categorias")
    public List<CategoriaResponse> listarCategorias() {
        return solicitacaoService.listarCategorias();
    }

    // POST /api/solicitacoes  -> cria uma nova solicitacao
    @PostMapping("/solicitacoes")
    public ResponseEntity<SolicitacaoResponse> criar(@Valid @RequestBody NovaSolicitacaoRequest dados) {
        SolicitacaoResponse criada = solicitacaoService.criar(dados);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    // GET /api/solicitacoes  -> TODAS as solicitacoes (painel do gestor)
    @GetMapping("/solicitacoes")
    public List<SolicitacaoResponse> listarTodas() {
        return solicitacaoService.listarTodas();
    }

    // GET /api/solicitacoes/cidadao/{id}  -> solicitacoes de um cidadao
    @GetMapping("/solicitacoes/cidadao/{id}")
    public List<SolicitacaoResponse> listarPorCidadao(@PathVariable Long id) {
        return solicitacaoService.listarPorCidadao(id);
    }
}
