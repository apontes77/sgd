package br.com.sgd.auth;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import br.com.sgd.config.SecurityProperties;
import br.com.sgd.user.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
  private final SecurityProperties properties;
  private final SecretKey key;

  public JwtService(SecurityProperties properties) {
    this.properties = properties;
    this.key = Keys.hmacShaKeyFor(properties.jwtSecret().getBytes(StandardCharsets.UTF_8));
  }

  public String createAccessToken(User user) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(user.getEmail())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(properties.accessTokenMinutes() * 60)))
        .claim("roles", user.getPerfis().stream().map(Enum::name).toList())
        .signWith(key)
        .compact();
  }

  public String subject(String token) {
    return claims(token).getSubject();
  }

  public boolean isValid(String token) {
    try {
      claims(token);
      return true;
    } catch (RuntimeException exception) {
      return false;
    }
  }

  private Claims claims(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }
}
