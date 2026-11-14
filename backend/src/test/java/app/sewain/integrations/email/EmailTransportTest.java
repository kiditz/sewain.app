package app.sewain.integrations.email;
import org.junit.jupiter.api.Test;
import jakarta.mail.internet.InternetAddress;
import static org.junit.jupiter.api.Assertions.*;
class EmailTransportTest {
 final EmailTransport transport=new EmailTransport();
 EmailConfiguration config(EmailConfiguration.Security security){return new EmailConfiguration(true,true,"shop@example.invalid","Rental Shop","smtp.example.invalid",587,security,"owner","imap.example.invalid",993,security,"owner","INBOX");}
 @Test void smtpConfigurationEnforcesTlsIdentityAndTimeouts(){for(var security:EmailConfiguration.Security.values()){var sender=transport.sender(new EmailSettingsService.Account(config(security),new EmailSettingsService.Credentials("secret","imap")));assertEquals("smtp.example.invalid",sender.getHost());assertEquals(587,sender.getPort());assertEquals("secret",sender.getPassword());assertEquals("UTF-8",sender.getDefaultEncoding());var p=sender.getJavaMailProperties();assertEquals("true",p.getProperty("mail.smtp.ssl.checkserveridentity"));assertEquals("10000",p.getProperty("mail.smtp.connectiontimeout"));assertEquals("15000",p.getProperty("mail.smtp.timeout"));assertEquals(Boolean.toString(security==EmailConfiguration.Security.SSL),p.getProperty("mail.smtp.ssl.enable"));assertEquals(Boolean.toString(security==EmailConfiguration.Security.STARTTLS),p.getProperty("mail.smtp.starttls.required"));assertEquals("true",p.getProperty("mail.imap.peek"));}}
 @Test void mimeMessagePreservesUnicodePlainTextAndSenderIdentity()throws Exception{var c=config(EmailConfiguration.Security.STARTTLS);var sender=transport.sender(new EmailSettingsService.Account(c,new EmailSettingsService.Credentials("secret","imap")));var mime=transport.message(sender,c,"customer@example.invalid","Booking café","Hello café <b>literal</b>");mime.saveChanges();assertEquals("Booking café",mime.getSubject());assertEquals("Hello café <b>literal</b>",mime.getContent());assertEquals("shop@example.invalid",((InternetAddress)mime.getFrom()[0]).getAddress());assertEquals("Rental Shop",((InternetAddress)mime.getFrom()[0]).getPersonal());assertEquals("customer@example.invalid",((InternetAddress)mime.getAllRecipients()[0]).getAddress());assertTrue(mime.isMimeType("text/plain"));}
 @Test void malformedRecipientIsRejectedBeforeAnyNetworkCall(){var c=config(EmailConfiguration.Security.SSL);var sender=transport.sender(new EmailSettingsService.Account(c,new EmailSettingsService.Credentials("secret","imap")));assertThrows(Exception.class,()->transport.message(sender,c,"bad@@address","Booking","Body"));}
 @Test void imapConnectionUsesCredentialsAndClosesStoreOnFailure()throws Exception {
  var session=org.mockito.Mockito.mock(jakarta.mail.Session.class);var store=org.mockito.Mockito.mock(jakarta.mail.Store.class);
  try(var sessions=org.mockito.Mockito.mockStatic(jakarta.mail.Session.class)) {
   sessions.when(()->jakarta.mail.Session.getInstance(org.mockito.ArgumentMatchers.any(java.util.Properties.class))).thenReturn(session);
   org.mockito.Mockito.when(session.getStore("imap")).thenReturn(store);
   var account=new EmailSettingsService.Account(config(EmailConfiguration.Security.SSL),new EmailSettingsService.Credentials("smtp-secret","imap-secret"));
   assertSame(store,transport.store(account));org.mockito.Mockito.verify(store).connect("imap.example.invalid",993,"owner","imap-secret");
   org.mockito.Mockito.doThrow(new jakarta.mail.MessagingException("Connection failed")).when(store).connect("imap.example.invalid",993,"owner","imap-secret");
   assertThrows(jakarta.mail.MessagingException.class,()->transport.store(account));org.mockito.Mockito.verify(store).close();
   org.mockito.Mockito.doThrow(new jakarta.mail.MessagingException("Close failed")).when(store).close();
   assertThrows(jakarta.mail.MessagingException.class,()->transport.store(account));
  }
 }
}
