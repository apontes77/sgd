package br.com.sgd.user;

import br.com.sgd.audit.AuditLog;
import br.com.sgd.audit.AuditLogRepository;
import br.com.sgd.auth.RefreshTokenRepository;
import br.com.sgd.auth.PasswordCredentialService;
import java.time.Instant;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class UserService {
    private final UserRepository users; private final AuditLogRepository audit; private final RefreshTokenRepository refreshTokens; private final PasswordCredentialService credentials;
    public UserService(UserRepository users, AuditLogRepository audit, RefreshTokenRepository refreshTokens, PasswordCredentialService credentials) { this.users = users; this.audit = audit; this.refreshTokens = refreshTokens; this.credentials = credentials; }
    @Transactional(readOnly = true) public Page<User> list(Boolean ativo, Pageable pageable) {
        return ativo == null ? users.findAll(pageable) : users.findAllByAtivo(ativo, pageable);
    }
    public User create(String nome, String email, Set<Role> perfis) { if (users.existsByEmailIgnoreCase(email)) throw new DuplicateEmailException(); User user = users.save(new User(nome, email, null, perfis)); audit.save(new AuditLog(user, "USUARIO", "CRIACAO", "{}")); credentials.requestInitialSetup(user); return user; }
    public User update(long id, String nome, Set<Role> perfis, Boolean ativo) { User user = users.findById(id).orElseThrow(UserNotFoundException::new); boolean deactivate = Boolean.FALSE.equals(ativo) && user.isAtivo(); user.update(nome, perfis, ativo); if (deactivate) refreshTokens.revokeAllByUserId(user.getId(), Instant.now()); audit.save(new AuditLog(user, "USUARIO", "ATUALIZACAO", "{}")); return user; }
    public static class DuplicateEmailException extends RuntimeException { } public static class UserNotFoundException extends RuntimeException { }
}
