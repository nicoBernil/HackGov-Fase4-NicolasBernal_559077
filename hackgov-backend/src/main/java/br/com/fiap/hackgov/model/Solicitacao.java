package br.com.fiap.hackgov.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Representa a tabela SOLICITACAO (a demanda urbana do cidadao).
 *
 * Repare nos @ManyToOne: cada solicitacao aponta para UMA categoria, UM status
 * e UM cidadao (o usuario que abriu). Isso espelha as chaves estrangeiras (FK)
 * que criamos no banco.
 */
@Entity
@Table(name = "SOLICITACAO")
public class Solicitacao {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqSolicitacao")
    @SequenceGenerator(name = "seqSolicitacao", sequenceName = "SEQ_SOLICITACAO", allocationSize = 1)
    @Column(name = "ID_SOLICITACAO")
    private Long id;

    @Column(name = "PROTOCOLO", nullable = false, unique = true, length = 20)
    private String protocolo;

    @Column(name = "TITULO", nullable = false, length = 120)
    private String titulo;

    @Column(name = "DESCRICAO", nullable = false, length = 1000)
    private String descricao;

    @Column(name = "LOGRADOURO", nullable = false, length = 150)
    private String logradouro;

    @Column(name = "BAIRRO", nullable = false, length = 100)
    private String bairro;

    @Column(name = "CIDADE", nullable = false, length = 100)
    private String cidade;

    @Column(name = "PRIORIDADE", nullable = false, length = 10)
    private String prioridade;

    // Ligacoes (chaves estrangeiras)
    @ManyToOne
    @JoinColumn(name = "ID_CATEGORIA", nullable = false)
    private Categoria categoria;

    @ManyToOne
    @JoinColumn(name = "ID_STATUS", nullable = false)
    private StatusSolicitacao status;

    @ManyToOne
    @JoinColumn(name = "ID_CIDADAO", nullable = false)
    private Usuario cidadao;

    @Column(name = "DATA_ABERTURA", nullable = false)
    private LocalDateTime dataAbertura;

    @Column(name = "DATA_CONCLUSAO")
    private LocalDateTime dataConclusao;

    public Solicitacao() {
    }

    // ------------------- Getters e Setters -------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProtocolo() {
        return protocolo;
    }

    public void setProtocolo(String protocolo) {
        this.protocolo = protocolo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getLogradouro() {
        return logradouro;
    }

    public void setLogradouro(String logradouro) {
        this.logradouro = logradouro;
    }

    public String getBairro() {
        return bairro;
    }

    public void setBairro(String bairro) {
        this.bairro = bairro;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getPrioridade() {
        return prioridade;
    }

    public void setPrioridade(String prioridade) {
        this.prioridade = prioridade;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public StatusSolicitacao getStatus() {
        return status;
    }

    public void setStatus(StatusSolicitacao status) {
        this.status = status;
    }

    public Usuario getCidadao() {
        return cidadao;
    }

    public void setCidadao(Usuario cidadao) {
        this.cidadao = cidadao;
    }

    public LocalDateTime getDataAbertura() {
        return dataAbertura;
    }

    public void setDataAbertura(LocalDateTime dataAbertura) {
        this.dataAbertura = dataAbertura;
    }

    public LocalDateTime getDataConclusao() {
        return dataConclusao;
    }

    public void setDataConclusao(LocalDateTime dataConclusao) {
        this.dataConclusao = dataConclusao;
    }
}
