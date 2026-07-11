package br.com.sgd.user;

import br.com.sgd.audit.AuditLog;
import br.com.sgd.audit.AuditLogRepository;
import java.util.List;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class UserService {
    private final UserRepository users; private final PasswordEncoder passwords; private final AuditLogRepository audit;
    public UserService(UserRepository users, PasswordEncoder passwords, AuditLogRepository audit) { this.users = users; this.passwords = passwords; this.audit = audit; }
    @Transactional(readOnly = true) public List<User> list() { return users.findAll(); }
    public User create(String nome, String email, String password, Set<Role> perfis) { if (users.existsByEmailIgnoreCase(email)) throw new DuplicateEmailException(); User user = users.save(new User(nome, email, passwords.encode(password), perfis)); audit.save(new AuditLog(user, "USUARIO", "CRIACAO", "{}")); return user; }
    public User update(long id, String nome, Set<Role> perfis, Boolean ativo) { User user = users.findById(id).orElseThrow(UserNotFoundException::new); user.update(nome, perfis, ativo); audit.save(new AuditLog(user, "USUARIO", "ATUALIZACAO", "{}")); return user; }
    public static class DuplicateEmailException extends RuntimeException { } public static class UserNotFoundException extends RuntimeException { }
}
