package br.com.sgd.auth;

import br.com.sgd.user.User;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

/** Test double that acknowledges delivery without exposing the raw token. */
@Component
@Profile("test")
public class LocalPasswordResetNotifier implements PasswordResetNotifier {
    private final PasswordResetDeliveryAck deliveryAck;

    public LocalPasswordResetNotifier(PasswordResetDeliveryAck deliveryAck) {
        this.deliveryAck = deliveryAck;
    }

    @Override
    public void notify(User user, String rawToken, PasswordResetToken.Type type, long validityMinutes) {
        deliveryAck.markDelivered(rawToken);
    }
}
