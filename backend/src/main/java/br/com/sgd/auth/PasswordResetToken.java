package br.com.sgd.auth;

import br.com.sgd.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tokens_redefinicao_senha")
public class PasswordResetToken {
    public enum Type { REDEFINICAO, DEFINICAO_INICIAL }
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(name = "expira_em", nullable = false) private Instant expiraEm;
    @Column(name = "usado_em") private Instant usadoEm;
    @Column(name = "enviado_em") private Instant enviadoEm;
    @Column(name = "criado_em", nullable = false) private Instant criadoEm;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Type tipo;
    protected PasswordResetToken() { }
    public PasswordResetToken(User usuario, String tokenHash, Instant criadoEm, Instant expiraEm, Type tipo) {
        this.id = UUID.randomUUID(); this.usuario = usuario; this.tokenHash = tokenHash;
        this.criadoEm = criadoEm; this.expiraEm = expiraEm; this.tipo = tipo;
    }
    public User getUsuario() { return usuario; }
    public String getTokenHash() { return tokenHash; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getExpiraEm() { return expiraEm; }
    public Instant getEnviadoEm() { return enviadoEm; }
    public Type getTipo() { return tipo; }
    public boolean isValid(Instant now) { return usadoEm == null && expiraEm.isAfter(now); }
    public void use(Instant now) { usadoEm = now; }
    public void markSent(Instant now) { this.enviadoEm = now; }
}
