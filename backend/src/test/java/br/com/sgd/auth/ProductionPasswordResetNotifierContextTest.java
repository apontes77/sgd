package br.com.sgd.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:mail-context;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.mail.host=localhost",
        "spring.mail.port=1025",
        "app.security.admin-email="
})
@ActiveProfiles("mail-context")
class ProductionPasswordResetNotifierContextTest {
    @Autowired
    private PasswordResetNotifier notifier;

    @Test
    void createsProductionNotifierWithoutDefaultConstructor() {
        assertThat(notifier).isInstanceOf(ProductionPasswordResetNotifier.class);
    }
}
