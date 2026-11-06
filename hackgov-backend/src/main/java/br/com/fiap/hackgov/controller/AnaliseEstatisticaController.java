package br.com.fiap.hackgov.controller;

import br.com.fiap.hackgov.dto.AnaliseEstatisticaResponse;
import br.com.fiap.hackgov.model.Perfil;
import br.com.fiap.hackgov.security.ExigePerfil;
import br.com.fiap.hackgov.service.AnaliseEstatisticaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Relatorio estatistico avancado (Visao de Futuro - Parte 3).
 *
 * GET /api/estatisticas/avancadas -> correlacoes, retas de regressao,
 * prazo previsto por prioridade e solicitacoes paradas no fluxo.
 *
 * Restrito ao GESTOR, assim como /api/estatisticas: cruza dados de todos os
 * cidadaos. O AutorizacaoInterceptor aplica a regra (HTTP 403 para os demais).
 */
@RestController
@RequestMapping("/api")
public class AnaliseEstatisticaController {

    private final AnaliseEstatisticaService analiseEstatisticaService;

    public AnaliseEstatisticaController(AnaliseEstatisticaService analiseEstatisticaService) {
        this.analiseEstatisticaService = analiseEstatisticaService;
    }

    @ExigePerfil(Perfil.GESTOR)
    @GetMapping("/estatisticas/avancadas")
    public AnaliseEstatisticaResponse gerarAnaliseAvancada() {
        return analiseEstatisticaService.gerar();
    }
}
