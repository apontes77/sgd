package br.com.sgd.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import br.com.sgd.user.User;

/** Adapter for local/test use. Replace with transactional e-mail in production. */
@Component
@Profile({"local", "test"})
public class LocalPasswordResetNotifier implements PasswordResetNotifier {
  private static final Logger LOGGER = LoggerFactory.getLogger(LocalPasswordResetNotifier.class);

  @Override
  public void notify(User user, String rawToken) {
    LOGGER.info("Password reset token for local user {}: {}", user.getEmail(), rawToken);
  }
}
