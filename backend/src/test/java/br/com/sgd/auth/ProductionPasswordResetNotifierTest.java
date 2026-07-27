package br.com.sgd.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;

import br.com.sgd.config.SecurityProperties;
import br.com.sgd.user.Role;
import br.com.sgd.user.User;

class ProductionPasswordResetNotifierTest {
  private final SecurityProperties properties =
      new SecurityProperties("01234567890123456789012345678901", 15, 7, 60, null, null);

  @Test
  void sendsAnAbsoluteSingleUseLinkAsHtml() throws Exception {
    JavaMailSender sender = mock(JavaMailSender.class);
    MimeMessage mimeMessage = new MimeMessage((Session) null);
    when(sender.createMimeMessage()).thenReturn(mimeMessage);

    ProductionPasswordResetNotifier notifier =
        new ProductionPasswordResetNotifier(
            sender, "https://sgd.example.com/", "no-reply@sgd.example.com", properties);
    User user = new User("Lider", "lider@example.com", "hash", Set.of(Role.DISCIPULADOR));

    notifier.notify(user, "token+/=");

    ArgumentCaptor<MimeMessage> message = ArgumentCaptor.forClass(MimeMessage.class);
    verify(sender).send(message.capture());
    assertThat(message.getValue().getFrom()[0].toString()).contains("no-reply@sgd.example.com");
    assertThat(message.getValue().getAllRecipients()[0].toString()).isEqualTo("lider@example.com");

    String content = rawMime(message.getValue());
    String expectedUrl = "https://sgd.example.com/redefinir-senha?token=token%2B%2F%3D";
    assertThat(content)
        .contains(expectedUrl)
        .contains("href=\"" + expectedUrl + "\"")
        .contains("60 minutos")
        .contains("text/html");
  }

  @Test
  void escapesHtmlInRecipientName() throws Exception {
    JavaMailSender sender = mock(JavaMailSender.class);
    MimeMessage mimeMessage = new MimeMessage((Session) null);
    when(sender.createMimeMessage()).thenReturn(mimeMessage);

    ProductionPasswordResetNotifier notifier =
        new ProductionPasswordResetNotifier(
            sender, "https://sgd.example.com", "no-reply@sgd.example.com", properties);
    User user =
        new User(
            "<script>alert(1)</script>", "lider@example.com", "hash", Set.of(Role.DISCIPULADOR));

    notifier.notify(user, "abc");

    ArgumentCaptor<MimeMessage> message = ArgumentCaptor.forClass(MimeMessage.class);
    verify(sender).send(message.capture());
    String content = rawMime(message.getValue());
    assertThat(content).contains("&lt;script&gt;alert(1)&lt;/script&gt;");
    assertThat(content)
        .containsPattern("(?s)Content-Type: text/html.*?&lt;script&gt;alert\\(1\\)&lt;/script&gt;");
  }

  @Test
  void refusesToStartWithoutProductionConfiguration() {
    JavaMailSender sender = mock(JavaMailSender.class);
    assertThatThrownBy(
            () -> new ProductionPasswordResetNotifier(sender, "", "from@example.com", properties))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("PASSWORD_RESET_FRONTEND_URL");
    assertThatThrownBy(
            () ->
                new ProductionPasswordResetNotifier(
                    sender, "https://sgd.example.com", "", properties))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("MAIL_FROM");
  }

  private static String rawMime(MimeMessage message) throws Exception {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    message.writeTo(buffer);
    return buffer.toString(StandardCharsets.UTF_8);
  }
}
