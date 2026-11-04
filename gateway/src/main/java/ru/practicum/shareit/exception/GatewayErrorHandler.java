package ru.practicum.shareit.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;

@RestControllerAdvice
public class GatewayErrorHandler {
    private final ObjectMapper objectMapper;

    public GatewayErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse("Ошибка валидации данных");
        return ResponseEntity.badRequest().body(new ErrorResponse(message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse("Некорректный формат тела запроса"));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException exception) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("Отсутствует обязательный заголовок: " + exception.getHeaderName()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException exception) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("Отсутствует обязательный параметр: " + exception.getParameterName()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException exception) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ErrorResponse("Метод не поддерживается"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMedia(HttpMediaTypeNotSupportedException exception) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(new ErrorResponse("Неподдерживаемый формат данных"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("Некорректное значение параметра: " + exception.getName()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode())
                .body(new ErrorResponse(exception.getReason() == null ? "Некорректный запрос" : exception.getReason()));
    }

    @ExceptionHandler(HttpStatusCodeException.class)
    public ResponseEntity<Object> handleServerError(HttpStatusCodeException exception) {
        String body = exception.getResponseBodyAsString();
        try {
            JsonNode json = objectMapper.readTree(body);
            return ResponseEntity.status(exception.getStatusCode()).body(json);
        } catch (IOException ignored) {
            return ResponseEntity.status(exception.getStatusCode())
                    .body(Map.of("error", body.isBlank() ? exception.getStatusText() : body));
        }
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ErrorResponse> handleServerUnavailable(ResourceAccessException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("Основной сервер ShareIt временно недоступен"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Непредвиденная ошибка: " + exception.getMessage()));
    }
}
