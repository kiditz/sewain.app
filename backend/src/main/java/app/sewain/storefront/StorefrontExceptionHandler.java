package app.sewain.storefront;

import app.sewain.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice(assignableTypes = {ThemeLibraryController.class, StorefrontController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
class StorefrontExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ErrorResponse> handle(ResponseStatusException exception, HttpServletRequest request) {
        var status = exception.getStatusCode();
        return ResponseEntity.status(status).body(new ErrorResponse(LocalDateTime.now(), status.value(),
                "Storefront error", exception.getReason(), request.getRequestURI(), Map.of()));
    }
}
