package app.sewain.integrations;

import app.sewain.exception.ValidationException;
import app.sewain.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

class IntegrationSecretsTest {
    @TempDir Path temporary;
    @AfterEach void clearTenant() { TenantContext.clear(); }
    @Test void credentialsSurviveRestartAndAreBoundToTenantAndPlugin() {
        TenantContext.set("merchant_a");
        var file = temporary.resolve("integrations.key").toString();
        var secrets = new IntegrationSecrets("", file);
        var encrypted = secrets.encrypt("email", "mailbox-password");
        assertFalse(encrypted.contains("mailbox-password"));
        assertNotEquals(encrypted, secrets.encrypt("email", "mailbox-password"));
        assertEquals("mailbox-password", new IntegrationSecrets("", file).decrypt("email", encrypted));
        assertThrows(ValidationException.class, () -> secrets.decrypt("another-plugin", encrypted));
        TenantContext.set("merchant_b");
        assertThrows(ValidationException.class, () -> secrets.decrypt("email", encrypted));
    }
    @Test void refusesModifiedCiphertextAndInvalidKeys() {
        TenantContext.set("merchant_a");
        var secrets = new IntegrationSecrets(Base64.getEncoder().encodeToString(new byte[32]), "unused");
        var bytes = Base64.getDecoder().decode(secrets.encrypt("email", "password"));
        bytes[bytes.length - 1] ^= 1;
        assertThrows(ValidationException.class, () -> secrets.decrypt("email", Base64.getEncoder().encodeToString(bytes)));
        assertThrows(ValidationException.class, () -> new IntegrationSecrets("invalid-key", "unused").encrypt("email", "password"));
    }
}
