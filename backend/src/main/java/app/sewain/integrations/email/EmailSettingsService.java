package app.sewain.integrations.email;

import app.sewain.exception.ValidationException;
import app.sewain.integrations.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
class EmailSettingsService {
    private final IntegrationInstallationRepository installations;
    private final IntegrationSecrets secrets;
    private final ObjectMapper mapper;

    record SaveCommand(boolean enabled, @NotNull @Valid EmailConfiguration configuration, String smtpPassword, String imapPassword) {
        @Override public String toString() { return "EmailSettingsSaveCommand[credentials redacted]"; }
    }
    record Response(boolean installed, boolean enabled, EmailConfiguration configuration, boolean smtpPasswordSet, boolean imapPasswordSet) {}
    record Credentials(String smtpPassword, String imapPassword) {}
    record Account(EmailConfiguration configuration, Credentials credentials) {}

    @Transactional(readOnly = true)
    public Response get() {
        IntegrationAccess.tenant();
        return installations.findByCode("email").map(this::response)
                .orElseGet(() -> new Response(false, false, EmailConfiguration.defaults(), false, false));
    }

    @Transactional
    public Response save(SaveCommand command) {
        IntegrationAccess.tenant();
        var installation = installations.findForUpdate("email").orElseGet(() -> {
            var created = new IntegrationInstallation(); created.setCode("email"); return created;
        });
        var previous = credentials(installation);
        var credentials = new Credentials(password(command.smtpPassword(), previous.smtpPassword()), password(command.imapPassword(), previous.imapPassword()));
        validate(command.configuration(), credentials, command.enabled());
        installation.setConfiguration(mapper.valueToTree(command.configuration()));
        var node = mapper.createObjectNode();
        node.put("smtpPassword", credentials.smtpPassword()); node.put("imapPassword", credentials.imapPassword());
        installation.setEncryptedSecrets(secrets.encrypt("email", node.toString()));
        installation.setEnabled(command.enabled());
        return response(installations.save(installation));
    }

    @Transactional
    public void disconnect() {
        IntegrationAccess.tenant();
        installations.findForUpdate("email").ifPresent(installations::delete);
    }

    @Transactional(readOnly = true)
    public Account account(boolean outbound) {
        IntegrationAccess.tenant();
        var installation = installations.findByCode("email").orElseThrow(() -> new ValidationException("integrations.email.notConfigured"));
        if (!installation.isEnabled()) throw new ValidationException("integrations.email.disabled");
        var configuration = configuration(installation);
        if (outbound ? !configuration.outboundEnabled() : !configuration.inboundEnabled())
            throw new ValidationException("integrations.email.directionDisabled");
        return new Account(configuration, credentials(installation));
    }

    private EmailConfiguration configuration(IntegrationInstallation installation) {
        return mapper.convertValue(installation.getConfiguration(), EmailConfiguration.class);
    }
    private Credentials credentials(IntegrationInstallation installation) {
        var node = mapper.readTree(secrets.decrypt("email", installation.getEncryptedSecrets()));
        return new Credentials(node.path("smtpPassword").asString(""), node.path("imapPassword").asString(""));
    }
    private Response response(IntegrationInstallation installation) {
        var credentials = credentials(installation);
        return new Response(true, installation.isEnabled(), configuration(installation), !credentials.smtpPassword().isEmpty(), !credentials.imapPassword().isEmpty());
    }
    private String password(String supplied, String previous) {
        if (supplied != null && supplied.length() > 4096) throw new ValidationException("integrations.email.invalidConfiguration");
        return supplied == null || supplied.isEmpty() ? previous : supplied;
    }
    private void validate(EmailConfiguration config, Credentials credentials, boolean enabled) {
        if (!enabled) return;
        if (!config.inboundEnabled() && !config.outboundEnabled()) throw new ValidationException("integrations.email.chooseDirection");
        if (config.outboundEnabled() && (blank(config.smtpHost()) || blank(config.smtpUsername()) || blank(config.fromAddress()) ||
                config.smtpSecurity() == null || credentials.smtpPassword().isEmpty())) throw new ValidationException("integrations.email.smtpRequired");
        if (config.inboundEnabled() && (blank(config.imapHost()) || blank(config.imapUsername()) || blank(config.imapFolder()) ||
                config.imapSecurity() == null || credentials.imapPassword().isEmpty())) throw new ValidationException("integrations.email.imapRequired");
        if (config.outboundEnabled()) noHeaderBreak(config.fromAddress(), config.fromName(), config.smtpHost(), config.smtpUsername());
        if (config.inboundEnabled()) noHeaderBreak(config.imapHost(), config.imapUsername(), config.imapFolder());
    }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private void noHeaderBreak(String... values) {
        for (var value : values) if (value != null && (value.contains("\r") || value.contains("\n")))
            throw new ValidationException("integrations.email.invalidConfiguration");
    }
}
