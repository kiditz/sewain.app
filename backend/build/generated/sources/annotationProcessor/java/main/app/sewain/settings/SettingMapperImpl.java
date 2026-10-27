package app.sewain.settings;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-20T12:27:41+0700",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-java-compiler-worker-9.7.1.jar, environment: Java 21.0.12 (Amazon.com Inc.)"
)
@Component
class SettingMapperImpl implements SettingMapper {

    @Autowired
    private JsonNodeMapper jsonNodeMapper;

    @Override
    public List<SettingResponse> toResponses(Iterable<Setting> entity) {
        if ( entity == null ) {
            return null;
        }

        List<SettingResponse> list = new ArrayList<SettingResponse>();
        for ( Setting setting : entity ) {
            list.add( toResponse( setting ) );
        }

        return list;
    }

    @Override
    public List<SettingResponse> toResponses(List<Setting> entity) {
        if ( entity == null ) {
            return null;
        }

        List<SettingResponse> list = new ArrayList<SettingResponse>( entity.size() );
        for ( Setting setting : entity ) {
            list.add( toResponse( setting ) );
        }

        return list;
    }

    @Override
    public List<Setting> toEntities(List<SettingCommand> entity) {
        if ( entity == null ) {
            return null;
        }

        List<Setting> list = new ArrayList<Setting>( entity.size() );
        for ( SettingCommand settingCommand : entity ) {
            list.add( toEntity( settingCommand ) );
        }

        return list;
    }

    @Override
    public void update(SettingCommand command, Setting entity) {
        if ( command == null ) {
            return;
        }

        if ( command.key() != null ) {
            entity.setKey( command.key() );
        }
        if ( command.value() != null ) {
            entity.setValue( jsonNodeMapper.toJsonNode( command.value() ) );
        }
    }

    @Override
    public SettingResponse toResponse(Setting setting) {
        if ( setting == null ) {
            return null;
        }

        UUID id = null;
        SettingKey key = null;
        JsonNode value = null;
        LocalDateTime createdAt = null;
        LocalDateTime updatedAt = null;

        id = setting.getId();
        key = setting.getKey();
        value = jsonNodeMapper.toJsonNode( setting.getValue() );
        createdAt = setting.getCreatedAt();
        updatedAt = setting.getUpdatedAt();

        SettingResponse settingResponse = new SettingResponse( id, key, value, createdAt, updatedAt );

        return settingResponse;
    }

    @Override
    public Setting toEntity(SettingCommand request) {
        if ( request == null ) {
            return null;
        }

        Setting setting = new Setting();

        setting.setKey( request.key() );
        setting.setValue( jsonNodeMapper.toJsonNode( request.value() ) );

        return setting;
    }
}
