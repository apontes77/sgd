package br.com.sgd.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.sgd.user.Role;
import br.com.sgd.user.User;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class ProductionPasswordResetNotifierTest {
    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void sendsTextAndHtmlWithFragmentLinkAndNoPlaintextPassword() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        ProductionPasswordResetNotifier notifier = notifier(sender, new MockEnvironment(), 0);

        notifier.notify(user(), "raw-token-123", PasswordResetToken.Type.REDEFINICAO, 30);

        verify(sender).send(message);
        assertThat(message.getSubject()).isEqualTo("Redefina sua senha");
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("pessoa@sgd.local");
        String content = flatten(message.getContent());
        assertThat(content)
                .contains("http://localhost:5173/#/redefinir-senha/raw-token-123")
                .contains("30 minutos")
                .doesNotContain("senha gerada", "password:");
        assertThat(message.getContent()).isInstanceOf(Multipart.class);
    }

    @Test
    void retriesThreeTimesWithoutPersistingDelivery() {
        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenAnswer(ignored -> new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new MailSendException("SMTP unavailable")).when(sender).send(any(MimeMessage.class));
        ProductionPasswordResetNotifier notifier = notifier(sender, new MockEnvironment(), 0);

        notifier.notify(user(), "raw-token-123", PasswordResetToken.Type.DEFINICAO_INICIAL, 1_440);

        verify(sender, times(3)).send(any(MimeMessage.class));
    }

    @Test
    void onlySubmitsDeliveryAfterCommit() {
        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenAnswer(ignored -> new MimeMessage(Session.getInstance(new Properties())));
        ProductionPasswordResetNotifier notifier = notifier(sender, new MockEnvironment(), 0);
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        notifier.notify(user(), "raw-token-123", PasswordResetToken.Type.REDEFINICAO, 30);

        verify(sender, never()).send(any(MimeMessage.class));
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }
        verify(sender).send(any(MimeMessage.class));
    }

    @Test
    void refusesIncompleteProductionConfiguration() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        MailProperties mail = new MailProperties();
        mail.setHost("smtp.example.com");
        mail.setPort(587);

        assertThatThrownBy(() -> new ProductionPasswordResetNotifier(
                mock(JavaMailSender.class),
                new PasswordResetMailProperties("http://example.com", "no-reply@example.com", false),
                mail,
                environment,
                new SyncTaskExecutor(),
                0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Production mail configuration");
    }

    private static ProductionPasswordResetNotifier notifier(
            JavaMailSender sender, MockEnvironment environment, long retryDelay) {
        MailProperties mail = new MailProperties();
        mail.setHost("localhost");
        mail.setPort(1025);
        return new ProductionPasswordResetNotifier(
                sender,
                new PasswordResetMailProperties("http://localhost:5173", "nao-responda@sgd.local", false),
                mail,
                environment,
                new SyncTaskExecutor(),
                retryDelay);
    }

    private static User user() {
        return new User("Pessoa", "pessoa@sgd.local", null, Set.of(Role.DISCIPULADOR));
    }

    private static String flatten(Object content) throws Exception {
        if (content instanceof String text) {
            return text;
        }
        if (content instanceof Multipart multipart) {
            StringBuilder result = new StringBuilder();
            for (int index = 0; index < multipart.getCount(); index++) {
                BodyPart part = multipart.getBodyPart(index);
                result.append(flatten(part.getContent()));
            }
            return result.toString();
        }
        return "";
    }
}
