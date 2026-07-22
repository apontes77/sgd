package br.com.sgd.auth;

import br.com.sgd.observability.TraceContext;
import br.com.sgd.user.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** SMTP adapter. Raw tokens live only in the short-lived in-memory delivery request. */
@Component
@org.springframework.context.annotation.Profile("!test")
public class ProductionPasswordResetNotifier implements PasswordResetNotifier {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductionPasswordResetNotifier.class);
    private static final int MAX_ATTEMPTS = 3;

    private final JavaMailSender mailSender;
    private final PasswordResetMailProperties properties;
    private final TaskExecutor executor;
    private final PasswordResetDeliveryAck deliveryAck;
    private final long retryInitialDelayMillis;

    @Autowired
    public ProductionPasswordResetNotifier(
            JavaMailSender mailSender,
            PasswordResetMailProperties properties,
            MailProperties mailProperties,
            Environment environment,
            @Qualifier("passwordResetMailExecutor") TaskExecutor executor,
            PasswordResetDeliveryAck deliveryAck) {
        this(mailSender, properties, mailProperties, environment, executor, deliveryAck, 1_000L);
    }

    ProductionPasswordResetNotifier(
            JavaMailSender mailSender,
            PasswordResetMailProperties properties,
            MailProperties mailProperties,
            Environment environment,
            TaskExecutor executor,
            PasswordResetDeliveryAck deliveryAck,
            long retryInitialDelayMillis) {
        this.mailSender = mailSender;
        this.properties = properties;
        this.executor = executor;
        this.deliveryAck = deliveryAck;
        this.retryInitialDelayMillis = retryInitialDelayMillis;
        validateProductionConfiguration(mailProperties, environment);
    }

    @Override
    public void notify(User user, String rawToken, PasswordResetToken.Type type, long validityMinutes) {
        Delivery delivery = new Delivery(user.getId(), user.getEmail(), rawToken, type, validityMinutes,
                TraceContext.currentTraceId());
        LOGGER.info("Password e-mail scheduled userId={} type={}", delivery.userId(), delivery.type());
        Runnable submit = () -> submit(delivery);

        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    submit.run();
                }
            });
        } else {
            submit.run();
        }
    }

    private void submit(Delivery delivery) {
        try {
            executor.execute(() -> {
                try (MDC.MDCCloseable ignored = MDC.putCloseable(TraceContext.MDC_KEY, delivery.traceId())) {
                    deliverWithRetry(delivery);
                }
            });
            LOGGER.info("Password e-mail queued userId={} type={}", delivery.userId(), delivery.type());
        } catch (RejectedExecutionException exception) {
            LOGGER.error("Password e-mail queue full; delivery discarded userId={} type={}",
                    delivery.userId(), delivery.type());
        }
    }

    private void deliverWithRetry(Delivery delivery) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                send(delivery);
                deliveryAck.markDelivered(delivery.rawToken());
                LOGGER.info("Password e-mail sent userId={} type={} attempt={}",
                        delivery.userId(), delivery.type(), attempt);
                return;
            } catch (Exception exception) {
                if (attempt == MAX_ATTEMPTS) {
                    LOGGER.error("Password e-mail failed userId={} type={} attempts={} errorType={}",
                            delivery.userId(), delivery.type(), MAX_ATTEMPTS, exception.getClass().getSimpleName());
                    return;
                }
                LOGGER.warn("Password e-mail attempt failed userId={} type={} attempt={} errorType={}",
                        delivery.userId(), delivery.type(), attempt, exception.getClass().getSimpleName());
                if (!waitBeforeRetry(attempt)) {
                    return;
                }
            }
        }
    }

    private boolean waitBeforeRetry(int failedAttempt) {
        try {
            Thread.sleep(retryInitialDelayMillis * failedAttempt);
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Password e-mail delivery retry was interrupted.");
            return false;
        }
    }

    private void send(Delivery delivery) throws MessagingException {
        String base = properties.publicUrl() == null ? "" : properties.publicUrl().replaceAll("/+$", "");
        String link = base + "/#/redefinir-senha/" + delivery.rawToken();
        boolean setup = delivery.type() == PasswordResetToken.Type.DEFINICAO_INICIAL;
        String subject = setup ? "Defina sua senha" : "Redefina sua senha";
        String action = setup ? "definir sua senha" : "redefinir sua senha";

        String text = "Recebemos uma solicitação para " + action + ".\n\n"
                + "Use o link abaixo em até " + delivery.validityMinutes() + " minutos:\n"
                + link + "\n\n"
                + "Se você não esperava esta mensagem, ignore-a.";
        String html = "<!doctype html><html><body>"
                + "<p>Recebemos uma solicitação para " + action + ".</p>"
                + "<p><a href=\"" + escapeHtml(link) + "\">" + capitalize(action) + "</a></p>"
                + "<p>Este link expira em " + delivery.validityMinutes() + " minutos.</p>"
                + "<p>Se você não esperava esta mensagem, ignore-a.</p>"
                + "</body></html>";

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        helper.setFrom(properties.from());
        helper.setTo(delivery.recipient());
        helper.setSubject(subject);
        helper.setText(text, html);
        mailSender.send(message);
    }

    private void validateProductionConfiguration(MailProperties mail, Environment environment) {
        if (!environment.acceptsProfiles(Profiles.of("prod"))) {
            return;
        }
        boolean invalid = isBlank(mail.getHost()) || mail.getPort() == null || mail.getPort() <= 0
                || isBlank(mail.getUsername()) || isBlank(mail.getPassword())
                || isBlank(properties.from()) || isBlank(properties.publicUrl())
                || !properties.publicUrl().startsWith("https://") || !properties.tls();
        if (invalid) {
            throw new IllegalStateException(
                    "Production mail configuration requires HTTPS public URL, SMTP credentials and TLS.");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static String escapeHtml(String value) {
        return Objects.requireNonNull(value)
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private record Delivery(Long userId, String recipient, String rawToken, PasswordResetToken.Type type,
                            long validityMinutes, String traceId) { }
}
