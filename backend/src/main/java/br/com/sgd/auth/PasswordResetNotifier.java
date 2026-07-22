package br.com.sgd.auth;

import br.com.sgd.user.User;

public interface PasswordResetNotifier {
    void notify(User user, String rawToken, PasswordResetToken.Type type, long validityMinutes);
}
