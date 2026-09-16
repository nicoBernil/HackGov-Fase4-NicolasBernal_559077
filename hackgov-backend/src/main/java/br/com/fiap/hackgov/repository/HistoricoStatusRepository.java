package br.com.fiap.hackgov.repository;

import br.com.fiap.hackgov.model.HistoricoStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoricoStatusRepository extends JpaRepository<HistoricoStatus, Long> {

    // Historico de UMA solicitacao, do mais antigo para o mais recente.
    // Essa ordem cronologica e a base para montar a PILHA em HistoricoStatusService.
    List<HistoricoStatus> findBySolicitacaoIdOrderByDataAlteracaoAsc(Long idSolicitacao);
}
