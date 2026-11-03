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
    private final app.sewain.customers.CustomerStorefrontCommandService customers;
    void submit(StorefrontSubmissionCommand command) {
        if (command.website() != null && !command.website().isBlank()) return;
        var name = command.name() == null ? "" : command.name().trim();
        var message = command.message() == null ? "" : command.message().trim();
        if (command.kind().equals("CONTACT") && (name.isBlank() || message.isBlank()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "storefrontForms.required");
        if (command.kind().equals("EMAIL_SIGNUP") && !Boolean.TRUE.equals(command.consent()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "storefrontForms.consentRequired");
        var email = command.email().trim().toLowerCase(Locale.ROOT);
        int inserted = repository.insert(command.requestId(),command.kind(),name,email,message,Boolean.TRUE.equals(command.consent()));
        if (inserted == 0) {
            if (repository.existsById(command.requestId())) return;
            // A fresh signup for an existing email can renew consent without duplicating the contact.
            if (command.kind().equals("EMAIL_SIGNUP")) customers.resolve(name,email,true);
            return;
        }
        var customerId = customers.resolve(name,email,command.kind().equals("EMAIL_SIGNUP"));
        repository.assignCustomer(command.requestId(),customerId);
    }
    void delete(UUID id) { repository.deleteById(id); }
}
