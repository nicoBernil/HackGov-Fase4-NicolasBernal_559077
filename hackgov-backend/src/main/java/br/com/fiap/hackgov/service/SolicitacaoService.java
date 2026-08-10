package br.com.fiap.hackgov.service;

import br.com.fiap.hackgov.dto.CategoriaResponse;
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

    public SolicitacaoService(SolicitacaoRepository solicitacaoRepository,
                              CategoriaRepository categoriaRepository,
                              StatusSolicitacaoRepository statusRepository,
                              UsuarioRepository usuarioRepository) {
        this.solicitacaoRepository = solicitacaoRepository;
        this.categoriaRepository = categoriaRepository;
        this.statusRepository = statusRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // Lista as categorias para o formulario.
    public List<CategoriaResponse> listarCategorias() {
        return categoriaRepository.findAllByOrderByNome()
                .stream()
                .map(CategoriaResponse::de)
                .toList();
    }

    /**
     * Cria uma nova solicitacao. (US03)
     * - valida categoria e cidadao;
     * - inicia com status "Recebido" e prioridade "MEDIA";
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

        Solicitacao s = new Solicitacao();
        s.setTitulo(dados.titulo());
        s.setDescricao(dados.descricao());
        s.setLogradouro(dados.logradouro());
        s.setBairro(dados.bairro());
        s.setCidade(dados.cidade());
        s.setCategoria(categoria);
        s.setCidadao(cidadao);
        s.setStatus(recebido);
        s.setPrioridade("MEDIA");
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
