package com.agrotis.backend.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidation(MethodArgumentNotValidException exception,
                                                HttpServletRequest request) {
        Map<String, String> errors = exception.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(FieldError::getField, error -> error.getDefaultMessage() == null
                ? "valor inválido" : error.getDefaultMessage(), (first, ignored) -> first));
        return error(HttpStatus.BAD_REQUEST, "Dados inválidos", request, Map.of("campos", errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleUnreadable(HttpMessageNotReadableException exception,
                                                HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST,
            "JSON inválido ou data em formato incorreto. Use datas ISO-8601, por exemplo 2026-01-01T03:00:00Z.",
            request, Map.of());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleNotFound(EntityNotFoundException exception,
                                              HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleTypeMismatch(MethodArgumentTypeMismatchException exception,
                                                  HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Parâmetro inválido: " + exception.getName(), request, Map.of());
    }

    @ExceptionHandler({ConstraintViolationException.class, MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleConstraint(Exception exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Parâmetro obrigatório ausente ou fora do intervalo permitido.",
            request, Map.of());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException exception,
                                                                    HttpServletRequest request) {
        String message = exception.getReason() == null
            ? "Ocorreu um erro na solicitação."
            : exception.getReason();
        return ResponseEntity.status(exception.getStatusCode())
            .body(error(exception.getStatusCode(), message, request, Map.of()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handleConflict(DataIntegrityViolationException exception,
                                              HttpServletRequest request) {
        return error(HttpStatus.CONFLICT,
            "A inscrição fiscal já está cadastrada ou o registro ainda possui vínculos.", request, Map.of());
    }

    private Map<String, Object> error(HttpStatusCode status, String message, HttpServletRequest request,
                                      Map<String, Object> details) {
        return Map.of(
            "timestamp", Instant.now().toString(),
            "status", status.value(),
            "error", HttpStatus.valueOf(status.value()).getReasonPhrase(),
            "message", message,
            "details", details,
            "path", request.getRequestURI()
        );
    }
}
