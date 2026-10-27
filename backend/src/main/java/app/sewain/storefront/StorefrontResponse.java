package app.sewain.storefront;

import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;

record StorefrontResponse(JsonNode configuration, LocalDateTime updatedAt) {
}
