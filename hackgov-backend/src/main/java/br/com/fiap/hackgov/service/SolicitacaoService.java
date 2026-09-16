package br.com.fiap.hackgov.service;

import br.com.fiap.hackgov.dto.AlterarStatusRequest;
import br.com.fiap.hackgov.dto.CategoriaResponse;
import br.com.fiap.hackgov.dto.ClassificacaoIaResponse;
import br.com.fiap.hackgov.dto.EstatisticasResponse;
import br.com.fiap.hackgov.dto.EstatisticasResponse.ItemEstatistica;
import br.com.fiap.hackgov.dto.HistoricoStatusResponse;
import br.com.fiap.hackgov.dto.NovaSolicitacaoRequest;
import br.com.fiap.hackgov.dto.SolicitacaoResponse;
import br.com.fiap.hackgov.dto.StatusResponse;
import br.com.fiap.hackgov.exception.RegraNegocioException;
import br.com.fiap.hackgov.model.Categoria;
import br.com.fiap.hackgov.model.Solicitacao;
import br.com.fiap.hackgov.model.StatusSolicitacao;
import br.com.fiap.hackgov.model.Usuario;
import br.com.fiap.hackgov.repository.CategoriaRepository;
import br.com.fiap.hackgov.repository.SolicitacaoRepository;
import br.com.fiap.hackgov.repository.StatusSolicitacaoRepository;
import br.com.fiap.hackgov.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

/**
 * Regras de negocio das solicitacoes urbanas.
 */
@Service
public class SolicitacaoService {

    private final SolicitacaoRepository solicitacaoRepository;
    private final CategoriaRepository categoriaRepository;
    private final StatusSolicitacaoRepository statusRepository;
    private final UsuarioRepository usuarioRepository;
    private final IaClassificacaoService iaClassificacaoService;
    private final HistoricoStatusService historicoStatusService;

    public SolicitacaoService(SolicitacaoRepository solicitacaoRepository,
                              CategoriaRepository categoriaRepository,
                              StatusSolicitacaoRepository statusRepository,
                              UsuarioRepository usuarioRepository,
                              IaClassificacaoService iaClassificacaoService,
                              HistoricoStatusService historicoStatusService) {
        this.solicitacaoRepository = solicitacaoRepository;
        this.categoriaRepository = categoriaRepository;
        this.statusRepository = statusRepository;
        this.usuarioRepository = usuarioRepository;
        this.iaClassificacaoService = iaClassificacaoService;
        this.historicoStatusService = historicoStatusService;
    }

    // Lista as categorias para o formulario.
    public List<CategoriaResponse> listarCategorias() {
        return categoriaRepository.findAllByOrderByNome()
                .stream()
                .map(CategoriaResponse::de)
                .toList();
    }

    // Lista os status possiveis, na ordem do fluxo (ORDEM), para o <select>
    // do gestor na hora de mudar o status de uma solicitacao.
    public List<StatusResponse> listarStatus() {
        return statusRepository.findAllByOrderByOrdem()
                .stream()
                .map(StatusResponse::de)
                .toList();
    }

    /**
     * Usado pelo front-end enquanto o cidadao digita a descricao, para
     * sugerir categoria e prioridade antes mesmo de enviar o formulario.
     */
    public ClassificacaoIaResponse classificarComIa(String descricao) {
        List<Categoria> categorias = categoriaRepository.findAllByOrderByNome();
        return iaClassificacaoService.classificar(descricao, categorias);
    }

