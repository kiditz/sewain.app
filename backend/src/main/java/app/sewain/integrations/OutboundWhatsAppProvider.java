package app.sewain.integrations;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;

/** Other modules depend on this capability rather than the Node gateway. */
public interface OutboundWhatsAppProvider extends IntegrationPlugin {
    record Text(@NotNull UUID requestId, @NotBlank @Pattern(regexp="\\+?[1-9][0-9]{7,14}") String to,
                @NotBlank @Size(max=4096) String body) {}
    record Delivery(UUID id,String status) {}
    Delivery send(@NotNull @Valid Text text);
}
