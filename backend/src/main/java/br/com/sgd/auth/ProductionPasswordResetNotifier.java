package br.com.sgd.auth;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import br.com.sgd.config.SecurityProperties;
import br.com.sgd.user.User;

@Component
@Profile("!local & !test")
public class ProductionPasswordResetNotifier implements PasswordResetNotifier {
  private final JavaMailSender mailSender;
  private final String frontendUrl;
  private final String from;
  private final long expirationMinutes;

  public ProductionPasswordResetNotifier(
      JavaMailSender mailSender,
      @Value("${app.password-reset.frontend-url:}") String frontendUrl,
      @Value("${app.mail.from:}") String from,
      SecurityProperties properties) {
    if (frontendUrl == null || frontendUrl.isBlank()) {
      throw new IllegalStateException(
          "PASSWORD_RESET_FRONTEND_URL deve ser configurada em producao");
    }
    if (from == null || from.isBlank()) {
      throw new IllegalStateException("MAIL_FROM deve ser configurado em producao");
    }
    this.mailSender = mailSender;
    this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    this.from = from;
    this.expirationMinutes = properties.passwordResetMinutes();
  }

  @Override
  public void notify(User user, String rawToken) {
    String encodedToken = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    String resetUrl = frontendUrl + "/redefinir-senha?token=" + encodedToken;
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(from);
    message.setTo(user.getEmail());
    message.setSubject("Redefinicao de senha - SGD");
    message.setText(
        """
                Ola, %s.

                Recebemos uma solicitacao para redefinir sua senha no SGD.
                Acesse o link abaixo em ate %d minutos:

                %s

                Se voce nao fez esta solicitacao, ignore esta mensagem.
                """
            .formatted(user.getNome(), expirationMinutes, resetUrl));
    mailSender.send(message);
  }
}