    /**
     * Cria uma nova solicitacao. (US03)
     * - valida categoria e cidadao;
     * - inicia com status "Recebido";
     * - a PRIORIDADE e definida pela IA a partir da descricao;
     * - registra a abertura no HISTORICO_STATUS (rastreabilidade desde o
     *   primeiro momento da solicitacao, nao so nas mudancas feitas pelo gestor);
     * - gera um numero de protocolo unico.
     */
    @Transactional
    public SolicitacaoResponse criar(NovaSolicitacaoRequest dados) {
        Categoria categoria = categoriaRepository.findById(dados.idCategoria())
                .orElseThrow(() -> new RegraNegocioException("Categoria invalida."));

        Usuario cidadao = usuarioRepository.findById(dados.idCidadao())
                .orElseThrow(() -> new RegraNegocioException("Usuario invalido."));

        StatusSolicitacao recebido = statusRepository.findByNome("Recebido")
                .orElseThrow(() -> new RegraNegocioException(
                        "Status 'Recebido' nao encontrado. Rode o dados_iniciais.sql."));

        String prioridade;
        try {
            prioridade = classificarComIa(dados.descricao()).prioridadeSugerida();
        } catch (Exception e) {
            prioridade = "MEDIA";
        }

        Solicitacao s = new Solicitacao();
        s.setTitulo(dados.titulo());
        s.setDescricao(dados.descricao());
        s.setLogradouro(dados.logradouro());
        s.setBairro(dados.bairro());
        s.setCidade(dados.cidade());
        s.setCategoria(categoria);
        s.setCidadao(cidadao);
        s.setStatus(recebido);
        s.setPrioridade(prioridade);
        s.setDataAbertura(LocalDateTime.now());
        s.setProtocolo("TEMP");

        Solicitacao salvo = solicitacaoRepository.save(s);

        String protocolo = "HG" + LocalDateTime.now().getYear()
                + String.format("%05d", salvo.getId());
        salvo.setProtocolo(protocolo);
        salvo = solicitacaoRepository.save(salvo);

        // Primeiro registro do historico: quem abriu foi o proprio cidadao.
        historicoStatusService.registrar(salvo, recebido, cidadao, "Solicitacao registrada pelo cidadao.");

        return SolicitacaoResponse.de(salvo);
    }

    // Lista as solicitacoes de um cidadao especifico. (US05)
    public List<SolicitacaoResponse> listarPorCidadao(Long idCidadao) {
        return solicitacaoRepository.findByCidadaoIdOrderByDataAberturaDesc(idCidadao)
                .stream()
                .map(SolicitacaoResponse::de)
                .toList();
    }

    /**
     * Lista TODAS as solicitacoes para o painel do gestor. (US06)
     *
     * ESTRUTURA DE DADOS: usa uma FILA DE PRIORIDADE (java.util.PriorityQueue)
     * para ordenar as solicitacoes por urgencia antes de exibir no painel -
     * ALTA primeiro, depois MEDIA, depois BAIXA; dentro da mesma prioridade,
     * as mais antigas saem primeiro da fila (FIFO). Isso implementa a US10
     * do backlog: o gestor sempre ve primeiro o que e mais urgente, sem
     * precisar reordenar manualmente.
     */
    public List<SolicitacaoResponse> listarTodas() {
        List<Solicitacao> todas = solicitacaoRepository.findAllByOrderByDataAberturaDesc();

        Comparator<Solicitacao> porPrioridadeEChegada = Comparator
                .comparingInt((Solicitacao s) -> pesoPrioridade(s.getPrioridade()))
                .thenComparing(Solicitacao::getDataAbertura);

        PriorityQueue<Solicitacao> filaDePrioridade = new PriorityQueue<>(porPrioridadeEChegada);
        filaDePrioridade.addAll(todas);

        List<Solicitacao> ordenadasPorPrioridade = new ArrayList<>();
        while (!filaDePrioridade.isEmpty()) {
            ordenadasPorPrioridade.add(filaDePrioridade.poll());
        }

        return ordenadasPorPrioridade.stream()
                .map(SolicitacaoResponse::de)
                .toList();
    }

    private int pesoPrioridade(String prioridade) {
        if (prioridade == null) return 3;
        return switch (prioridade) {
            case "ALTA" -> 0;
            case "MEDIA" -> 1;
            case "BAIXA" -> 2;
            default -> 3;
        };
    }

