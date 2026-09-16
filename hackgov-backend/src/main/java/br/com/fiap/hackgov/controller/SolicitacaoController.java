package br.com.fiap.hackgov.controller;

import br.com.fiap.hackgov.dto.AlterarStatusRequest;
import br.com.fiap.hackgov.dto.CategoriaResponse;
import br.com.fiap.hackgov.dto.ClassificacaoIaResponse;
import br.com.fiap.hackgov.dto.DescricaoRequest;
import br.com.fiap.hackgov.dto.HistoricoStatusResponse;
import br.com.fiap.hackgov.dto.NovaSolicitacaoRequest;
import br.com.fiap.hackgov.dto.EstatisticasResponse;
import br.com.fiap.hackgov.dto.SolicitacaoResponse;
import br.com.fiap.hackgov.dto.StatusResponse;
import br.com.fiap.hackgov.exception.AcessoNegadoException;
import br.com.fiap.hackgov.model.Perfil;
import br.com.fiap.hackgov.model.Usuario;
import br.com.fiap.hackgov.security.AutorizacaoInterceptor;
import br.com.fiap.hackgov.security.ExigePerfil;
import br.com.fiap.hackgov.service.SolicitacaoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints das solicitacoes urbanas e das categorias.
 *
 * CONTROLE DE ACESSO (Parte 5): toda rota aqui, exceto /categorias, passa
 * pelo AutorizacaoInterceptor (ver pacote security) e exige o cabecalho
 * X-Usuario-Id. Rotas marcadas com @ExigePerfil(Perfil.GESTOR) so podem
 * ser chamadas por um usuario com perfil GESTOR; as demais fazem uma
 * checagem manual de "dono do dado" (um cidadao so pode ver as PROPRIAS
 * solicitacoes e o PROPRIO historico).
 */
@RestController
@RequestMapping("/api")
public class SolicitacaoController {

    private final SolicitacaoService solicitacaoService;

    public SolicitacaoController(SolicitacaoService solicitacaoService) {
        this.solicitacaoService = solicitacaoService;
    }

    // GET /api/categorias  -> lista as categorias (para o formulario). Publica:
    // nao expoe nenhum dado pessoal, so os nomes das categorias.
    @GetMapping("/categorias")
    public List<CategoriaResponse> listarCategorias() {
        return solicitacaoService.listarCategorias();
    }

    // GET /api/status  -> lista os status possiveis (para o <select> do
    // gestor mudar o status). Publica: nao expoe dado pessoal, so nomes de status.
    @GetMapping("/status")
    public List<StatusResponse> listarStatus() {
        return solicitacaoService.listarStatus();
    }

    // POST /api/solicitacoes/classificar-ia -> sugestao de categoria e
    // prioridade via IA, a partir da descricao digitada. O front-end chama
    // isto enquanto o cidadao preenche o formulario, ANTES de enviar.
    @PostMapping("/solicitacoes/classificar-ia")
    public ResponseEntity<ClassificacaoIaResponse> classificarComIa(@Valid @RequestBody DescricaoRequest dados) {
        return ResponseEntity.ok(solicitacaoService.classificarComIa(dados.descricao()));
    }

