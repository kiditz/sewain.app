package app.sewain.storefront;

import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

record StorefrontCommand(@NotNull JsonNode configuration) {
    public StorefrontCommand { configuration=app.sewain.shared.ContentSecurity.cleanConfiguration(configuration); }
}
