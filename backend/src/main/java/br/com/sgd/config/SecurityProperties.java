package br.com.sgd.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(String jwtSecret, long accessTokenMinutes, long refreshTokenDays,
                                 long passwordResetMinutes, String adminEmail, String adminPassword) { }
