package app.sewain.integrations;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/integrations")
@RequiredArgsConstructor
class IntegrationController {
    private final List<IntegrationPlugin> plugins;
    private final IntegrationInstallationRepository installations;
    record PluginResponse(String code, String name, String description, Set<String> capabilities, boolean installed, boolean enabled) {}

    @GetMapping
    @Transactional(readOnly = true)
    List<PluginResponse> list() {
        IntegrationAccess.tenant();
        return plugins.stream().map(plugin -> {
            var installation = installations.findByCode(plugin.code());
            return new PluginResponse(plugin.code(), plugin.name(), plugin.description(), plugin.capabilities(),
                    installation.isPresent(), installation.map(IntegrationInstallation::isEnabled).orElse(false));
        }).toList();
    }
}
