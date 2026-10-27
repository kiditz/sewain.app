package app.sewain.settings;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JsonNodeMapper {

    private final ObjectMapper objectMapper;

    public JsonNode toJsonNode(Object value) {
        return objectMapper.valueToTree(value);
    }

    public <T> T toObject(JsonNode jsonNode, Class<T> type) {
        return objectMapper.convertValue(jsonNode, type);
    }
}