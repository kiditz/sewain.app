package app.sewain.integrations.email;

import jakarta.validation.constraints.*;

record EmailConfiguration(
        boolean outboundEnabled,
        boolean inboundEnabled,
        @Email @Size(max = 254) String fromAddress,
        @Size(max = 100) String fromName,
        @Size(max = 253) String smtpHost,
        @Min(1) @Max(65535) int smtpPort,
        Security smtpSecurity,
        @Size(max = 254) String smtpUsername,
        @Size(max = 253) String imapHost,
        @Min(1) @Max(65535) int imapPort,
        Security imapSecurity,
        @Size(max = 254) String imapUsername,
        @Size(max = 255) String imapFolder
) {
    enum Security { SSL, STARTTLS }
    static EmailConfiguration defaults() {
        return new EmailConfiguration(false, false, "", "", "", 587, Security.STARTTLS, "", "", 993, Security.SSL, "", "INBOX");
    }
}
