import type { Client } from '@stomp/stompjs';

export interface ProductImportStatus {
  id: string;
  status: 'QUEUED' | 'RUNNING' | 'COMPLETED' | 'FAILED';
  total: number;
  processed: number;
  succeeded: number;
  skipped: number;
  failed: number;
  message: string | null;
  version: number;
  errors: { row: number; status: string; message: string }[];
}

export function useProductImport() {
  const api = useApi();
  const config = useRuntimeConfig();
  const token = useCookie<string | null>('access_token');
  const status = ref<ProductImportStatus | null>(null);
  const connection = ref('CONNECTING');
  const error = ref('');
  const uploading = ref(false);
  const busy = computed(() => uploading.value || ['QUEUED', 'RUNNING'].includes(status.value?.status ?? ''));
  let client: Client | undefined;
  let disposed = false;
  let revision = 0;
  function accept(value: ProductImportStatus | null, replace = false) {
    if (!value || disposed) return;
    if (status.value?.id === value.id && value.version < status.value.version) return;
    if (!replace && status.value && status.value.id !== value.id) return;
    status.value = value;
  }
  async function snapshot() {
    const current = revision;
    try {
      const latest = await api<ProductImportStatus | null>('/products/imports/latest');
      if (current === revision) accept(latest, true);
    } catch { if (!disposed) error.value = 'Gagal mengambil status import.'; }
  }
  async function upload(file: File) {
    uploading.value = true; error.value = ''; revision++;
    try {
      const body = new FormData(); body.append('file', file);
      accept(await api<ProductImportStatus>('/products/imports', { method: 'POST', body }), true);
      // Recover events that arrived before the upload response assigned the job ID.
      await snapshot();
    } catch (cause) {
      error.value = useApiError().apiErrorMessage(cause);
    } finally { uploading.value = false; }
  }
  onMounted(async () => {
    const { Client } = await import('@stomp/stompjs');
    if (disposed) return;
    const url = new URL('/ws/provisioning', config.public.apiBaseUrl);
    url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:';
    client = new Client({
      brokerURL: url.href, reconnectDelay: 3000, connectionTimeout: 15000,
      heartbeatIncoming: 10000, heartbeatOutgoing: 10000,
      beforeConnect: async () => {
        if (!token.value) { connection.value = 'AUTH_REQUIRED'; await client?.deactivate(); return; }
        connection.value = 'CONNECTING';
        client!.connectHeaders = { Authorization: `Bearer ${token.value}` };
      },
      onConnect: () => {
        if (disposed) return;
        connection.value = 'CONNECTED';
        client!.subscribe('/user/queue/product-imports', message => {
          try { accept(JSON.parse(message.body) as ProductImportStatus); } catch { error.value = 'Status import tidak valid.'; }
        });
        // One snapshot per connection/reconnection; no polling timer.
        void snapshot();
      },
      onWebSocketClose: () => { if (!disposed) connection.value = 'DISCONNECTED'; },
      onStompError: () => { if (!disposed) { connection.value = 'AUTH_REQUIRED'; error.value = 'Sambungan progress ditolak. Coba masuk kembali.'; void client?.deactivate(); } },
    });
    client.activate();
    await snapshot();
  });
  onBeforeUnmount(() => { disposed = true; void client?.deactivate(); });
  return { status, connection, error, busy, uploading, upload, snapshot };
}
