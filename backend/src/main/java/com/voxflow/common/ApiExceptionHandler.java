package com.voxflow.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import com.voxflow.assistant.security.OperationCancelledException;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException exception) {
        return ResponseEntity
                .status(exception.getStatusCode())
                .body(new ErrorResponse(
                        Instant.now(),
                        exception.getStatusCode().value(),
                        exception.getReason() != null ? exception.getReason() : "Request failed."
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Invalid request.");

        return badRequest(message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException exception) {
        return badRequest("Malformed or unreadable request body.");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException exception) {
        return badRequest("Required request parameter is missing.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return badRequest("Invalid request parameter.");
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingPart(MissingServletRequestPartException exception) {
        return badRequest("Required request part is missing.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException exception) {
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ErrorResponse(
                        Instant.now(),
                        HttpStatus.PAYLOAD_TOO_LARGE.value(),
                        "Uploaded file is too large. Maximum size is 25MB."
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException exception) {
        return badRequest(exception.getMessage() != null ? exception.getMessage() : "Invalid request.");
    }

    @ExceptionHandler(OperationCancelledException.class)
    public ResponseEntity<ErrorResponse> handleCancelled(OperationCancelledException exception) {
        return ResponseEntity
                .status(499)
                .body(new ErrorResponse(
                        Instant.now(),
                        499,
                        "Operation cancelled."
                ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException exception) {
        log.error("Request failed", exception);
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse(
                        Instant.now(),
                        HttpStatus.BAD_GATEWAY.value(),
                        exception.getMessage() != null
                                ? exception.getMessage()
                                : "External service request failed."
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception exception) {
        log.error("Unexpected request failure", exception);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        Instant.now(),
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "An unexpected server error occurred."
                ));
    }

    private ResponseEntity<ErrorResponse> badRequest(String message) {
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(Instant.now(), HttpStatus.BAD_REQUEST.value(), message));
    }

    public record ErrorResponse(Instant timestamp, int status, String message, String error) {
        public ErrorResponse(Instant timestamp, int status, String message) {
            this(timestamp, status, message, message);
        }
    }
}
