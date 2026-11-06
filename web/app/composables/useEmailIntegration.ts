export interface EmailConfiguration {
  outboundEnabled: boolean;
  inboundEnabled: boolean;
  fromAddress: string;
  fromName: string;
  smtpHost: string;
  smtpPort: number;
  smtpSecurity: "SSL" | "STARTTLS";
  smtpUsername: string;
  imapHost: string;
  imapPort: number;
  imapSecurity: "SSL" | "STARTTLS";
  imapUsername: string;
  imapFolder: string;
}
export interface EmailIntegration {
  installed: boolean;
  enabled: boolean;
  configuration: EmailConfiguration;
  smtpPasswordSet: boolean;
  imapPasswordSet: boolean;
}
export interface IntegrationPlugin {
  code: string;
  name: string;
  description: string;
  capabilities: string[];
  installed: boolean;
  enabled: boolean;
}
export interface IntegrationEmailMessage {
  id: string;
  direction: "INBOUND" | "OUTBOUND";
  status: "RECEIVED" | "SENDING" | "SENT" | "UNKNOWN";
  sender: string;
  recipient: string;
  subject: string;
  occurredAt: string;
}
export interface EmailMessagePage {
  items: IntegrationEmailMessage[];
  total: number;
  page: number;
  pages: number;
}
export const useEmailIntegration = () => {
  const api = useApi();
  const base = "/integrations/email";
  return {
    plugins: () => api<IntegrationPlugin[]>("/integrations"),
    get: () => api<EmailIntegration>(base),
    save: (settings: { enabled: boolean; configuration: EmailConfiguration; smtpPassword: string; imapPassword: string }) =>
      api<EmailIntegration>(base, { method: "PUT", body: settings }),
    disconnect: () => api(base, { method: "DELETE" }),
    check: (direction: "smtp" | "imap") => api(`${base}/check-${direction}`, { method: "POST" }),
    sync: () => api<{ imported: number; skipped: number }>(`${base}/sync`, { method: "POST" }),
    messages: (direction: "INBOUND" | "OUTBOUND", page = 0) => api<EmailMessagePage>(`${base}/messages`, { query: { direction, page } }),
    detail: (id: string) => api<{ message: IntegrationEmailMessage; body: string }>(`${base}/messages/${id}`),
    send: (body: { requestId: string; to: string; subject: string; body: string }) =>
      api<IntegrationEmailMessage>(`${base}/messages`, { method: "POST", body }),
  };
};
