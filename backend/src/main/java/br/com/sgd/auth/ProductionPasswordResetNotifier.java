package br.com.sgd.auth;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

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
    String safeName = HtmlUtils.htmlEscape(user.getNome());
    String safeUrl = HtmlUtils.htmlEscape(resetUrl);

    try {
      MimeMessage mimeMessage = mailSender.createMimeMessage();
      MimeMessageHelper helper =
          new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
      helper.setFrom(from);
      helper.setTo(user.getEmail());
      helper.setSubject("Redefinicao de senha - SGD");
      helper.setText(plainTextBody(user.getNome(), resetUrl), htmlBody(safeName, safeUrl));
      mailSender.send(mimeMessage);
    } catch (MessagingException exception) {
      throw new IllegalStateException("Falha ao montar e-mail de redefinicao de senha", exception);
    }
  }

  private String plainTextBody(String nome, String resetUrl) {
    return """
            Ola, %s.

            Recebemos uma solicitacao para redefinir sua senha no SGD.
            Acesse o link abaixo em ate %d minutos:

            %s

            Se voce nao fez esta solicitacao, ignore esta mensagem.
            """
        .formatted(nome, expirationMinutes, resetUrl);
  }

  private String htmlBody(String safeName, String safeUrl) {
    return """
            <p>Ola, %s.</p>
            <p>Recebemos uma solicitacao para redefinir sua senha no SGD.
            Acesse o link abaixo em ate %d minutos:</p>
            <p><a href="%s">%s</a></p>
            <p>Se voce nao fez esta solicitacao, ignore esta mensagem.</p>
            """
        .formatted(safeName, expirationMinutes, safeUrl, safeUrl);
  }
}
