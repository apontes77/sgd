package br.com.sgd.exception;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import br.com.sgd.auth.AuthService;
import br.com.sgd.user.UserService;
import br.com.sgd.organizacao.GerenciaService;
import br.com.sgd.organizacao.DiscipuladoService;
import br.com.sgd.organizacao.Discipulado;

@RestControllerAdvice
public class ApiExceptionHandler {

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

    @ExceptionHandler(UserService.DuplicateEmailException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateEmail(UserService.DuplicateEmailException exception) {
        return response(HttpStatus.CONFLICT, "E-mail já cadastrado.");
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
        return response(HttpStatus.UNPROCESSABLE_ENTITY, "Regra organizacional não atendida.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception exception) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado.");
    }

    private ResponseEntity<Map<String, Object>> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "message", message));
    }
}
