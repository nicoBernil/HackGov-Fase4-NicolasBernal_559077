package br.com.fiap.hackgov.service;

import br.com.fiap.hackgov.dto.HistoricoStatusResponse;
import br.com.fiap.hackgov.exception.RegraNegocioException;
import br.com.fiap.hackgov.model.HistoricoStatus;
import br.com.fiap.hackgov.model.Solicitacao;
import br.com.fiap.hackgov.model.StatusSolicitacao;
import br.com.fiap.hackgov.model.Usuario;
import br.com.fiap.hackgov.repository.HistoricoStatusRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Responsavel pelo historico de alteracoes de status de uma solicitacao.
 *
 * ESTRUTURA DE DADOS: o historico e tratado como uma PILHA (LIFO - Deque
 * usado como Stack). Cada alteracao de status e empilhada na ordem em que
 * aconteceu; o TOPO da pilha e sempre a alteracao mais recente. Para
 * descobrir qual era o status IMEDIATAMENTE ANTERIOR ao atual (necessario
 * para a funcionalidade de "desfazer"), desempilhamos o topo (a alteracao
 * atual) e olhamos o que sobrou no topo - por definicao da pilha, isso e o
 * estado anterior.
 *
 * Importante: "desfazer" nunca apaga nem sobrescreve uma linha do historico
 * (isso violaria a trilha de auditoria da Parte 5). Ele apenas EMPILHA um
 * novo registro apontando de volta para o status anterior. O historico
 * completo de tudo o que aconteceu continua intacto e consultavel.
 */
@Service
public class HistoricoStatusService {

    private final HistoricoStatusRepository historicoRepository;

    public HistoricoStatusService(HistoricoStatusRepository historicoRepository) {
        this.historicoRepository = historicoRepository;
    }

    public HistoricoStatus registrar(Solicitacao solicitacao, StatusSolicitacao status, Usuario ator, String observacao) {
        HistoricoStatus registro = new HistoricoStatus(solicitacao, status, ator, observacao);
        return historicoRepository.save(registro);
    }

    // Historico completo de uma solicitacao, do mais antigo ao mais recente,
    // pronto para exibir na tela (linha do tempo).
    public List<HistoricoStatusResponse> listar(Long idSolicitacao) {
        return historicoRepository.findBySolicitacaoIdOrderByDataAlteracaoAsc(idSolicitacao)
                .stream()
                .map(HistoricoStatusResponse::de)
                .toList();
    }

    /**
     * Monta a pilha de historico e desempilha o topo para descobrir o status
     * imediatamente anterior ao atual. Usado pelo "desfazer ultima alteracao".
     */
    public StatusSolicitacao statusAnteriorAoUltimo(Long idSolicitacao) {
        List<HistoricoStatus> emOrdemCronologica = historicoRepository
                .findBySolicitacaoIdOrderByDataAlteracaoAsc(idSolicitacao);

        if (emOrdemCronologica.isEmpty()) {
            throw new RegraNegocioException("Esta solicitacao ainda nao tem historico de status.");
        }

        // Empilha na ordem em que aconteceram: o ultimo "push" (a alteracao
        // mais recente) fica no topo da pilha.
        Deque<HistoricoStatus> pilha = new ArrayDeque<>();
        for (HistoricoStatus registro : emOrdemCronologica) {
            pilha.push(registro);
        }

        pilha.pop(); // remove o topo (a alteracao atual) - so na pilha em memoria, nada e apagado do banco

        if (pilha.isEmpty()) {
            throw new RegraNegocioException(
                    "Nao ha uma alteracao anterior para desfazer - este e o primeiro status registrado.");
        }

        return pilha.peek().getStatus(); // o novo topo e o status imediatamente anterior
    }
}
