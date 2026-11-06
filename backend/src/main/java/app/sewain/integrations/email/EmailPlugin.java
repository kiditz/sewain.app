package app.sewain.integrations.email;

import app.sewain.integrations.IntegrationPlugin;
import org.springframework.stereotype.Component;
import java.util.Set;

@Component
class EmailPlugin implements IntegrationPlugin {
    public String code() { return "email"; }
    public String name() { return "Email"; }
    public String description() { return "Connect your mailbox to send email over SMTP and receive email over IMAP."; }
    public Set<String> capabilities() { return Set.of("EMAIL_OUTBOUND", "EMAIL_INBOUND"); }
}
