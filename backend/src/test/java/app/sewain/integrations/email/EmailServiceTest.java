package app.sewain.integrations.email;

import app.sewain.integrations.IntegrationInstallation;
import app.sewain.integrations.IntegrationInstallationRepository;
import app.sewain.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import jakarta.mail.internet.MimeMessage;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmailServiceTest {
    @AfterEach void clearTenant() { TenantContext.clear(); }
    @Test void smtpFailureIsRecordedAndHttpRetryDoesNotSendTwice() throws Exception {
        TenantContext.set("merchant_a");
        var settings = mock(EmailSettingsService.class);
        var transport = mock(EmailTransport.class);
        var messages = mock(EmailMessageRepository.class);
        var installations = mock(IntegrationInstallationRepository.class);
        var transactions = mock(TransactionTemplate.class);
        when(transactions.execute(any())).thenAnswer(invocation -> ((TransactionCallback<?>) invocation.getArgument(0)).doInTransaction(null));
        var config = EmailConfiguration.defaults();
        var account = new EmailSettingsService.Account(config, new EmailSettingsService.Credentials("password", ""));
        when(settings.account(true)).thenReturn(account);
        when(installations.findForUpdate("email")).thenReturn(Optional.of(new IntegrationInstallation()));
        var stored = new HashMap<String, EmailMessage>();
        when(messages.findByExternalKey(any())).thenAnswer(invocation -> Optional.ofNullable(stored.get(invocation.getArgument(0))));
        when(messages.saveAndFlush(any())).thenAnswer(invocation -> {
            EmailMessage message = invocation.getArgument(0); stored.put(message.getExternalKey(), message); return message;
        });
        when(messages.findById(any())).thenAnswer(invocation -> stored.values().stream().filter(message -> message.getId().equals(invocation.getArgument(0))).findFirst());
        var sender = mock(JavaMailSenderImpl.class);
        var mime = mock(MimeMessage.class);
        when(transport.sender(account)).thenReturn(sender);
        when(transport.message(any(), any(), any(), any(), any())).thenReturn(mime);
        doThrow(new MailSendException("timeout")).when(sender).send(mime);
        var service = new EmailService(settings, transport, messages, installations, transactions);
        var command = new EmailService.SendCommand(UUID.randomUUID(), "customer@example.com", "Booking", "Your booking");
        var first = service.send(command);
        var retry = service.send(command);
        assertEquals("UNKNOWN", first.status());
        assertEquals(first.id(), retry.id());
        verify(sender, times(1)).send(mime);
        verify(messages, times(1)).saveAndFlush(any());
    }
}
