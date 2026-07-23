package br.com.sgd.adolescente;

import java.time.Instant;
import java.time.LocalDate;
import jakarta.persistence.*;

@Entity
@Table(name = "adolescentes")
public class Adolescente {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 120)
  private String nome;

  @Column(name = "data_nascimento", nullable = false)
  private LocalDate dataNascimento;

  @Column(length = 40)
  private String telefone;

  @Column(length = 120)
  private String instagram;

  @Column(nullable = false)
  private boolean ativo = true;

  @Column(name = "criado_em", nullable = false)
  private Instant criadoEm = Instant.now();

  @Column(name = "atualizado_em", nullable = false)
  private Instant atualizadoEm = Instant.now();

  protected Adolescente() {}

  public Adolescente(String nome, LocalDate dataNascimento, String telefone, String instagram) {
    atualizarDados(nome, dataNascimento, telefone, instagram);
  }

  public void atualizar(
      String nome, LocalDate dataNascimento, String telefone, String instagram, Boolean ativo) {
    atualizarDados(nome, dataNascimento, telefone, instagram);
    if (ativo != null) this.ativo = ativo;
    atualizadoEm = Instant.now();
  }

  private void atualizarDados(
      String nome, LocalDate dataNascimento, String telefone, String instagram) {
    if (nome == null || nome.isBlank())
      throw new IllegalArgumentException("O nome do adolescente é obrigatório.");
    if (dataNascimento == null || dataNascimento.isAfter(LocalDate.now()))
      throw new IllegalArgumentException("A data de nascimento é inválida.");
    this.nome = nome.trim();
    this.dataNascimento = dataNascimento;
    this.telefone = normalizar(telefone);
    this.instagram = normalizar(instagram);
  }

  private static String normalizar(String valor) {
    return valor == null || valor.isBlank() ? null : valor.trim();
  }

  public Long getId() {
    return id;
  }

  public String getNome() {
    return nome;
  }

  public LocalDate getDataNascimento() {
    return dataNascimento;
  }

  public String getTelefone() {
    return telefone;
  }

  public String getInstagram() {
    return instagram;
  }

  public boolean isAtivo() {
    return ativo;
  }
}
