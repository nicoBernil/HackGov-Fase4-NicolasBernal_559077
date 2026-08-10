package br.com.fiap.hackgov.repository;

import br.com.fiap.hackgov.model.StatusSolicitacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StatusSolicitacaoRepository extends JpaRepository<StatusSolicitacao, Long> {

    // Busca um status pelo nome (ex.: "Recebido") para usar ao criar a solicitacao.
    Optional<StatusSolicitacao> findByNome(String nome);
}
