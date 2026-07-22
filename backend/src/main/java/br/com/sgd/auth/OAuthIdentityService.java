package br.com.sgd.auth;

import br.com.sgd.user.User;
import br.com.sgd.user.UserRepository;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Provider-neutral account linking boundary used by Google and Microsoft login adapters. */
@Service @Transactional
public class OAuthIdentityService {
    private final OAuthIdentityRepository identities;
    private final UserRepository users;
    public OAuthIdentityService(OAuthIdentityRepository identities, UserRepository users) { this.identities = identities; this.users = users; }

    public User resolveOrProvision(OAuthProvider provider, String subject, String verifiedEmail, String displayName) {
        if (provider == null || blank(subject) || blank(verifiedEmail)) throw new InvalidOAuthIdentityException();
        var identity = identities.findByProviderAndExternalSubject(provider, subject);
        if (identity.isPresent()) {
            if (!identity.get().getUsuario().isAtivo()) throw new InvalidOAuthIdentityException();
            return identity.get().getUsuario();
        }
        return linkVerifiedIdentity(provider, subject, verifiedEmail, displayName);
    }

    private User linkVerifiedIdentity(OAuthProvider provider, String subject, String email, String displayName) {
        User user = users.findByEmailIgnoreCase(email).orElseGet(() -> users.save(new User(
                blank(displayName) ? email : displayName, email, null, Set.of())));
        if (!user.isAtivo()) throw new InvalidOAuthIdentityException();
        identities.findByProviderAndUsuarioId(provider, user.getId()).ifPresent(identity -> { throw new OAuthIdentityConflictException(); });
        identities.save(new OAuthIdentity(user, provider, subject));
        return user;
    }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    public static class InvalidOAuthIdentityException extends RuntimeException { }
    public static class OAuthIdentityConflictException extends RuntimeException { }
}
