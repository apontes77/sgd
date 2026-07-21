package br.com.sgd.exception;

import br.com.sgd.observability.TraceIds;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import br.com.sgd.auth.AuthService;
import br.com.sgd.auth.OAuthIdentityService;
import br.com.sgd.user.UserService;
import br.com.sgd.organizacao.GerenciaService;
import br.com.sgd.organizacao.DiscipuladoService;
import br.com.sgd.organizacao.Discipulado;
import org.springframework.security.authorization.AuthorizationDeniedException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidArgument(MethodArgumentTypeMismatchException exception) {
        return response(HttpStatus.BAD_REQUEST, "Parâmetro inválido.");
    }

    @ExceptionHandler({AuthService.InvalidCredentialsException.class, AuthService.InvalidTokenException.class})
    public ResponseEntity<Map<String, Object>> handleUnauthorized(RuntimeException exception) {
        return response(HttpStatus.UNAUTHORIZED, "Credenciais ou token inválidos.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {
        return response(HttpStatus.BAD_REQUEST, "Dados de entrada inválidos.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException exception) {
        return response(HttpStatus.BAD_REQUEST, detalheCorpoInvalido(exception));
    }

    private static String detalheCorpoInvalido(HttpMessageNotReadableException exception) {
        Throwable causa = exception.getMostSpecificCause();
        if (causa instanceof com.fasterxml.jackson.databind.exc.InvalidFormatException invalid
                && invalid.getTargetType() != null && invalid.getTargetType().isEnum()) {
            return "Valor inválido para " + invalid.getTargetType().getSimpleName() + ": '"
                    + invalid.getValue() + "'.";
        }
        if (causa instanceof IllegalArgumentException illegal && illegal.getMessage() != null
                && !illegal.getMessage().isBlank()) {
            return illegal.getMessage();
        }
        return "O corpo da requisição é inválido.";
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException exception) {
        return response(HttpStatus.BAD_REQUEST, "Parâmetros de entrada inválidos.");
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(AuthorizationDeniedException exception) {
        return response(HttpStatus.FORBIDDEN, "Você não possui permissão para realizar esta operação.");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        return response(status, exception.getReason() == null ? status.getReasonPhrase() : exception.getReason());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException exception) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage() == null ? "Dados de entrada inválidos." : exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException exception) {
        return response(HttpStatus.CONFLICT, "A operação viola uma restrição de integridade dos dados.");
    }

    @ExceptionHandler(OAuthIdentityService.InvalidOAuthIdentityException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidOAuth(OAuthIdentityService.InvalidOAuthIdentityException exception) {
        return response(HttpStatus.UNAUTHORIZED, "A identidade externa não corresponde a um usuário ativo cadastrado.");
    }

    @ExceptionHandler(OAuthIdentityService.OAuthIdentityConflictException.class)
    public ResponseEntity<Map<String, Object>> handleOAuthConflict(OAuthIdentityService.OAuthIdentityConflictException exception) {
        return response(HttpStatus.CONFLICT, "O provedor externo já está vinculado a outra identidade.");
    }

    @ExceptionHandler(UserService.DuplicateEmailException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateEmail(UserService.DuplicateEmailException exception) {
        return response(HttpStatus.CONFLICT, "E-mail já cadastrado.");
    }

    @ExceptionHandler(UserService.UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(UserService.UserNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "Usuário não encontrado.");
    }

    @ExceptionHandler({GerenciaService.GerenciaNotFoundException.class, GerenciaService.UsuarioOrganizacionalNotFoundException.class,
            DiscipuladoService.DiscipuladoNotFoundException.class, DiscipuladoService.GerenciaNotFoundException.class,
            DiscipuladoService.UsuarioOrganizacionalNotFoundException.class})
    public ResponseEntity<Map<String, Object>> handleOrganizationalNotFound(RuntimeException exception) {
        return response(HttpStatus.NOT_FOUND, "Recurso organizacional não encontrado.");
    }

    @ExceptionHandler({GerenciaService.GerenteInvalidoException.class, DiscipuladoService.GerenciaInativaException.class,
            DiscipuladoService.DiscipuladorInvalidoException.class, DiscipuladoService.CoLiderInvalidoException.class,
            Discipulado.CoLiderLimitExceededException.class})
    public ResponseEntity<Map<String, Object>> handleOrganizationalRule(RuntimeException exception) {
        String detail = switch (exception) {
            case GerenciaService.GerenteInvalidoException ignored -> "O gerente deve estar ativo e possuir o perfil GERENTE.";
            case DiscipuladoService.GerenciaInativaException ignored -> "A gerência informada está inativa.";
            case DiscipuladoService.DiscipuladorInvalidoException ignored -> "O discipulador deve estar ativo e possuir o perfil DISCIPULADOR.";
            case DiscipuladoService.CoLiderInvalidoException ignored -> "O co-líder deve estar ativo, possuir o perfil CO_LIDER e ser diferente do discipulador.";
            default -> "Um discipulado aceita no máximo dois co-líderes distintos.";
        };
        return response(HttpStatus.CONFLICT, detail);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception exception) {
        LOGGER.error("Erro inesperado ao processar requisição", exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado.");
    }

    private ResponseEntity<Map<String, Object>> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON).body(Map.of(
                "type", "about:blank",
                "title", status.getReasonPhrase(),
                "status", status.value(),
                "detail", message,
                "traceId", TraceIds.currentOrRandom()));
    }
}
