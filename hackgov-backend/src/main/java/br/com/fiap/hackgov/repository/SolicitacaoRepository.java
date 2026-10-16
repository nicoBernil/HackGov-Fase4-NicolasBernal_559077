package br.com.fiap.hackgov.repository;

import br.com.fiap.hackgov.model.Solicitacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolicitacaoRepository extends JpaRepository<Solicitacao, Long> {

    // Lista as solicitacoes de UM cidadao, da mais nova para a mais antiga.
    List<Solicitacao> findByCidadaoIdOrderByDataAberturaDesc(Long cidadaoId);

    // Lista TODAS as solicitacoes (para o painel do gestor), da mais nova p/ antiga.
    List<Solicitacao> findAllByOrderByDataAberturaDesc();

    // Solicitacoes ainda em aberto (sem data de conclusao). Usado pela
    // deteccao de duplicadas: so faz sentido avisar sobre o que ainda nao foi resolvido.
    List<Solicitacao> findByDataConclusaoIsNull();
}
