package br.com.sgd.organizacao;

import br.com.sgd.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "discipulados")
public class Discipulado {
    public static final int MAX_CO_LIDERES = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Sexo sexo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gerencia_id", nullable = false)
    private Gerencia gerencia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "discipulador_id", nullable = false)
    private User discipulador;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "discipulado_co_lideres",
        joinColumns = @JoinColumn(name = "discipulado_id"),
        inverseJoinColumns = @JoinColumn(name = "usuario_id")
    )
    private Set<User> coLideres = new LinkedHashSet<>();

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm = Instant.now();

    protected Discipulado() {
    }

    public Discipulado(String nome, Sexo sexo, Gerencia gerencia, User discipulador) {
        this.nome = normalizarNome(nome);
        if (sexo == null) throw new IllegalArgumentException("O sexo do discipulado é obrigatório.");
        if (gerencia == null) throw new IllegalArgumentException("A gerência do discipulado é obrigatória.");
        if (discipulador == null) throw new IllegalArgumentException("O discipulador é obrigatório.");
        this.sexo = sexo;
        this.gerencia = gerencia;
        this.discipulador = discipulador;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public Sexo getSexo() { return sexo; }
    public Gerencia getGerencia() { return gerencia; }
    public User getDiscipulador() { return discipulador; }
    public Set<User> getCoLideres() { return Set.copyOf(coLideres); }
    public boolean isAtivo() { return ativo; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }

    public void update(String nome, Sexo sexo, Gerencia gerencia, User discipulador, Boolean ativo) {
        if (nome != null) this.nome = normalizarNome(nome);
        if (sexo != null) this.sexo = sexo;
        if (gerencia != null) this.gerencia = gerencia;
        if (discipulador != null) this.discipulador = discipulador;
        if (ativo != null) this.ativo = ativo;
        this.atualizadoEm = Instant.now();
    }

    public void replaceCoLideres(Set<User> novosCoLideres) {
        if (novosCoLideres == null) throw new IllegalArgumentException("A lista de co-líderes é obrigatória.");
        if (novosCoLideres.size() > MAX_CO_LIDERES) throw new CoLiderLimitExceededException();
        this.coLideres = new LinkedHashSet<>(novosCoLideres);
        this.atualizadoEm = Instant.now();
    }

    private static String normalizarNome(String nome) {
        if (nome == null || nome.isBlank()) throw new IllegalArgumentException("O nome do discipulado é obrigatório.");
        return nome.trim();
    }

    public static class CoLiderLimitExceededException extends RuntimeException {
    }
}
