package br.com.sgd.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import br.com.sgd.config.SecurityProperties;
import br.com.sgd.user.Role;
import br.com.sgd.user.User;

class ProductionPasswordResetNotifierTest {
  private final SecurityProperties properties =
      new SecurityProperties("01234567890123456789012345678901", 15, 7, 60, null, null);

  @Test
  void sendsAnAbsoluteSingleUseLink() {
    JavaMailSender sender = mock(JavaMailSender.class);
    ProductionPasswordResetNotifier notifier =
        new ProductionPasswordResetNotifier(
            sender, "https://sgd.example.com/", "no-reply@sgd.example.com", properties);
    User user = new User("Lider", "lider@example.com", "hash", Set.of(Role.DISCIPULADOR));

    notifier.notify(user, "token+/=");

    ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(sender).send(message.capture());
    assertThat(message.getValue().getFrom()).isEqualTo("no-reply@sgd.example.com");
    assertThat(message.getValue().getTo()).containsExactly("lider@example.com");
    assertThat(message.getValue().getText())
        .contains("https://sgd.example.com/redefinir-senha?token=token%2B%2F%3D")
        .contains("60 minutos");
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
}
