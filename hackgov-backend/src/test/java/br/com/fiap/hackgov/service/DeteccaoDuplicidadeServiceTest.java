package br.com.fiap.hackgov.service;

import br.com.fiap.hackgov.dto.SolicitacaoSimilarResponse;
import br.com.fiap.hackgov.dto.VerificarDuplicidadeRequest;
import br.com.fiap.hackgov.model.Categoria;
import br.com.fiap.hackgov.model.Solicitacao;
import br.com.fiap.hackgov.model.StatusSolicitacao;
import br.com.fiap.hackgov.repository.SolicitacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testes da deteccao de duplicadas. Nao precisam do Oracle: o repositorio e
 * substituido por um "mock" (Mockito) que devolve solicitacoes de exemplo.
 *
 * Para rodar: na pasta hackgov-backend, execute  mvn test
 */
class DeteccaoDuplicidadeServiceTest {

    private static final Long BURACO = 1L;
    private static final Long ILUMINACAO = 2L;
    private static final Long LIXO = 3L;

    private SolicitacaoRepository repositorio;
    private DeteccaoDuplicidadeService servico;

    @BeforeEach
    void preparar() {
        repositorio = mock(SolicitacaoRepository.class);
        servico = new DeteccaoDuplicidadeService(repositorio);

        when(repositorio.findByDataConclusaoIsNull()).thenReturn(List.of(
                solicitacao(10L, "HG202600010", "Buraco enorme na rua",
                        "Tem um buraco grande no asfalto, carros estao desviando e ja vi moto cair.",
                        "Rua das Flores, 120", "Centro", "Rio de Janeiro", BURACO),
                solicitacao(11L, "HG202600011", "Poste apagado",
                        "O poste da esquina esta sem luz faz uma semana, a rua fica escura a noite.",
                        "Rua das Flores, 300", "Centro", "Rio de Janeiro", ILUMINACAO),
                solicitacao(12L, "HG202600012", "Lixo acumulado",
                        "Lixo acumulado na calcada ha dias, cheiro forte e ratos.",
                        "Av. Brasil, 50", "Centro", "Rio de Janeiro", LIXO),
                // Mesmo problema, mas em OUTRO bairro: nao pode aparecer.
                solicitacao(13L, "HG202600013", "Buraco na rua",
                        "Buraco grande no asfalto da rua.",
                        "Rua das Flores, 120", "Tijuca", "Rio de Janeiro", BURACO)
        ));
    }

    @Test
    void encontraADuplicataMesmoComPalavrasDiferentes() {
        var nova = new VerificarDuplicidadeRequest(
                "Cratera no asfalto",
                "Buracos no asfalto da Rua das Flores, perto do numero 120. Perigoso para motos.",
                "R. das Flores 118", "centro", "rio de janeiro", BURACO);

        List<SolicitacaoSimilarResponse> similares = servico.buscarSimilares(nova);

        assertEquals("HG202600010", similares.get(0).protocolo(), "a mais parecida deve vir primeiro");
        assertTrue(similares.get(0).similaridadePercentual() >= 50);
        assertTrue(similares.stream().noneMatch(s -> s.protocolo().equals("HG202600013")),
                "solicitacao de outro bairro nao conta como duplicata");
    }

    @Test
    void naoAvisaQuandoOProblemaEDiferente() {
        var nova = new VerificarDuplicidadeRequest(
                "Arvore caida",
                "Uma arvore caiu na praca depois da chuva e bloqueou a passagem de pedestres.",
                "Praca XV", "Centro", "Rio de Janeiro", 5L);

        assertTrue(servico.buscarSimilares(nova).isEmpty());
    }

    @Test
    void mesmoTipoDeProblemaEmOutraRuaNaoEDuplicata() {
        var nova = new VerificarDuplicidadeRequest(
                "Buraco", "Buraco na rua perto da padaria",
                "Rua Sete de Setembro", "Centro", "Rio de Janeiro", BURACO);

        assertTrue(servico.buscarSimilares(nova).isEmpty());
    }

    @Test
    void naoProcuraEmOutroBairro() {
        var nova = new VerificarDuplicidadeRequest(
                "Poste apagado", "O poste da esquina esta sem luz faz uma semana.",
                "Rua das Flores, 300", "Copacabana", "Rio de Janeiro", ILUMINACAO);

        assertTrue(servico.buscarSimilares(nova).isEmpty());
    }

    @Test
    void normalizaAcentosPluralEStopwords() {
        assertEquals("iluminacao publica", DeteccaoDuplicidadeService.normalizar("Iluminação Pública!"));
        assertEquals(List.of("buraco", "asfalto"), DeteccaoDuplicidadeService.tokenizar("Buracos no asfalto"));
        assertEquals(Set.of("flores"), DeteccaoDuplicidadeService.tokensEndereco("Rua das Flores, 120"));
    }

    // ---------------- montagem dos dados de exemplo ----------------

    private static Solicitacao solicitacao(Long id, String protocolo, String titulo, String descricao,
                                           String logradouro, String bairro, String cidade, Long idCategoria) {
        Categoria categoria = new Categoria();
        categoria.setId(idCategoria);
        categoria.setNome("Categoria " + idCategoria);

        StatusSolicitacao status = new StatusSolicitacao();
        status.setId(1L);
        status.setNome("Recebido");

        Solicitacao s = new Solicitacao();
        s.setId(id);
        s.setProtocolo(protocolo);
        s.setTitulo(titulo);
        s.setDescricao(descricao);
        s.setLogradouro(logradouro);
        s.setBairro(bairro);
        s.setCidade(cidade);
        s.setCategoria(categoria);
        s.setStatus(status);
        s.setPrioridade("MEDIA");
        s.setDataAbertura(LocalDateTime.of(2026, 10, 1, 9, 0));
        return s;
    }
}
