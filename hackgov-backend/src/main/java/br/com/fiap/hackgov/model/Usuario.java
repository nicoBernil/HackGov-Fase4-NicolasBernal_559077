package br.com.fiap.hackgov.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Representa a tabela USUARIO no banco.
 *
 * Cada anotacao (@Entity, @Table, @Column...) liga um pedaco desta classe a
 * uma coluna da tabela. O Hibernate usa esse "mapa" para transformar linhas do
 * banco em objetos Java e vice-versa.
 */
@Entity
@Table(name = "USUARIO")
public class Usuario {

    @Id
    // Em Oracle os IDs vem de uma SEQUENCE. Aqui ligamos o ID a SEQ_USUARIO.
    // allocationSize = 1 e importante: faz o Java pedir os numeros de 1 em 1,
    // igual a sequence criada no banco.
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqUsuario")
    @SequenceGenerator(name = "seqUsuario", sequenceName = "SEQ_USUARIO", allocationSize = 1)
    @Column(name = "ID_USUARIO")
    private Long id;

    @Column(name = "NOME", nullable = false, length = 120)
    private String nome;

    @Column(name = "CPF", nullable = false, unique = true, length = 11)
    private String cpf;

    @Column(name = "EMAIL", nullable = false, unique = true, length = 150)
    private String email;

    // Guardamos SEMPRE o hash da senha, nunca a senha original.
    @Column(name = "SENHA_HASH", nullable = false, length = 100)
    private String senhaHash;

    // Guarda o texto "CIDADAO" ou "GESTOR" na coluna PERFIL.
    @Enumerated(EnumType.STRING)
    @Column(name = "PERFIL", nullable = false, length = 10)
    private Perfil perfil;

    @Column(name = "DATA_CRIACAO", nullable = false)
    private LocalDateTime dataCriacao;

    // O JPA exige um construtor vazio.
    public Usuario() {
    }

    public Usuario(String nome, String cpf, String email, String senhaHash, Perfil perfil) {
        this.nome = nome;
        this.cpf = cpf;
        this.email = email;
        this.senhaHash = senhaHash;
        this.perfil = perfil;
        this.dataCriacao = LocalDateTime.now();
    }

    // ------------------- Getters e Setters -------------------
    // Sao os metodos que permitem ler e alterar cada campo.

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

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public void setPerfil(Perfil perfil) {
        this.perfil = perfil;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }
}
