package com.poli.restaurante.web.exception;

import com.poli.restaurante.exception.BusinessRuleException;
import com.poli.restaurante.exception.ConflictException;
import com.poli.restaurante.exception.ResourceNotFoundException;
import com.poli.restaurante.exception.TooManyAttemptsException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Traduce excepciones a respuestas RFC 7807 (ProblemDetail) con códigos HTTP consistentes. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail noEncontrado(ResourceNotFoundException e) {
        return problema(HttpStatus.NOT_FOUND, "Recurso no encontrado", e.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail conflicto(ConflictException e) {
        return problema(HttpStatus.CONFLICT, "Conflicto", e.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    ProblemDetail reglaDeNegocio(BusinessRuleException e) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "Regla de negocio incumplida", e.getMessage());
    }

    @ExceptionHandler(TooManyAttemptsException.class)
    ProblemDetail demasiadosIntentos(TooManyAttemptsException e) {
        return problema(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos", e.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail noAutenticado(AuthenticationException e) {
        return problema(HttpStatus.UNAUTHORIZED, "No autenticado", "Credenciales inválidas");
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail accesoDenegado(AccessDeniedException e) {
        // Los mensajes propios de negocio son útiles; los genéricos de Spring Security no se exponen.
        String detalle = "Access Denied".equals(e.getMessage()) ? "No tiene permisos para esta operación" : e.getMessage();
        return problema(HttpStatus.FORBIDDEN, "Acceso denegado", detalle);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail modificacionConcurrente(ObjectOptimisticLockingFailureException e) {
        return problema(HttpStatus.CONFLICT, "Conflicto de concurrencia",
                "El recurso fue modificado por otra operación; vuelva a intentarlo");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integridad(DataIntegrityViolationException e) {
        log.warn("Violación de integridad: {}", e.getMostSpecificCause().getMessage());
        return problema(HttpStatus.CONFLICT, "Conflicto de datos",
                "La operación viola una restricción de integridad de los datos");
    }

    @ExceptionHandler(PropertyReferenceException.class)
    ProblemDetail ordenInvalido(PropertyReferenceException e) {
        return problema(HttpStatus.BAD_REQUEST, "Parámetro inválido", "Campo de ordenamiento desconocido: " + e.getPropertyName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail tipoInvalido(MethodArgumentTypeMismatchException e) {
        return problema(HttpStatus.BAD_REQUEST, "Parámetro inválido", "El parámetro '" + e.getName() + "' tiene un valor inválido");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail inesperado(Exception e) {
        log.error("Error no controlado", e);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno", "Ocurrió un error inesperado");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> errores.putIfAbsent(f.getField(), f.getDefaultMessage()));
        ProblemDetail pd = problema(HttpStatus.BAD_REQUEST, "Datos inválidos", "La solicitud contiene datos inválidos");
        pd.setProperty("errores", errores);
        return ResponseEntity.badRequest().body(pd);
    }

    private static ProblemDetail problema(HttpStatus estado, String titulo, String detalle) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(estado, detalle);
        pd.setTitle(titulo);
        return pd;
    }
}
