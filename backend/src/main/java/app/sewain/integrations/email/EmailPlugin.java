package app.sewain.integrations.email;

import app.sewain.integrations.OutboundEmailProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.stereotype.Component;
import java.util.Set;

@Component
@Validated
@RequiredArgsConstructor
class EmailPlugin implements OutboundEmailProvider {
    private final EmailService emailService;
    public Delivery send(OutboundEmail email) {
        var result = emailService.send(new EmailService.SendCommand(email.requestId(), email.to(), email.subject(), email.body()));
        return new Delivery(result.id(), result.status());
    }
    public String code() { return "email"; }
    public String name() { return "Email"; }
    public String description() { return "Connect your mailbox to send email over SMTP and receive email over IMAP."; }
    public Set<String> capabilities() { return Set.of("EMAIL_OUTBOUND", "EMAIL_INBOUND"); }
}
