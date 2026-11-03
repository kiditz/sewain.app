package app.sewain.storefront;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Locale;
import java.util.UUID;
@Service
@RequiredArgsConstructor
@Transactional
class StorefrontSubmissionCommandService {
    private final StorefrontSubmissionRepository repository;
    void submit(StorefrontSubmissionCommand command) {
        if (command.website() != null && !command.website().isBlank()) return;
        var name = command.name() == null ? "" : command.name().trim();
        var message = command.message() == null ? "" : command.message().trim();
        if (command.kind().equals("CONTACT") && (name.isBlank() || message.isBlank()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "storefrontForms.required");
        if (command.kind().equals("EMAIL_SIGNUP") && !command.consent())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "storefrontForms.consentRequired");
        repository.insert(command.requestId(),command.kind(),name,command.email().trim().toLowerCase(Locale.ROOT),message,command.consent());
    }
    void delete(UUID id) { repository.deleteById(id); }
}
