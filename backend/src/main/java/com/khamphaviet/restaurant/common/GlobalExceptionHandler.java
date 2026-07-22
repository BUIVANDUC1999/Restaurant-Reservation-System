package com.khamphaviet.restaurant.common;

import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> denied(AccessDeniedException ex) { return error(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này", Map.of()); }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, ConcurrencyFailureException.class})
    public ResponseEntity<?> concurrent(RuntimeException ex) {
        return error(HttpStatus.CONFLICT, "The data changed during this operation. Please reload and try again", Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> conflict(DataIntegrityViolationException ex) {
        return error(HttpStatus.CONFLICT, "The submitted data conflicts with an existing record", Map.of());
    }

    private ResponseEntity<?> error(HttpStatus status, String message, Map<String, String> fields) {
        return ResponseEntity.status(status).body(Map.of("timestamp", Instant.now(), "status", status.value(), "message", message, "fieldErrors", fields));
    }
}
