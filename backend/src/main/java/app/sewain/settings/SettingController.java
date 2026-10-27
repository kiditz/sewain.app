package app.sewain.settings;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
class SettingController {

    private final SettingCommandService commandService;
    private final SettingQueryService queryService;

    @GetMapping
    List<SettingResponse> findAll() {
        return queryService.findAll();
    }

    @GetMapping("/{key}")
    SettingResponse findByKey(@PathVariable SettingKey key) {
        return queryService.findByKey(key);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    SettingResponse create(
          @Valid @RequestBody SettingCommand command
    ) {
        return commandService.create(command);
    }

    @PutMapping("/{id}")
    SettingResponse update(
          @PathVariable UUID id,
          @Valid @RequestBody SettingCommand command
    ) {
        return commandService.update(id, command);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        commandService.delete(id);
    }
}