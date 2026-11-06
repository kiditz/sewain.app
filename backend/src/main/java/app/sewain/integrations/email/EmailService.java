package app.sewain.integrations.email;

import app.sewain.exception.ResourceNotFoundException;
import app.sewain.exception.ValidationException;
import app.sewain.integrations.IntegrationAccess;
import app.sewain.integrations.IntegrationInstallationRepository;
import jakarta.mail.*;
import jakarta.mail.internet.ContentType;
import jakarta.mail.internet.MimeUtility;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
@RequiredArgsConstructor
class EmailService {
    private final EmailSettingsService settings;
    private final EmailTransport transport;
    private final EmailMessageRepository messages;
    private final IntegrationInstallationRepository installations;
    private final TransactionTemplate transactions;

    record SendCommand(@NotNull UUID requestId, @NotBlank @Email @Size(max = 254) String to,
                       @NotBlank @Size(max = 255) String subject, @NotBlank @Size(max = 100000) String body) {}
    record Summary(UUID id, String direction, String status, String sender, String recipient, String subject, LocalDateTime occurredAt) {}
    record Detail(Summary message, String body) {}
    record MessagePage(List<Summary> items, long total, int page, int pages) {}
    record SyncResult(int imported, int skipped) {}

    MessagePage list(String direction, int page) {
        IntegrationAccess.tenant();
        if (!Set.of("INBOUND", "OUTBOUND").contains(direction) || page < 0) throw new ValidationException("integrations.email.invalidRequest");
        var result = messages.findByDirection(direction, PageRequest.of(page, 25, Sort.by("occurredAt").descending().and(Sort.by("id"))));
        return new MessagePage(result.map(this::summary).getContent(), result.getTotalElements(), page, result.getTotalPages());
    }
    Detail detail(UUID id) {
        IntegrationAccess.tenant();
        var message = messages.findById(id).orElseThrow(() -> new ResourceNotFoundException("integrations.email.messageNotFound"));
        return new Detail(summary(message), message.getBody());
    }
    Summary summary(EmailMessage message) {
        return new Summary(message.getId(), message.getDirection(), message.getStatus(), message.getSender(), message.getRecipient(), message.getSubject(), message.getOccurredAt());
    }

    void checkConnection(boolean outbound) {
        var account = settings.account(outbound);
        try {
            if (outbound) transport.sender(account).testConnection();
            else try (var store = transport.store(account)) {
                var folder = store.getFolder(account.configuration().imapFolder());
                try { folder.open(Folder.READ_ONLY); } finally { if (folder.isOpen()) folder.close(false); }
            }
        } catch (Exception error) { throw new ValidationException("integrations.email.connectionFailed"); }
    }

    Summary send(SendCommand command) {
        var account = settings.account(true);
        if (command.to().contains("\r") || command.to().contains("\n") || command.subject().contains("\r") || command.subject().contains("\n"))
            throw new ValidationException("integrations.email.invalidRequest");
        var sender = transport.sender(account);
        final jakarta.mail.internet.MimeMessage mime;
        try { mime = transport.message(sender, account.configuration(), command.to(), command.subject(), command.body()); }
        catch (Exception error) { throw new ValidationException("integrations.email.invalidRequest"); }
        var created = new boolean[]{false};
        // Persist before SMTP. A repeated request ID must never send the message twice.
        var stored = transactions.execute(status -> {
            installations.findForUpdate("email").orElseThrow(() -> new ValidationException("integrations.email.notConfigured"));
            var existing = messages.findByExternalKey("outbound:" + command.requestId());
            if (existing.isPresent()) {
                var previous = existing.get();
                if (!previous.getRecipient().equals(command.to()) || !previous.getSubject().equals(command.subject()) || !previous.getBody().equals(command.body()))
                    throw new ValidationException("integrations.email.requestAlreadyUsed");
                return previous;
            }
            var message = new EmailMessage(); message.setExternalKey("outbound:" + command.requestId());
            message.setDirection("OUTBOUND"); message.setStatus("SENDING");
            message.setSender(account.configuration().fromAddress()); message.setRecipient(command.to());
            message.setSubject(command.subject()); message.setBody(command.body());
            created[0] = true;
            return messages.saveAndFlush(message);
        });
        if (!created[0]) return summary(Objects.requireNonNull(stored));
        String deliveryStatus;
        try { sender.send(mime); deliveryStatus = "SENT"; }
        catch (Exception error) { deliveryStatus = "UNKNOWN"; }
        var finalStatus = deliveryStatus;
        return transactions.execute(status -> {
            var message = messages.findById(Objects.requireNonNull(stored).getId()).orElseThrow();
            message.setStatus(finalStatus); messages.save(message); return summary(message);
        });
    }