    /**
     * Altera o status de uma solicitacao (gestor) e registra a mudanca no
     * historico. Se o novo status for "Concluido", marca a data de conclusao.
     */
    @Transactional
    public SolicitacaoResponse alterarStatus(Long idSolicitacao, AlterarStatusRequest dados) {
        Solicitacao solicitacao = solicitacaoRepository.findById(idSolicitacao)
                .orElseThrow(() -> new RegraNegocioException("Solicitacao nao encontrada."));

        StatusSolicitacao novoStatus = statusRepository.findById(dados.idNovoStatus())
                .orElseThrow(() -> new RegraNegocioException("Status invalido."));

        Usuario ator = usuarioRepository.findById(dados.idGestor())
                .orElseThrow(() -> new RegraNegocioException("Usuario responsavel pela alteracao nao encontrado."));

        solicitacao.setStatus(novoStatus);
        if (ehStatusConcluido(novoStatus.getNome())) {
            solicitacao.setDataConclusao(LocalDateTime.now());
        }
        Solicitacao salvo = solicitacaoRepository.save(solicitacao);

        historicoStatusService.registrar(salvo, novoStatus, ator, dados.observacao());

        return SolicitacaoResponse.de(salvo);
    }

    /**
     * Desfaz a ultima alteracao de status, voltando para o status
     * imediatamente anterior (usa a PILHA de historico - ver
     * HistoricoStatusService). Gera um NOVO registro no historico em vez de
     * apagar o anterior, preservando a trilha de auditoria.
     */
    @Transactional
    public SolicitacaoResponse desfazerUltimaAlteracaoStatus(Long idSolicitacao, Long idAtor) {
        StatusSolicitacao statusAnterior = historicoStatusService.statusAnteriorAoUltimo(idSolicitacao);
        AlterarStatusRequest dados = new AlterarStatusRequest(
                statusAnterior.getId(), idAtor, "Revertido automaticamente para o status anterior.");
        return alterarStatus(idSolicitacao, dados);
    }

    // Historico completo de uma solicitacao, para exibir como linha do tempo na tela.
    public List<HistoricoStatusResponse> listarHistorico(Long idSolicitacao) {
        return historicoStatusService.listar(idSolicitacao);
    }

    /**
     * Relatorio estatistico das solicitacoes (Parte 4): quantidade por
     * categoria, por prioridade, por status, e o tempo medio de resolucao
     * (so conta quem ja tem DATA_CONCLUSAO preenchida).
     */
    public EstatisticasResponse gerarEstatisticas() {
        List<Solicitacao> todas = solicitacaoRepository.findAll();

        List<ItemEstatistica> porCategoria = agrupar(todas, s -> s.getCategoria().getNome());
        List<ItemEstatistica> porPrioridade = agrupar(todas, Solicitacao::getPrioridade);
        List<ItemEstatistica> porStatus = agrupar(todas, s -> s.getStatus().getNome());

        List<Solicitacao> concluidas = todas.stream()
                .filter(s -> s.getDataConclusao() != null)
                .toList();

        Double tempoMedioDias = null;
        if (!concluidas.isEmpty()) {
            double somaHoras = concluidas.stream()
                    .mapToDouble(s -> Duration.between(s.getDataAbertura(), s.getDataConclusao()).toMinutes() / 60.0)
                    .sum();
            tempoMedioDias = (somaHoras / concluidas.size()) / 24.0;
        }

        return new EstatisticasResponse(todas.size(), porCategoria, porPrioridade, porStatus, tempoMedioDias);
    }

    // Agrupa e conta, mantendo a ordem em que cada rotulo apareceu primeiro
    // (evita que a ordem do grafico mude sozinha a cada chamada).
    private List<ItemEstatistica> agrupar(List<Solicitacao> lista, java.util.function.Function<Solicitacao, String> chave) {
        Map<String, Long> contagem = lista.stream()
                .collect(Collectors.groupingBy(chave, LinkedHashMap::new, Collectors.counting()));
        return contagem.entrySet().stream()
                .map(e -> new ItemEstatistica(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(ItemEstatistica::total).reversed())
                .toList();
    }

    // Usado pelo controle de acesso (Parte 5): confirma se quem esta pedindo
    // os dados e o proprio cidadao dono da solicitacao, antes de liberar
    // informacoes que podem conter dados pessoais (endereco, historico etc.).
    public boolean pertenceAoCidadao(Long idSolicitacao, Long idCidadao) {
        return solicitacaoRepository.findById(idSolicitacao)
                .map(s -> s.getCidadao().getId().equals(idCidadao))
                .orElse(false);
    }

    private boolean ehStatusConcluido(String nomeStatus) {
        return "Concluido".equalsIgnoreCase(nomeStatus) || "Concluído".equalsIgnoreCase(nomeStatus);
    }
}
