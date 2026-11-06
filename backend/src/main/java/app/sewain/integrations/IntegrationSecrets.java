package app.sewain.integrations;

import app.sewain.exception.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.Base64;

/** AES-GCM binds credentials to both tenant and plugin. The key never goes into the database. */
@Component
public class IntegrationSecrets {
    private final String configuredKey;
    private final Path keyFile;
    private byte[] localKey;
    private final SecureRandom random = new SecureRandom();

    public IntegrationSecrets(@Value("${app.integrations.encryption-key:}") String configuredKey,
                              @Value("${app.integrations.key-file:data/integrations.key}") String keyFile) {
        this.configuredKey = configuredKey;
        this.keyFile = Path.of(keyFile);
    }

    private synchronized byte[] key() throws Exception {
        if (localKey == null) {
            if (!configuredKey.isBlank()) {
                localKey = Base64.getDecoder().decode(configuredKey);
            } else {
                Files.createDirectories(keyFile.toAbsolutePath().getParent());
                // A file lock prevents two local processes from observing a half-written key.
                try (var channel = java.nio.channels.FileChannel.open(keyFile,
                        java.util.Set.of(java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.READ, java.nio.file.StandardOpenOption.WRITE),
                        PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
                     var lock = channel.lock()) {
                    if (channel.size() == 0) {
                        var bytes = new byte[32]; random.nextBytes(bytes);
                        var encoded = ByteBuffer.wrap(Base64.getEncoder().encode(bytes));
                        while (encoded.hasRemaining()) channel.write(encoded);
                        channel.force(true);
                        localKey = bytes;
                    } else {
                        if (channel.size() > 128) throw new IllegalStateException();
                        var content = ByteBuffer.allocate((int) channel.size());
                        while (content.hasRemaining() && channel.read(content) != -1) { /* Read the complete key. */ }
                        localKey = Base64.getDecoder().decode(new String(content.array(), StandardCharsets.UTF_8).trim());
                    }
                }
            }
            if (localKey.length != 32) { localKey = null; throw new IllegalStateException(); }
        }
        return localKey;
    }

    public String encrypt(String plugin, String plaintext) {
        try {
            var nonce = new byte[12]; random.nextBytes(nonce);
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key(), "AES"), new GCMParameterSpec(128, nonce));
            cipher.updateAAD((IntegrationAccess.tenant() + ":" + plugin).getBytes(StandardCharsets.UTF_8));
            var encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(12 + encrypted.length).put(nonce).put(encrypted).array());
        } catch (Exception error) { throw new ValidationException("integrations.encryptionUnavailable"); }
    }

    public String decrypt(String plugin, String encrypted) {
        if (encrypted == null) return "{}";
        try {
            var buffer = ByteBuffer.wrap(Base64.getDecoder().decode(encrypted));
            var nonce = new byte[12]; buffer.get(nonce);
            var data = new byte[buffer.remaining()]; buffer.get(data);
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key(), "AES"), new GCMParameterSpec(128, nonce));
            cipher.updateAAD((IntegrationAccess.tenant() + ":" + plugin).getBytes(StandardCharsets.UTF_8));
            return new String(cipher.doFinal(data), StandardCharsets.UTF_8);
        } catch (Exception error) { throw new ValidationException("integrations.credentialsUnavailable"); }
    }
}