    SyncResult sync() {
        var account = settings.account(false);
        // Serialize mailbox imports per tenant. IMAP remains read-only and never marks mail as read.
        return transactions.execute(status -> {
            installations.findForUpdate("email").orElseThrow(() -> new ValidationException("integrations.email.notConfigured"));
            try (var store = transport.store(account)) {
                var folder = store.getFolder(account.configuration().imapFolder());
                try {
                    folder.open(Folder.READ_ONLY);
                    if (!(folder instanceof UIDFolder uidFolder)) throw new ValidationException("integrations.email.imapUnsupported");
                    var config = account.configuration();
                    var identity = config.imapHost().toLowerCase(Locale.ROOT) + ":" + config.imapPort() + ":" + config.imapUsername() + ":" + config.imapFolder();
                    var mailbox = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(identity.getBytes(StandardCharsets.UTF_8)));
                    int imported = 0, skipped = 0;
                    int count = folder.getMessageCount();
                    if (count == 0) return new SyncResult(0, 0);
                    var batch = folder.getMessages(Math.max(1, count - 49), count);
                    var profile = new FetchProfile(); profile.add(FetchProfile.Item.ENVELOPE); profile.add(FetchProfile.Item.SIZE); profile.add(UIDFolder.FetchProfileItem.UID);
                    folder.fetch(batch, profile);
                    for (var message : batch) {
                        var key = "inbound:" + mailbox + ":" + uidFolder.getUIDValidity() + ":" + uidFolder.getUID(message);
                        if (messages.existsByExternalKey(key)) continue;
                        if (message.getSize() < 0 || message.getSize() > 2 * 1024 * 1024) { skipped++; continue; }
                        var saved = new EmailMessage(); saved.setExternalKey(key); saved.setDirection("INBOUND"); saved.setStatus("RECEIVED");
                        saved.setSender(cut(addresses(message.getFrom()), 512));
                        saved.setRecipient(cut(addresses(message.getAllRecipients()), 512));
                        saved.setSubject(cut(Optional.ofNullable(message.getSubject()).orElse(""), 255));
                        saved.setBody(body(message, 0, new int[]{0}));
                        var date = message.getReceivedDate();
                        if (date != null) saved.setOccurredAt(LocalDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC));
                        messages.save(saved); imported++;
                    }
                    messages.flush();
                    return new SyncResult(imported, skipped);
                } finally { if (folder.isOpen()) folder.close(false); }
            } catch (ValidationException error) { throw error; }
            catch (Exception error) { throw new ValidationException("integrations.email.syncFailed"); }
        });
    }

    private String body(Part part, int depth, int[] parts) throws Exception {
        if (depth > 8 || ++parts[0] > 50 || Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) return "";
        if (part.isMimeType("text/plain") || part.isMimeType("text/html")) {
            var type = new ContentType(part.getContentType());
            var charset = type.getParameter("charset");
            Charset encoding;
            try { encoding = charset == null ? StandardCharsets.UTF_8 : Charset.forName(MimeUtility.javaCharset(charset)); }
            catch (IllegalArgumentException error) { encoding = StandardCharsets.UTF_8; }
            try (var input = part.getInputStream()) {
                var text = new String(input.readNBytes(128 * 1024), encoding);
                return cut(part.isMimeType("text/html") ? Jsoup.parse(text).text() : text, 100000);
            }
        }
        if (part.isMimeType("multipart/*") && part.getContent() instanceof Multipart multipart) {
            var text = new StringBuilder();
            for (int i = 0; i < multipart.getCount() && i < 50; i++) {
                var value = body(multipart.getBodyPart(i), depth + 1, parts);
                if (part.isMimeType("multipart/alternative") && !value.isBlank()) return value;
                text.append(value).append('\n');
                if (text.length() > 100000) break;
            }
            return cut(text.toString().trim(), 100000);
        }
        return "";
    }
    private String addresses(Address[] addresses) { return addresses == null ? "" : Arrays.stream(addresses).map(Object::toString).collect(java.util.stream.Collectors.joining(", ")); }
    private String cut(String value, int limit) { return value.substring(0, Math.min(value.length(), limit)); }
}
