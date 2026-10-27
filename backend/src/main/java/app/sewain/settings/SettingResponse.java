package app.sewain.settings;

import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.UUID;

record SettingResponse(
    UUID id,
    SettingKey key,
    JsonNode value,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}