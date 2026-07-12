package br.com.sgd.auth;

import br.com.sgd.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Safe fallback until a transactional e-mail adapter is configured. */
@Component @Profile("!local & !test")
public class ProductionPasswordResetNotifier implements PasswordResetNotifier {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductionPasswordResetNotifier.class);
    @Override public void notify(User user, String rawToken) {
        LOGGER.warn("Password reset requested for {}, but no transactional e-mail provider is configured.", user.getEmail());
    }
}
