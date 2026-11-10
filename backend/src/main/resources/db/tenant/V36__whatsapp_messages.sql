CREATE TABLE integration_whatsapp_messages (
 id UUID PRIMARY KEY, session_id UUID NOT NULL,
 external_id VARCHAR(255), request_id UUID UNIQUE,
 direction VARCHAR(10) NOT NULL CHECK (direction IN ('INBOUND','OUTBOUND')),
 status VARCHAR(20) NOT NULL CHECK (status IN ('RECEIVED','SENDING','SENT','UNKNOWN')),
 peer VARCHAR(100) NOT NULL, body TEXT NOT NULL,
 occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE(session_id,external_id)
);
CREATE INDEX integration_whatsapp_messages_date ON integration_whatsapp_messages(occurred_at DESC);
