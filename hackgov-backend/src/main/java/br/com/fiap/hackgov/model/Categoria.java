package br.com.fiap.hackgov.model;

import jakarta.persistence.*;

/**
 * Representa a tabela CATEGORIA (tipos de problema urbano).
 * Ja vem preenchida pelo script dados_iniciais.sql.
 */
@Entity
@Table(name = "CATEGORIA")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqCategoria")
    @SequenceGenerator(name = "seqCategoria", sequenceName = "SEQ_CATEGORIA", allocationSize = 1)
    @Column(name = "ID_CATEGORIA")
    private Long id;

    @Column(name = "NOME", nullable = false, length = 80)
    private String nome;

    public Categoria() {
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
}
