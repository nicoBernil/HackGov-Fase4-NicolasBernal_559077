package br.com.fiap.hackgov.service;

import br.com.fiap.hackgov.dto.CategoriaResponse;
import br.com.fiap.hackgov.dto.ClassificacaoIaResponse;
import br.com.fiap.hackgov.dto.NovaSolicitacaoRequest;
import br.com.fiap.hackgov.dto.SolicitacaoResponse;
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

import java.time.LocalDateTime;
import java.util.List;

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

    public SolicitacaoService(SolicitacaoRepository solicitacaoRepository,
                              CategoriaRepository categoriaRepository,
                              StatusSolicitacaoRepository statusRepository,
                              UsuarioRepository usuarioRepository,
                              IaClassificacaoService iaClassificacaoService) {
        this.solicitacaoRepository = solicitacaoRepository;
        this.categoriaRepository = categoriaRepository;
        this.statusRepository = statusRepository;
        this.usuarioRepository = usuarioRepository;
        this.iaClassificacaoService = iaClassificacaoService;
    }

    // Lista as categorias para o formulario.
    public List<CategoriaResponse> listarCategorias() {
        return categoriaRepository.findAllByOrderByNome()
                .stream()
                .map(CategoriaResponse::de)
                .toList();
    }

    /**
     * NOVO: usado pelo front-end enquanto o cidadao digita a descricao, para
     * sugerir categoria e prioridade antes mesmo de enviar o formulario. O
     * cidadao ainda pode corrigir a categoria manualmente no <select>.
     */
    public ClassificacaoIaResponse classificarComIa(String descricao) {
        List<Categoria> categorias = categoriaRepository.findAllByOrderByNome();
        return iaClassificacaoService.classificar(descricao, categorias);
    }

    /**
     * Cria uma nova solicitacao. (US03)
     * - valida categoria e cidadao;
     * - inicia com status "Recebido";
     * - a PRIORIDADE agora e definida pela IA a partir da descricao (antes
     *   era sempre fixa em "MEDIA"), com um Plano B por palavras-chave caso a
     *   IA generativa esteja indisponivel - a criacao da solicitacao nunca
     *   falha por causa disso;
     * - gera um numero de protocolo unico.
     *
     * @Transactional garante que tudo aconteca junto: se algo falhar no meio,
     * nada e gravado pela metade.
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

        // A prioridade e sugerida pela IA a partir do texto da descricao.
        // Se, por qualquer motivo, a classificacao falhar (rede fora do ar,
        // resposta inesperada etc.), caimos em "MEDIA" - a solicitacao do
        // cidadao NUNCA deixa de ser criada por causa da IA.
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
        // O protocolo definitivo precisa do ID, que so existe apos salvar.
        // Por isso colocamos um valor temporario aqui e atualizamos depois.
        s.setProtocolo("TEMP");

        // 1) Salva para gerar o ID.
        Solicitacao salvo = solicitacaoRepository.save(s);

        // 2) Monta o protocolo com o ano + o ID e salva de novo.
        String protocolo = "HG" + LocalDateTime.now().getYear()
                + String.format("%05d", salvo.getId());
        salvo.setProtocolo(protocolo);

        return SolicitacaoResponse.de(solicitacaoRepository.save(salvo));
    }

    // Lista as solicitacoes de um cidadao especifico. (US05)
    public List<SolicitacaoResponse> listarPorCidadao(Long idCidadao) {
        return solicitacaoRepository.findByCidadaoIdOrderByDataAberturaDesc(idCidadao)
                .stream()
                .map(SolicitacaoResponse::de)
                .toList();
    }

    // Lista TODAS as solicitacoes para o painel do gestor. (US06)
    public List<SolicitacaoResponse> listarTodas() {
        return solicitacaoRepository.findAllByOrderByDataAberturaDesc()
                .stream()
                .map(SolicitacaoResponse::de)
                .toList();
    }
}
