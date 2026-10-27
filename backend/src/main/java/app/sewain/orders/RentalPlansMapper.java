package app.sewain.orders;

import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
class RentalPlansMapper {

	private final ObjectMapper objectMapper;


	@Named("mapRentalPlans")
	List<Map<String, Object>> map(Object value) {
		if (value == null) {
			return List.of();
		}

		if (value instanceof List<?> list) {
			return list.stream()
					.filter(Map.class::isInstance)
					.map(item -> (Map<String, Object>) item)
					.toList();
		}

		return objectMapper.convertValue(
				value,
				new TypeReference<List<Map<String, Object>>>() {
				}
		);
	}
}