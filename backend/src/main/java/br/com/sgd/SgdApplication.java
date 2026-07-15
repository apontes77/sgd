package br.com.sgd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class SgdApplication {

    public static void main(String[] args) {
        SpringApplication.run(SgdApplication.class, args);
    }
}
