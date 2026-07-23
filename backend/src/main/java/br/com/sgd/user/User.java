package br.com.sgd.user;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuarios")
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 120)
  private String nome;

  @Column(nullable = false, unique = true, length = 254)
  private String email;

  @Column(name = "senha_hash", nullable = false)
  private String senhaHash;

  @Column(nullable = false)
  private boolean ativo = true;

  @Column(name = "criado_em", nullable = false)
  private Instant criadoEm = Instant.now();

  @Column(name = "atualizado_em", nullable = false)
  private Instant atualizadoEm = Instant.now();

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "usuario_perfis", joinColumns = @JoinColumn(name = "usuario_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "perfil")
  private Set<Role> perfis = new HashSet<>();

  protected User() {}

  public User(String nome, String email, String senhaHash, Set<Role> perfis) {
    this.nome = nome;
    this.email = email.toLowerCase();
    this.senhaHash = senhaHash;
    this.perfis = new HashSet<>(perfis);
  }

  public Long getId() {
    return id;
  }

  public String getNome() {
    return nome;
  }

  public String getEmail() {
    return email;
  }

  public String getSenhaHash() {
    return senhaHash;
  }

  public boolean isAtivo() {
    return ativo;
  }

  public Set<Role> getPerfis() {
    return Set.copyOf(perfis);
  }

  public void update(String nome, Set<Role> perfis, Boolean ativo) {
    if (nome != null && !nome.isBlank()) this.nome = nome;
    if (perfis != null && !perfis.isEmpty()) this.perfis = new HashSet<>(perfis);
    if (ativo != null) this.ativo = ativo;
    this.atualizadoEm = Instant.now();
  }

  public void updatePassword(String senhaHash) {
    this.senhaHash = senhaHash;
    this.atualizadoEm = Instant.now();
  }
}
