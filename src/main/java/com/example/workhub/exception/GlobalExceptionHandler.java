package com.example.workhub.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TicketConflictException.class)
    public ResponseEntity<String> handleTicketConflict(TicketConflictException exception) {
        return ResponseEntity.status(409).body(exception.getMessage());
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationException(
            MethodArgumentNotValidException exception) {

        FieldError error = exception.getBindingResult().getFieldError();

        if (error == null) {
            return ResponseEntity.badRequest().body("Validation failed");
        }

        return ResponseEntity.badRequest()
                .body(error.getField() + ": " + error.getDefaultMessage());
    }

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<String> handleTicketNotFound(
            TicketNotFoundException exception) {

        return ResponseEntity.status(404)
                .body(exception.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<String> handleUserNotFound(
            UserNotFoundException exception) {

        return ResponseEntity.status(400)
                .body(exception.getMessage());
    }

    @ExceptionHandler({InvalidStatusTransitionException.class,
        InvalidPriorityException.class}
    )
    public ResponseEntity<String> handleInvalidRequest(
            RuntimeException exception) {

        return ResponseEntity.status(400)
                .body(exception.getMessage());
    }
}