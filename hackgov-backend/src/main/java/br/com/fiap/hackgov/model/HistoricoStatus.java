package br.com.fiap.hackgov.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Representa a tabela HISTORICO_STATUS (ja existia no banco, mas ainda nao
 * tinha entidade Java associada).
 *
 * Cada linha registra UMA mudanca de status de UMA solicitacao: qual era o
 * novo status, quem fez a mudanca (ID_GESTOR - tanto gestor quanto o proprio
 * cidadao no registro inicial, ja que ambos sao USUARIO), uma observacao
 * opcional e a data/hora. Este historico e append-only: nunca apagamos nem
 * sobrescrevemos uma linha (ver HistoricoStatusService), o que preserva a
 * trilha de auditoria (Parte 5).
 */
@Entity
@Table(name = "HISTORICO_STATUS")
public class HistoricoStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqHistorico")
    @SequenceGenerator(name = "seqHistorico", sequenceName = "SEQ_HISTORICO", allocationSize = 1)
    @Column(name = "ID_HISTORICO")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ID_SOLICITACAO", nullable = false)
    private Solicitacao solicitacao;

    @ManyToOne
    @JoinColumn(name = "ID_STATUS", nullable = false)
    private StatusSolicitacao status;

    // Coluna no banco chama-se ID_GESTOR, mas referencia USUARIO de forma
    // geral: tanto o gestor quanto o cidadao (no registro inicial, criado
    // automaticamente ao abrir a solicitacao) sao USUARIO.
    @ManyToOne
    @JoinColumn(name = "ID_GESTOR", nullable = false)
    private Usuario ator;

    @Column(name = "OBSERVACAO", length = 500)
    private String observacao;

    @Column(name = "DATA_ALTERACAO", nullable = false)
    private LocalDateTime dataAlteracao;

    public HistoricoStatus() {
    }

    public HistoricoStatus(Solicitacao solicitacao, StatusSolicitacao status, Usuario ator, String observacao) {
        this.solicitacao = solicitacao;
        this.status = status;
        this.ator = ator;
        this.observacao = observacao;
        this.dataAlteracao = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Solicitacao getSolicitacao() {
        return solicitacao;
    }

    public void setSolicitacao(Solicitacao solicitacao) {
        this.solicitacao = solicitacao;
    }

    public StatusSolicitacao getStatus() {
        return status;
    }

    public void setStatus(StatusSolicitacao status) {
        this.status = status;
    }

    public Usuario getAtor() {
        return ator;
    }

    public void setAtor(Usuario ator) {
        this.ator = ator;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public LocalDateTime getDataAlteracao() {
        return dataAlteracao;
    }

    public void setDataAlteracao(LocalDateTime dataAlteracao) {
        this.dataAlteracao = dataAlteracao;
    }
}
