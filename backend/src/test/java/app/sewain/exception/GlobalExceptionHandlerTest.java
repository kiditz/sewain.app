package app.sewain.exception;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {
    @Test
    void mapsDomainErrorsToTheirHttpStatusAndPreservesErrorCode() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new ErrorController())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        for (var entry : java.util.Map.of("validation", 400, "missing", 404,
                "conflict", 409, "denied", 403, "internal", 500).entrySet()) {
            mvc.perform(get("/errors/" + entry.getKey()))
                    .andExpect(status().is(entry.getValue()))
                    .andExpect(jsonPath("$.status").value(entry.getValue()))
                    .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND));
        }
    }

    @RestController
    static class ErrorController {
        @GetMapping("/errors/{kind}")
        void fail(@org.springframework.web.bind.annotation.PathVariable("kind") String kind) {
            throw switch (kind) {
                case "validation" -> new ValidationException(ErrorCode.USER_NOT_FOUND);
                case "missing" -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND);
                case "conflict" -> new ConflictException(ErrorCode.USER_NOT_FOUND);
                case "denied" -> new AccessDeniedException(ErrorCode.USER_NOT_FOUND);
                default -> new IllegalStateException(ErrorCode.USER_NOT_FOUND);
            };
        }
    }
}
