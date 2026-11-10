import type { Client } from '@stomp/stompjs';

export interface WhatsAppStatus {
  configured: boolean;
  installed: boolean;
  enabled: boolean;
  sessionId: string | null;
  state: string;
  qr: string | null;
  phone: string | null;
}
export interface WhatsAppMessage {
  id: string;
  direction: 'INBOUND' | 'OUTBOUND';
  status: 'RECEIVED' | 'SENDING' | 'SENT' | 'UNKNOWN';
  peer: string;
  body: string;
  occurredAt: string;
}

export function useWhatsAppIntegration() {
  const api = useApi();
  const config = useRuntimeConfig();
  const token = useCookie<string | null>('access_token');
  const merchant = useMerchantStore();
  const base = '/integrations/whatsapp';
  const status = ref<WhatsAppStatus | null>(null);
  const messages = ref<WhatsAppMessage[]>([]);
  const total = ref(0);
  const page = ref(0);
  const loading = ref(false);
  const busy = ref(false);
  const error = ref('');
  const disconnectFailed = ref(false);
  const connection = ref('CONNECTING');
  let client: Client | undefined;
  let disposed = false;
  let dirty = false;
  let generation = 0;
  const failure = (cause: unknown) => {
    const message = (cause as { data?: { message?: unknown } })?.data?.message;
    error.value = typeof message === 'string' ? message : 'Integrasi WhatsApp tidak dapat diakses.';
  };
  async function refresh() {
    if (disposed) return;
    if (loading.value) { dirty = true; return; }
    loading.value = true;
    const current = generation;
    try {
      const [snapshot, history] = await Promise.all([
        api<WhatsAppStatus>(base),
        api<{ items: WhatsAppMessage[]; total: number }>(`${base}/messages`, { query: { page: page.value } }),
      ]);
      if (!disposed && generation === current) { status.value = snapshot; messages.value = history.items; total.value = history.total; }
    } catch (cause) { if (!disposed && generation === current) failure(cause); }
    finally {
      loading.value = false;
      if (dirty && !disposed) { dirty = false; void refresh(); }
    }
  }
  async function connect() {
    busy.value = true; error.value = '';
    try { await api(`${base}/connect`, { method: 'POST' }); disconnectFailed.value = false; await refresh(); }
    catch (cause) { failure(cause); await refresh(); }
    finally { busy.value = false; }
  }
  async function disconnect() {
    busy.value = true; error.value = '';
    try { await api(base, { method: 'DELETE' }); disconnectFailed.value = false; }
    catch (cause) { disconnectFailed.value = true; failure(cause); }
    finally { await refresh(); busy.value = false; }
  }
  async function send(text: { requestId: string; to: string; body: string }) {
    busy.value = true; error.value = '';
    try {
      const result = await api<WhatsAppMessage>(`${base}/messages`, { method: 'POST', body: text });
      page.value = 0; await refresh(); return result;
    } catch (cause) { failure(cause); return null; }
    finally { busy.value = false; }
  }
  watch(page, () => { void refresh(); });
  watch(() => merchant.current?.id, () => {
    generation++; disconnectFailed.value = false; status.value = null; messages.value = []; total.value = 0; page.value = 0; error.value = ''; void refresh();
  });
  onMounted(async () => {
    void refresh();
    const { Client } = await import('@stomp/stompjs');
    if (disposed) return;
    const url = new URL('/ws/provisioning', config.public.apiBaseUrl);
    url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:';
    client = new Client({
      brokerURL: url.href, reconnectDelay: 3000, connectionTimeout: 15000,
      heartbeatIncoming: 10000, heartbeatOutgoing: 10000,
      beforeConnect: async () => {
        if (!token.value) { connection.value = 'AUTH_REQUIRED'; await client?.deactivate(); return; }
        connection.value = 'CONNECTING'; client!.connectHeaders = { Authorization: `Bearer ${token.value}` };
      },
      onConnect: () => {
        if (disposed) return;
        connection.value = 'CONNECTED';
        client!.subscribe('/user/queue/whatsapp', message => {
          try {
            const event = JSON.parse(message.body) as { sessionId?: string };
            if (event.sessionId === status.value?.sessionId || !status.value) void refresh();
          } catch { /* An invalid notification never changes local data. */ }
        });
        void refresh(); // Snapshot after refresh/reconnect; no polling.
      },
      onWebSocketClose: () => { if (!disposed) connection.value = 'DISCONNECTED'; },
      onStompError: () => { if (!disposed) { connection.value = 'AUTH_REQUIRED'; void client?.deactivate(); } },
    });
    client.activate();
  });
  onBeforeUnmount(() => { disposed = true; generation++; void client?.deactivate(); });
  return { status, disconnectFailed, messages, total, page, loading, busy, error, connection, refresh, connect, disconnect, send };
}
