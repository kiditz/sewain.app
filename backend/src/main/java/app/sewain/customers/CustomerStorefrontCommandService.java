package app.sewain.customers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;
import java.util.UUID;

/** Resolve a store contact without changing an existing profile or account credentials. */
@Service
@RequiredArgsConstructor
public class CustomerStorefrontCommandService {
    private final CustomerRepository repository;

    @Transactional
    public UUID resolve(String name, String email, boolean emailSignup) {
        var normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        var existing = repository.findFirstByEmailIgnoreCaseOrderByCreatedAtAscIdAsc(normalizedEmail);
        if (existing.isPresent()) {
            if (emailSignup) repository.subscribeEmail(existing.get().getId());
            return existing.get().getId();
        }
        var fullName = name == null || name.isBlank() ? normalizedEmail : name.trim();
        repository.insertStorefrontContact(UUID.randomUUID(), fullName.substring(0, Math.min(150, fullName.length())), normalizedEmail,
                emailSignup ? "EMAIL_SIGNUP" : "CONTACT_FORM", emailSignup);
        // A concurrent request may already have inserted the same normalized email.
        var customer = repository.findFirstByEmailIgnoreCaseOrderByCreatedAtAscIdAsc(normalizedEmail).orElseThrow();
        if (emailSignup) repository.subscribeEmail(customer.getId());
        return customer.getId();
    }
}
