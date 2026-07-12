package br.com.sgd.frequencia;

import br.com.sgd.adolescente.Adolescente;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "frequencias", uniqueConstraints = @UniqueConstraint(columnNames = {"encontro_id", "adolescente_id"}))
public class Frequencia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "encontro_id") private Encontro encontro;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "adolescente_id") private Adolescente adolescente;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private SituacaoFrequencia situacao;
    @Column(name = "registrada_em", nullable = false) private Instant registradaEm;
    @Column(name = "atualizada_em", nullable = false) private Instant atualizadaEm;
    protected Frequencia() { }
    public Frequencia(Encontro encontro, Adolescente adolescente, SituacaoFrequencia situacao, Instant agora) { this.encontro=encontro; this.adolescente=adolescente; this.situacao=situacao; this.registradaEm=agora; this.atualizadaEm=agora; }
    public void atualizar(SituacaoFrequencia situacao, Instant agora) { this.situacao=situacao; this.atualizadaEm=agora; }
    public Long getId(){return id;} public Encontro getEncontro(){return encontro;} public Adolescente getAdolescente(){return adolescente;}
    public SituacaoFrequencia getSituacao(){return situacao;} public Instant getRegistradaEm(){return registradaEm;}
}
