package app.sewain.settings;

import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

record SettingCommand(@NotNull SettingKey key, @NotNull JsonNode value) {
}