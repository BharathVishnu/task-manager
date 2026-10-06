package taskamanager.backend.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiError> app(AppException e) {
        return ResponseEntity.status(e.getStatus())
                .body(new ApiError(e.getCode(), e.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException e) {
        Map<String, String> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(fe -> fields.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        return ResponseEntity.badRequest()
                .body(new ApiError("VALIDATION_FAILED", "Invalid input", fields));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
                .body(new ApiError("BAD_REQUEST", "Malformed or invalid JSON body", null));
    }

    /** Hibernate's @Version check failed: someone else saved between our load and flush. */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> optimistic(ObjectOptimisticLockingFailureException e) {
        return ResponseEntity.status(409)
                .body(new ApiError("VERSION_CONFLICT", "Changed by someone else. Reload and try again.", null));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> integrity(DataIntegrityViolationException e) {
        log.warn("Data integrity violation", e);
        return ResponseEntity.status(409)
                .body(new ApiError("CONFLICT", "Conflicts with existing data", null)); // no SQL text leaked
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> other(Exception e) {
        // Spring's own web errors (404 no route, 405, etc.) implement ErrorResponse: keep their status
        if (e instanceof ErrorResponse er) {
            int s = er.getStatusCode().value();
            return ResponseEntity.status(s).body(new ApiError("HTTP_" + s, "Request failed", null));
        }
        log.error("Unexpected error", e);
        return ResponseEntity.status(500)
                .body(new ApiError("INTERNAL_ERROR", "Something went wrong", null));
    }
}