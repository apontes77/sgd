package br.com.sgd.frequencia;

import br.com.sgd.organizacao.Discipulado;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity @Table(name = "encontros")
public class Encontro {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "discipulado_id") private Discipulado discipulado;
    @Column(nullable = false) private LocalDate data;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12) private SituacaoEncontro situacao;
    @Column(length = 500) private String justificativa;
    @Column(name = "criado_em", nullable = false) private Instant criadoEm = Instant.now();
    @Column(name = "atualizado_em", nullable = false) private Instant atualizadoEm = Instant.now();
    protected Encontro() { }
    public Encontro(Discipulado discipulado, LocalDate data, SituacaoEncontro situacao, Instant agora) {
        this(discipulado, data, situacao, null, agora);
    }
    public Encontro(Discipulado discipulado, LocalDate data, SituacaoEncontro situacao, String justificativa, Instant agora) {
        if (discipulado == null || data == null || situacao == null) throw new IllegalArgumentException("Os dados do encontro são obrigatórios.");
        this.discipulado = discipulado; this.data = data; this.situacao = situacao; this.justificativa = justificativa; this.criadoEm = agora; this.atualizadoEm = agora;
    }
    public void atualizar(LocalDate data, SituacaoEncontro situacao, String justificativa, Instant agora) { if (data != null) this.data = data; if (situacao != null) this.situacao = situacao; this.justificativa = justificativa; this.atualizadoEm = agora; }
    public Long getId() { return id; } public Discipulado getDiscipulado() { return discipulado; } public LocalDate getData() { return data; }
    public SituacaoEncontro getSituacao() { return situacao; } public String getJustificativa() { return justificativa; } public Instant getCriadoEm() { return criadoEm; }
}
