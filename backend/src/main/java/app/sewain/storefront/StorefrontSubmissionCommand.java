package app.sewain.storefront;
import jakarta.validation.constraints.*;
import java.util.UUID;
record StorefrontSubmissionCommand(
    @NotNull UUID requestId,
    @NotBlank @Pattern(regexp = "CONTACT|EMAIL_SIGNUP") String kind,
    @Size(max = 120) String name,
    @NotBlank @Email @Size(max = 254) String email,
    @Size(max = 5000) String message,
    Boolean consent,
    @Size(max = 200) String website
) {}
