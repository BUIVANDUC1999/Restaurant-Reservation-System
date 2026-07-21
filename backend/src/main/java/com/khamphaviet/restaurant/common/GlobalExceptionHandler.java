package com.khamphaviet.restaurant.common;

import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> business(BusinessException ex) { return error(HttpStatus.BAD_REQUEST, ex.getMessage(), Map.of()); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> validation(MethodArgumentNotValidException ex) {
        var fields = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(e -> e.getField(), e -> e.getDefaultMessage() == null ? "Không hợp lệ" : e.getDefaultMessage(), (a, b) -> a));
        return error(HttpStatus.BAD_REQUEST, "Dữ liệu gửi lên chưa hợp lệ", fields);
    }

    private ResponseEntity<?> error(HttpStatus status, String message, Map<String, String> fields) {
        return ResponseEntity.status(status).body(Map.of("timestamp", Instant.now(), "status", status.value(), "message", message, "fieldErrors", fields));
    }
}

