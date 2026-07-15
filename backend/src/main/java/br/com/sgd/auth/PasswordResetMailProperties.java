package br.com.sgd.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail")
public record PasswordResetMailProperties(String publicUrl, String from, boolean tls) { }
