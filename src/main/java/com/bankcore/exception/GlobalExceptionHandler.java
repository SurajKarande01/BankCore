package com.bankcore.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorDetails> handleAccountNotFound(AccountNotFoundException ex, HttpServletRequest request) {
        String path = request != null ? request.getRequestURI() : "";
        ErrorDetails error = ErrorDetails.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(ex.getMessage())
                .path(path)
                .build();
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorDetails> handleInsufficientBalance(InsufficientBalanceException ex, HttpServletRequest request) {
        String path = request != null ? request.getRequestURI() : "";
        ErrorDetails error = ErrorDetails.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .error(HttpStatus.UNPROCESSABLE_ENTITY.getReasonPhrase())
                .message(ex.getMessage())
                .path(path)
                .build();
        return new ResponseEntity<>(error, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(AccountInactiveException.class)
    public ResponseEntity<ErrorDetails> handleAccountInactive(AccountInactiveException ex, HttpServletRequest request) {
        String path = request != null ? request.getRequestURI() : "";
        ErrorDetails error = ErrorDetails.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Account Inactive")
                .message(ex.getMessage())
                .path(path)
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidTransactionException.class)
    public ResponseEntity<ErrorDetails> handleInvalidTransaction(InvalidTransactionException ex, HttpServletRequest request) {
        String path = request != null ? request.getRequestURI() : "";
        ErrorDetails error = ErrorDetails.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Invalid Transaction")
                .message(ex.getMessage())
                .path(path)
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDetails> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        if (ex.getBindingResult() != null) {
            for (ObjectError err : ex.getBindingResult().getAllErrors()) {
                String fieldName = (err instanceof FieldError fe) ? fe.getField() : err.getObjectName();
                String message = err.getDefaultMessage();
                errors.put(fieldName, message != null ? message : "Invalid value");
            }
        }

        String path = request != null ? request.getRequestURI() : "";
        ErrorDetails error = ErrorDetails.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Request body has one or more validation errors")
                .path(path)
                .validationErrors(errors)
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetails> handleGlobalException(Exception ex, HttpServletRequest request) {
        String path = request != null ? request.getRequestURI() : "";
        log.error("Unhandled server exception encountered on path {}: ", path, ex);

        ErrorDetails error = ErrorDetails.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message(ex.getMessage() != null ? ex.getMessage() : "An unexpected server error occurred")
                .path(path)
                .build();
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

