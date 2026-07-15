package br.com.sgd.auth;

import br.com.sgd.user.User;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

/** Test double that intentionally neither sends nor exposes a raw token. */
@Component
@Profile("test")
public class LocalPasswordResetNotifier implements PasswordResetNotifier {
    @Override
    public void notify(User user, String rawToken, PasswordResetToken.Type type, long validityMinutes) { }
}
