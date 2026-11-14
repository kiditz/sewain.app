package app.sewain.integrations.email;

import app.sewain.exception.*;
import app.sewain.integrations.*;
import app.sewain.tenant.TenantContext;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.junit.jupiter.api.*;
import org.springframework.data.domain.*;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.transaction.support.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class EmailWorkflowTest {
    final EmailSettingsService settings=mock(EmailSettingsService.class);
    final EmailTransport transport=mock(EmailTransport.class);
    final EmailMessageRepository messages=mock(EmailMessageRepository.class);
    final IntegrationInstallationRepository installations=mock(IntegrationInstallationRepository.class);
    final TransactionTemplate transactions=mock(TransactionTemplate.class);
    final EmailService service=new EmailService(settings,transport,messages,installations,transactions);
    final JavaMailSenderImpl sender=mock(JavaMailSenderImpl.class);
    final Store store=mock(Store.class);
    final Map<String,EmailMessage> saved=new HashMap<>();
    @BeforeEach void setup() throws Exception {
        TenantContext.set("shop");var account=new EmailSettingsService.Account(EmailConfiguration.defaults(),new EmailSettingsService.Credentials("smtp","imap"));
        when(settings.account(anyBoolean())).thenReturn(account);when(transport.sender(account)).thenReturn(sender);when(transport.store(account)).thenReturn(store);
        when(installations.findForUpdate("email")).thenReturn(Optional.of(new IntegrationInstallation()));
        when(transactions.execute(any())).thenAnswer(c->((TransactionCallback<?>)c.getArgument(0)).doInTransaction(null));
        when(messages.findByExternalKey(anyString())).thenAnswer(c->Optional.ofNullable(saved.get(c.getArgument(0))));
        when(messages.saveAndFlush(any())).thenAnswer(c->{EmailMessage m=c.getArgument(0);saved.put(m.getExternalKey(),m);return m;});
        when(messages.findById(any())).thenAnswer(c->saved.values().stream().filter(m->m.getId().equals(c.getArgument(0))).findFirst());
        when(transport.message(any(),any(),anyString(),anyString(),anyString())).thenReturn(mock(MimeMessage.class));
    }
    @AfterEach void clear(){TenantContext.clear();}
    Folder mailbox(List<Message> batch) throws Exception {
        var folder=mock(Folder.class,withSettings().extraInterfaces(UIDFolder.class));var uid=(UIDFolder)folder;
        when(store.getFolder("INBOX")).thenReturn(folder);when(folder.isOpen()).thenReturn(true);when(folder.getMessageCount()).thenReturn(batch.size());
        when(folder.getMessages(anyInt(),anyInt())).thenReturn(batch.toArray(Message[]::new));when(uid.getUIDValidity()).thenReturn(99L);
        when(uid.getUID(any())).thenAnswer(c->(long)batch.indexOf(c.getArgument(0))+1);
        return folder;
    }
    MimeMessage message(String body,boolean html) throws Exception {
        var mime=new MimeMessage(Session.getInstance(new Properties()));
        mime.setFrom(new InternetAddress("sender@example.invalid"));mime.setRecipients(Message.RecipientType.TO,"shop@example.invalid");
        mime.setSubject("Booking"); if(html)mime.setContent(body,"text/html; charset=UTF-8");else mime.setText(body,"UTF-8");mime.saveChanges();
        var result=spy(mime);doReturn(100).when(result).getSize();return result;
    }

    @Test void listEnforcesDirectionAndPageThenMapsMessages() {
        var m=new EmailMessage();m.setSubject("Booking");m.setDirection("INBOUND");m.setStatus("RECEIVED");
        when(messages.findByDirection(eq("INBOUND"),any(Pageable.class))).thenReturn(new PageImpl<>(List.of(m)));
        var result=service.list("INBOUND",0);assertEquals(1,result.total());assertEquals("Booking",result.items().getFirst().subject());
        assertThrows(ValidationException.class,()->service.list("OTHER",0));assertThrows(ValidationException.class,()->service.list("OUTBOUND",-1));
    }
    @Test void detailReturnsStoredBodyOrNotFound() {
        var m=new EmailMessage();m.setBody("Hello");saved.put("test",m);
        assertEquals("Hello",service.detail(m.getId()).body());assertThrows(ResourceNotFoundException.class,()->service.detail(UUID.randomUUID()));
    }
    @Test void connectionChecksUseReadOnlyImapAndCloseResources() throws Exception {
        var folder=mailbox(List.of());service.checkConnection(true);verify(sender).testConnection();
        service.checkConnection(false);verify(folder).open(Folder.READ_ONLY);verify(folder).close(false);verify(store).close();
    }
    @Test void connectionFailureReturnsSafeValidationError() throws Exception {
        doThrow(new MessagingException("password=secret")).when(sender).testConnection();
        var failure=assertThrows(ValidationException.class,()->service.checkConnection(true));assertFalse(failure.toString().contains("password=secret"));
    }
    @Test void successfulSendIsPersistedAndRequestIdCannotBeReusedForDifferentContent() {
        var id=UUID.randomUUID();var command=new EmailService.SendCommand(id,"customer@example.invalid","Booking","Hello");
        var result=service.send(command);assertEquals("SENT",result.status());assertEquals(result.id(),service.send(command).id());
        assertThrows(ValidationException.class,()->service.send(new EmailService.SendCommand(id,command.to(),"Other",command.body())));
        verify(sender,times(1)).send(any(MimeMessage.class));
    }
    @Test void headerInjectionAndMimeFailureNeverReachSmtp() throws Exception {
        assertThrows(ValidationException.class,()->service.send(new EmailService.SendCommand(UUID.randomUUID(),"customer@example.invalid\r\nInjected:x","Booking","Hello")));
        assertThrows(ValidationException.class,()->service.send(new EmailService.SendCommand(UUID.randomUUID(),"customer@example.invalid","Subject\nInjected","Hello")));
        when(transport.message(any(),any(),anyString(),anyString(),anyString())).thenThrow(new MessagingException("Invalid MIME"));
        assertThrows(ValidationException.class,()->service.send(new EmailService.SendCommand(UUID.randomUUID(),"customer@example.invalid","Booking","Hello")));
        verify(sender,never()).send(any(MimeMessage.class));
    }
    @Test void syncImportsPlainAndHtmlAsTextWithUtcReceivedDate() throws Exception {
        var plain=message("Hello café",false);var html=message("<h1>Booking</h1><p>Confirmed</p>",true);
        var at=Instant.parse("2026-01-01T00:00:00Z");doReturn(Date.from(at)).when(plain).getReceivedDate();
        var folder=mailbox(List.of(plain,html));var result=service.sync();assertEquals(2,result.imported());assertEquals(0,result.skipped());
        var capture=org.mockito.ArgumentCaptor.forClass(EmailMessage.class);verify(messages,times(2)).save(capture.capture());
        assertEquals("Hello café",capture.getAllValues().getFirst().getBody());assertEquals("Booking Confirmed",capture.getAllValues().get(1).getBody());
        assertEquals(LocalDateTime.ofInstant(at,ZoneOffset.UTC),capture.getAllValues().getFirst().getOccurredAt());
        assertEquals("RECEIVED",capture.getValue().getStatus());verify(folder).close(false);verify(messages).flush();
    }
    @Test void syncSkipsKnownUidsAndUnknownOrOversizedMessages() throws Exception {
        var duplicate=message("Duplicate",false);var unknown=message("Unknown",false);var large=message("Large",false);
        doReturn(-1).when(unknown).getSize();doReturn(2*1024*1024+1).when(large).getSize();mailbox(List.of(duplicate,unknown,large));
        when(messages.existsByExternalKey(endsWith(":1"))).thenReturn(true);
        var result=service.sync();assertEquals(0,result.imported());assertEquals(2,result.skipped());verify(messages,never()).save(any());
    }
    @Test void multipartAlternativeChoosesTextAndAttachmentsAreExcluded() throws Exception {
        var text=new MimeBodyPart();text.setText("Plain version","UTF-8");var html=new MimeBodyPart();html.setContent("<b>HTML version</b>","text/html");
        var multipart=new MimeMultipart("alternative");multipart.addBodyPart(text);multipart.addBodyPart(html);
        var mime=message("",false);mime.setContent(multipart);mime.saveChanges();mailbox(List.of(mime));
        service.sync();var capture=org.mockito.ArgumentCaptor.forClass(EmailMessage.class);verify(messages).save(capture.capture());assertEquals("Plain version",capture.getValue().getBody());
    }
    @Test void emptyInboxReturnsNoImportsAndUnsupportedImapStillClosesFolder() throws Exception {
        var folder=mailbox(List.of());assertEquals(0,service.sync().imported());verify(folder).close(false);
        var unsupported=mock(Folder.class);when(unsupported.isOpen()).thenReturn(true);when(store.getFolder("INBOX")).thenReturn(unsupported);
        assertThrows(ValidationException.class,service::sync);verify(unsupported).close(false);
    }
    @Test void missingInstallationAndTransportFailureDoNotExposeCredentials() throws Exception {
        when(installations.findForUpdate("email")).thenReturn(Optional.empty());assertThrows(ValidationException.class,service::sync);
        when(installations.findForUpdate("email")).thenReturn(Optional.of(new IntegrationInstallation()));
        when(transport.store(any())).thenThrow(new MessagingException("secret credentials"));
        var error=assertThrows(ValidationException.class,service::sync);assertFalse(error.toString().contains("secret credentials"));
    }
    @Test void mixedBodyOmitsAttachmentsAndNonTextParts() throws Exception {
        var text=new MimeBodyPart();text.setText("Visible message","UTF-8");
        var attachment=new MimeBodyPart();attachment.setText("Hidden attachment","UTF-8");attachment.setDisposition(Part.ATTACHMENT);
        var binary=new MimeBodyPart();binary.setContent(new byte[]{1,2},"application/octet-stream");
        var multipart=new MimeMultipart("mixed");multipart.addBodyPart(text);multipart.addBodyPart(attachment);multipart.addBodyPart(binary);
        var mime=message("",false);mime.setContent(multipart);mime.saveChanges();mailbox(List.of(mime));
        service.sync();var capture=org.mockito.ArgumentCaptor.forClass(EmailMessage.class);verify(messages).save(capture.capture());
        assertEquals("Visible message",capture.getValue().getBody());
    }
    @Test void unsupportedCharsetFallsBackToUtf8AndMissingEnvelopeIsSafe() throws Exception {
        var mime=mock(MimeMessage.class);when(mime.getSize()).thenReturn(100);when(mime.isMimeType("text/plain")).thenReturn(true);
        when(mime.getContentType()).thenReturn("text/plain; charset=x-invalid-charset");
        when(mime.getInputStream()).thenReturn(new java.io.ByteArrayInputStream("Hello café".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        mailbox(List.of(mime));service.sync();var capture=org.mockito.ArgumentCaptor.forClass(EmailMessage.class);verify(messages).save(capture.capture());
        assertEquals("Hello café",capture.getValue().getBody());assertEquals("",capture.getValue().getSender());assertEquals("",capture.getValue().getRecipient());assertEquals("",capture.getValue().getSubject());
    }
    @Test void largeMultipartBodyStopsAtLimitAndDoesNotAppendLaterParts() throws Exception {
        var first=new MimeBodyPart();first.setText("x".repeat(100000),"UTF-8");
        var later=new MimeBodyPart();later.setText("DO NOT APPEND","UTF-8");
        var multipart=new MimeMultipart("mixed");multipart.addBodyPart(first);multipart.addBodyPart(later);
        var mime=message("",false);mime.setContent(multipart);mime.saveChanges();mailbox(List.of(mime));service.sync();
        var capture=org.mockito.ArgumentCaptor.forClass(EmailMessage.class);verify(messages).save(capture.capture());assertEquals("x".repeat(100000),capture.getValue().getBody());
    }
    @Test void requestIdentityCannotBeReusedForAnotherRecipientOrBodyAndInstallationMustExist() {
        var id=UUID.randomUUID();service.send(new EmailService.SendCommand(id,"one@example.invalid","Subject","Body"));
        assertThrows(ValidationException.class,()->service.send(new EmailService.SendCommand(id,"two@example.invalid","Subject","Body")));
        assertThrows(ValidationException.class,()->service.send(new EmailService.SendCommand(id,"one@example.invalid","Subject","Changed")));
        when(installations.findForUpdate("email")).thenReturn(Optional.empty());
        assertThrows(ValidationException.class,()->service.send(new EmailService.SendCommand(UUID.randomUUID(),"one@example.invalid","Subject","Body")));
    }
}
