package br.com.fiap.hackgov.model;

import jakarta.persistence.*;

/**
 * Representa a tabela STATUS_SOLICITACAO (etapas do atendimento).
 * Ex.: Recebido, Em Analise, Em Andamento, Concluido.
 */
@Entity
@Table(name = "STATUS_SOLICITACAO")
public class StatusSolicitacao {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqStatus")
    @SequenceGenerator(name = "seqStatus", sequenceName = "SEQ_STATUS", allocationSize = 1)
    @Column(name = "ID_STATUS")
    private Long id;

    @Column(name = "NOME", nullable = false, length = 40)
    private String nome;

    @Column(name = "ORDEM", nullable = false)
    private Integer ordem;

    public StatusSolicitacao() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Integer getOrdem() {
        return ordem;
    }

    public void setOrdem(Integer ordem) {
        this.ordem = ordem;
    }
}
