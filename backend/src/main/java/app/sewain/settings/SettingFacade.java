package app.sewain.settings;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

@Service
@RequiredArgsConstructor
public class SettingFacade {
	private final SettingQueryService queryService;

	public JsonNode getValue(SettingKey key) {
		return queryService.findByKey(key).value();
	}
}
