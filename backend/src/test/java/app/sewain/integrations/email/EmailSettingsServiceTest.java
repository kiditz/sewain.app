package app.sewain.integrations.email;

import app.sewain.exception.ValidationException;
import app.sewain.integrations.*;
import app.sewain.tenant.TenantContext;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class EmailSettingsServiceTest {
    final IntegrationInstallationRepository installations=mock(IntegrationInstallationRepository.class);
    final IntegrationSecrets secrets=mock(IntegrationSecrets.class);
    final ObjectMapper json=new ObjectMapper();
    final EmailSettingsService service=new EmailSettingsService(installations,secrets,json);
    @BeforeEach void setup() {
        TenantContext.set("test_store");
        when(secrets.decrypt(eq("email"),nullable(String.class))).thenReturn("{}");
        when(secrets.encrypt(eq("email"),anyString())).thenReturn("encrypted-credentials");
        when(installations.save(any())).thenAnswer(c->c.getArgument(0));
    }
    @AfterEach void clear() { TenantContext.clear(); }
    EmailConfiguration config(boolean out,boolean in,String host) {
        return new EmailConfiguration(out,in,"store@example.invalid","Store",host,587,EmailConfiguration.Security.STARTTLS,
                "smtp-user","imap.example.invalid",993,EmailConfiguration.Security.SSL,"imap-user","INBOX");
    }
    IntegrationInstallation installed(boolean enabled,EmailConfiguration config) {
        var installation=new IntegrationInstallation(); installation.setCode("email"); installation.setEnabled(enabled);
        installation.setConfiguration(json.valueToTree(config)); installation.setEncryptedSecrets("stored");
        when(installations.findByCode("email")).thenReturn(Optional.of(installation));
        when(installations.findForUpdate("email")).thenReturn(Optional.of(installation)); return installation;
    }
    @Test void uninstalledAccountReturnsSafeDefaults() {
        var response=service.get(); assertFalse(response.installed()); assertFalse(response.enabled());
        assertFalse(response.smtpPasswordSet()); assertFalse(response.imapPasswordSet());
    }
    @Test void installationRequiresTenantContext() {
        TenantContext.set("public"); assertThrows(AccessDeniedException.class,service::get);
        verifyNoInteractions(installations);
    }
    @Test void enabledOutboundAccountRequiresPasswordAndConfiguration() {
        assertThrows(ValidationException.class,()->service.save(new EmailSettingsService.SaveCommand(true,config(true,false,"smtp.example.invalid"),null,null)));
        assertThrows(ValidationException.class,()->service.save(new EmailSettingsService.SaveCommand(true,config(true,false," "),"password",null)));
        verify(installations,never()).save(any());
    }
    @Test void enabledInboundAccountRequiresImapPassword() {
        assertThrows(ValidationException.class,()->service.save(new EmailSettingsService.SaveCommand(true,config(false,true,"smtp.example.invalid"),null,null)));
    }
    @Test void enabledAccountNeedsAtLeastOneDirectionButDisabledDraftMayBeIncomplete() {
        assertThrows(ValidationException.class,()->service.save(new EmailSettingsService.SaveCommand(true,EmailConfiguration.defaults(),null,null)));
        assertTrue(service.save(new EmailSettingsService.SaveCommand(false,EmailConfiguration.defaults(),null,null)).installed());
    }
    @Test void headerInjectionAndOversizedPasswordAreRejected() {
        assertThrows(ValidationException.class,()->service.save(new EmailSettingsService.SaveCommand(true,config(true,false,"smtp\r\nInjected: value"),"secret",null)));
        assertThrows(ValidationException.class,()->service.save(new EmailSettingsService.SaveCommand(false,EmailConfiguration.defaults(),"x".repeat(4097),null)));
    }
    @Test void omittedPasswordsPreserveStoredCredentialsAndResponseOnlyExposesFlags() {
        installed(true,config(true,true,"smtp.example.invalid"));
        when(secrets.decrypt("email","stored")).thenReturn("{\"smtpPassword\":\"old-smtp\",\"imapPassword\":\"old-imap\"}");
        when(secrets.decrypt("email","encrypted-credentials")).thenReturn("{\"smtpPassword\":\"old-smtp\",\"imapPassword\":\"old-imap\"}");
        var response=service.save(new EmailSettingsService.SaveCommand(true,config(true,true,"smtp.example.invalid"),"",null));
        assertTrue(response.smtpPasswordSet()); assertTrue(response.imapPasswordSet());
        var capture=org.mockito.ArgumentCaptor.forClass(String.class); verify(secrets).encrypt(eq("email"),capture.capture());
        assertEquals("old-smtp",json.readTree(capture.getValue()).path("smtpPassword").asString());
        assertFalse(response.toString().contains("old-smtp"));
    }
    @Test void missingDisabledAndWrongDirectionAccountsCannotSend() {
        assertThrows(ValidationException.class,()->service.account(true));
        installed(false,config(true,true,"smtp.example.invalid")); assertThrows(ValidationException.class,()->service.account(true));
        installed(true,config(false,true,"smtp.example.invalid")); assertThrows(ValidationException.class,()->service.account(true));
        assertNotNull(service.account(false));
    }
    @Test void disconnectDeletesInstallationAndRepeatedDisconnectIsSafe() {
        var installation=installed(true,config(true,true,"smtp.example.invalid")); service.disconnect(); verify(installations).delete(installation);
        when(installations.findForUpdate("email")).thenReturn(Optional.empty()); service.disconnect(); verify(installations,times(1)).delete(any());
    }
    @Test void saveCommandNeverPrintsCredentials() {
        var command=new EmailSettingsService.SaveCommand(true,config(true,true,"smtp.example.invalid"),"smtp-secret","imap-secret");
        assertFalse(command.toString().contains("smtp-secret")); assertFalse(command.toString().contains("imap-secret"));
    }
}
