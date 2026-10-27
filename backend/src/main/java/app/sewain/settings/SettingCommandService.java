package app.sewain.settings;

import java.util.UUID;

interface SettingCommandService {

    SettingResponse create(SettingCommand command);

    SettingResponse update(UUID id, SettingCommand command);

    void delete(UUID id);
}