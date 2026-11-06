package app.sewain.integrations.email;

import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import java.util.Properties;

@Component
class EmailTransport {
    JavaMailSenderImpl sender(EmailSettingsService.Account account) {
        var config = account.configuration();
        var sender = new JavaMailSenderImpl();
        sender.setHost(config.smtpHost()); sender.setPort(config.smtpPort());
        sender.setUsername(config.smtpUsername()); sender.setPassword(account.credentials().smtpPassword());
        sender.setDefaultEncoding("UTF-8");
        var properties = properties("smtp", config.smtpSecurity());
        properties.setProperty("mail.smtp.auth", "true");
        sender.setJavaMailProperties(properties);
        return sender;
    }

    Store store(EmailSettingsService.Account account) throws Exception {
        var config = account.configuration();
        var store = Session.getInstance(properties("imap", config.imapSecurity())).getStore("imap");
        try {
            store.connect(config.imapHost(), config.imapPort(), config.imapUsername(), account.credentials().imapPassword());
            return store;
        } catch (Exception error) { try { store.close(); } catch (Exception ignored) {} throw error; }
    }

    MimeMessage message(JavaMailSenderImpl sender, EmailConfiguration config, String to, String subject, String body) throws Exception {
        var message = sender.createMimeMessage();
        var helper = new MimeMessageHelper(message, false, "UTF-8");
        var address = new InternetAddress(config.fromAddress(), config.fromName(), "UTF-8");
        address.validate(); helper.setFrom(address);
        var recipient = new InternetAddress(to, true); recipient.validate(); helper.setTo(recipient);
        helper.setSubject(subject); helper.setText(body, false);
        return message;
    }

    private Properties properties(String protocol, EmailConfiguration.Security security) {
        var properties = new Properties();
        var prefix = "mail." + protocol + ".";
        properties.setProperty(prefix + "connectiontimeout", "10000");
        properties.setProperty(prefix + "timeout", "15000");
        properties.setProperty(prefix + "writetimeout", "15000");
        properties.setProperty(prefix + "ssl.checkserveridentity", "true");
        properties.setProperty(prefix + "ssl.enable", Boolean.toString(security == EmailConfiguration.Security.SSL));
        properties.setProperty(prefix + "starttls.enable", Boolean.toString(security == EmailConfiguration.Security.STARTTLS));
        properties.setProperty(prefix + "starttls.required", Boolean.toString(security == EmailConfiguration.Security.STARTTLS));
        properties.setProperty("mail.imap.peek", "true");
        return properties;
    }
}
