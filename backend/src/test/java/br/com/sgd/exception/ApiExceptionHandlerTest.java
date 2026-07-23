package br.com.sgd.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.server.ResponseStatusException;

import br.com.sgd.auth.AuthService;
import br.com.sgd.auth.OAuthIdentityService;
import br.com.sgd.frequencia.SituacaoEncontro;
import br.com.sgd.user.UserService;

class ApiExceptionHandlerTest {
  private final ApiExceptionHandler handler = new ApiExceptionHandler();

  @Test
  void mapsAuthenticationAndOAuthFailures() {
    assertProblem(
        handler.handleUnauthorized(new AuthService.InvalidCredentialsException()),
        401,
        "Credenciais ou token");
    assertProblem(
        handler.handleUnauthorized(new AuthService.InvalidTokenException()),
        401,
        "Credenciais ou token");
    assertProblem(
        handler.handleInvalidOAuth(new OAuthIdentityService.InvalidOAuthIdentityException()),
        401,
        "identidade externa");
    assertProblem(
        handler.handleOAuthConflict(new OAuthIdentityService.OAuthIdentityConflictException()),
        409,
        "provedor externo");
  }

  @Test
  void mapsUserAndIntegrityFailures() {
    assertProblem(
        handler.handleDuplicateEmail(new UserService.DuplicateEmailException()), 409, "E-mail");
    assertProblem(handler.handleUserNotFound(new UserService.UserNotFoundException()), 404, "Usu");
    assertProblem(
        handler.handleDataIntegrity(new DataIntegrityViolationException("constraint")),
        409,
        "integridade");
  }

  @Test
  void mapsUnreadableBodyAndIllegalArguments() {
    assertProblem(
        handler.handleUnreadable(new HttpMessageNotReadableException("invalid body")),
        400,
        "corpo");
    assertProblem(
        handler.handleIllegalArgument(new IllegalArgumentException("specific detail")),
        400,
        "specific detail");
    assertProblem(
        handler.handleIllegalArgument(new IllegalArgumentException()), 400, "Dados de entrada");
  }

  @Test
  void mapsUnreadableBodyWithEnumCause() {
    var cause =
        com.fasterxml.jackson.databind.exc.InvalidFormatException.from(
            (com.fasterxml.jackson.core.JsonParser) null,
            "bad",
            "CANCELADO",
            SituacaoEncontro.class);
    assertProblem(
        handler.handleUnreadable(new HttpMessageNotReadableException("wrap", cause)),
        400,
        "SituacaoEncontro");
  }

  @Test
  void preservesResponseStatusReasonAndUsesDefaultTitleWhenAbsent() {
    assertProblem(
        handler.handleResponseStatus(
            new ResponseStatusException(HttpStatus.NOT_FOUND, "custom reason")),
        404,
        "custom reason");
    assertProblem(
        handler.handleResponseStatus(new ResponseStatusException(HttpStatus.GONE)), 410, "Gone");
  }

  @Test
  void unexpectedFailureDoesNotExposeExceptionDetails() {
    ResponseEntity<Map<String, Object>> response =
        handler.handleUnexpected(new RuntimeException("database password"));
    assertProblem(response, 500, "Erro interno inesperado");
    assertThat(response.getBody().get("detail").toString()).doesNotContain("database password");
  }

  private void assertProblem(
      ResponseEntity<Map<String, Object>> response, int status, String detailFragment) {
    assertThat(response.getStatusCode().value()).isEqualTo(status);
    assertThat(response.getHeaders().getContentType().toString())
        .isEqualTo("application/problem+json");
    assertThat(response.getBody())
        .containsEntry("type", "about:blank")
        .containsEntry("status", status);
    assertThat(response.getBody().get("detail").toString()).contains(detailFragment);
    assertThat(response.getBody().get("traceId").toString()).isNotBlank();
  }
}
