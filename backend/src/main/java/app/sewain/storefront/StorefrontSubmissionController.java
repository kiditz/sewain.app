package app.sewain.storefront;
import app.sewain.shared.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController
@RequestMapping("/api/storefront/submissions")
@RequiredArgsConstructor
class StorefrontSubmissionController {
    private final StorefrontSubmissionCommandService commands;
    private final StorefrontSubmissionQueryService queries;
    @PostMapping @ResponseStatus(HttpStatus.ACCEPTED)
    void submit(@Valid @RequestBody StorefrontSubmissionCommand command) { commands.submit(command); }
    @GetMapping PageResponse<StorefrontSubmission> list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return queries.list(page,size); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) { commands.delete(id); }
}
