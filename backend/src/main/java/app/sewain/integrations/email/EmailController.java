package app.sewain.integrations.email;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/integrations/email")
@RequiredArgsConstructor
class EmailController {
    private final EmailSettingsService settings;
    private final EmailService email;
    @GetMapping EmailSettingsService.Response configuration() { return settings.get(); }
    @PutMapping EmailSettingsService.Response save(@Valid @RequestBody EmailSettingsService.SaveCommand command) { return settings.save(command); }
    @DeleteMapping @ResponseStatus(HttpStatus.NO_CONTENT) void disconnect() { settings.disconnect(); }
    @PostMapping("/check-smtp") @ResponseStatus(HttpStatus.NO_CONTENT) void checkSmtp() { email.checkConnection(true); }
    @PostMapping("/check-imap") @ResponseStatus(HttpStatus.NO_CONTENT) void checkImap() { email.checkConnection(false); }
    @PostMapping("/sync") EmailService.SyncResult sync() { return email.sync(); }
    @PostMapping("/messages") EmailService.Summary send(@Valid @RequestBody EmailService.SendCommand command) { return email.send(command); }
    @GetMapping("/messages") EmailService.MessagePage list(@RequestParam(defaultValue = "INBOUND") String direction, @RequestParam(defaultValue = "0") int page) { return email.list(direction, page); }
    @GetMapping("/messages/{id}") EmailService.Detail detail(@PathVariable UUID id) { return email.detail(id); }
}
