package app.sewain.settings;

import java.util.List;

interface SettingQueryService {

    List<SettingResponse> findAll();

    SettingResponse findByKey(SettingKey key);
}