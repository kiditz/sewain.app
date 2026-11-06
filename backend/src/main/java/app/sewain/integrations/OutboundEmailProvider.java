package app.sewain.integrations;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;

/** Capability used by other modules without depending on SMTP or a particular provider. */
public interface OutboundEmailProvider extends IntegrationPlugin {
    record OutboundEmail(@NotNull UUID requestId, @NotBlank @Email @Size(max = 254) String to,
                         @NotBlank @Size(max = 255) String subject, @NotBlank @Size(max = 100000) String body) {}
    record Delivery(UUID id, String status) {}
    Delivery send(@NotNull @Valid OutboundEmail email);
}
