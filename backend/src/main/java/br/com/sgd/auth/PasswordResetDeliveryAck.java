package br.com.sgd.auth;

import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetDeliveryAck {
    private final PasswordResetTokenRepository resetTokens;

    public PasswordResetDeliveryAck(PasswordResetTokenRepository resetTokens) {
        this.resetTokens = resetTokens;
    }

    @Transactional
    public void markDelivered(String rawToken) {
        resetTokens.markEnviadoEm(PasswordCredentialService.hash(rawToken), Instant.now());
    }
}