    // POST /api/solicitacoes  -> cria uma nova solicitacao.
    // Controle de acesso: o cidadao so pode abrir solicitacao em nome dele
    // mesmo (evita que alguem registre reclamacoes usando o ID de outra pessoa).
    @PostMapping("/solicitacoes")
    public ResponseEntity<SolicitacaoResponse> criar(@Valid @RequestBody NovaSolicitacaoRequest dados,
                                                      HttpServletRequest request) {
        Usuario usuarioAutenticado = AutorizacaoInterceptor.usuarioAutenticado(request);
        if (!usuarioAutenticado.getId().equals(dados.idCidadao())) {
            throw new AcessoNegadoException("Voce so pode registrar solicitacoes em seu proprio nome.");
        }
        SolicitacaoResponse criada = solicitacaoService.criar(dados);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    // GET /api/solicitacoes  -> TODAS as solicitacoes (painel do gestor).
    // Restrita ao perfil GESTOR: e o unico perfil que pode ver solicitacoes
    // de TODOS os cidadaos ao mesmo tempo.
    @ExigePerfil(Perfil.GESTOR)
    @GetMapping("/solicitacoes")
    public List<SolicitacaoResponse> listarTodas() {
        return solicitacaoService.listarTodas();
    }

    // GET /api/solicitacoes/cidadao/{id}  -> solicitacoes de um cidadao.
    // Controle de acesso: o GESTOR pode ver de qualquer cidadao; um CIDADAO
    // so pode ver as suas proprias (nunca as de outro cidadao).
    @GetMapping("/solicitacoes/cidadao/{id}")
    public List<SolicitacaoResponse> listarPorCidadao(@PathVariable Long id, HttpServletRequest request) {
        Usuario usuarioAutenticado = AutorizacaoInterceptor.usuarioAutenticado(request);
        boolean ehGestor = usuarioAutenticado.getPerfil() == Perfil.GESTOR;
        boolean ehDonoDosDados = usuarioAutenticado.getId().equals(id);
        if (!ehGestor && !ehDonoDosDados) {
            throw new AcessoNegadoException("Voce so pode ver as suas proprias solicitacoes.");
        }
        return solicitacaoService.listarPorCidadao(id);
    }

    // GET /api/estatisticas  -> relatorio (Parte 4): contagem por categoria,
    // prioridade, status, e tempo medio de resolucao. Restrita ao GESTOR,
    // pois cruza dados de TODOS os cidadaos ao mesmo tempo.
    @ExigePerfil(Perfil.GESTOR)
    @GetMapping("/estatisticas")
    public EstatisticasResponse gerarEstatisticas() {
        return solicitacaoService.gerarEstatisticas();
    }

    // PATCH /api/solicitacoes/{id}/status  -> gestor altera o status
    // (gera um novo registro no historico/auditoria - Parte 5). Restrita ao GESTOR.
    @ExigePerfil(Perfil.GESTOR)
    @PatchMapping("/solicitacoes/{id}/status")
    public ResponseEntity<SolicitacaoResponse> alterarStatus(@PathVariable Long id,
                                                              @Valid @RequestBody AlterarStatusRequest dados) {
        return ResponseEntity.ok(solicitacaoService.alterarStatus(id, dados));
    }

    // POST /api/solicitacoes/{id}/status/desfazer?idGestor=1
    // -> volta para o status imediatamente anterior (usa a PILHA de historico).
    // Nao apaga nada: cria um NOVO registro apontando para o status anterior.
    // Restrita ao GESTOR, assim como a alteracao de status em si.
    @ExigePerfil(Perfil.GESTOR)
    @PostMapping("/solicitacoes/{id}/status/desfazer")
    public ResponseEntity<SolicitacaoResponse> desfazerUltimaAlteracaoStatus(@PathVariable Long id,
                                                                              @RequestParam Long idGestor) {
        return ResponseEntity.ok(solicitacaoService.desfazerUltimaAlteracaoStatus(id, idGestor));
    }

    // GET /api/solicitacoes/{id}/historico  -> linha do tempo completa de status
    // (trilha de auditoria da solicitacao, do primeiro registro ao mais recente).
    // Controle de acesso: GESTOR ve qualquer historico; CIDADAO so o da propria solicitacao.
    @GetMapping("/solicitacoes/{id}/historico")
    public List<HistoricoStatusResponse> listarHistorico(@PathVariable Long id, HttpServletRequest request) {
        Usuario usuarioAutenticado = AutorizacaoInterceptor.usuarioAutenticado(request);
        boolean ehGestor = usuarioAutenticado.getPerfil() == Perfil.GESTOR;
        boolean ehDonoDaSolicitacao = solicitacaoService.pertenceAoCidadao(id, usuarioAutenticado.getId());
        if (!ehGestor && !ehDonoDaSolicitacao) {
            throw new AcessoNegadoException("Voce so pode ver o historico das suas proprias solicitacoes.");
        }
        return solicitacaoService.listarHistorico(id);
    }
}
