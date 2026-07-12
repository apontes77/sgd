package br.com.sgd.organizacao;

import br.com.sgd.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "gerencias")
public class Gerencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gerente_id", nullable = false)
    private User gerente;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm = Instant.now();

    protected Gerencia() {
    }

    public Gerencia(String nome, User gerente) {
        this.nome = normalizarNome(nome);
        this.gerente = gerente;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public User getGerente() { return gerente; }
    public boolean isAtivo() { return ativo; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }

    public void update(String nome, User gerente, Boolean ativo) {
        if (nome != null) this.nome = normalizarNome(nome);
        if (gerente != null) this.gerente = gerente;
        if (ativo != null) this.ativo = ativo;
        this.atualizadoEm = Instant.now();
    }

    private static String normalizarNome(String nome) {
        if (nome == null || nome.isBlank()) throw new IllegalArgumentException("O nome da gerência é obrigatório.");
        return nome.trim();
    }
}
