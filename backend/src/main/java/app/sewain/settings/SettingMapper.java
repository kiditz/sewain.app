package app.sewain.settings;

import app.sewain.shared.GlobalMapper;
import app.sewain.shared.GlobalMapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfig.class, uses = JsonNodeMapper.class)
interface SettingMapper extends GlobalMapper<Setting, SettingResponse, SettingCommand> {
	SettingResponse toResponse(Setting setting);
	Setting toEntity(SettingCommand request);
}