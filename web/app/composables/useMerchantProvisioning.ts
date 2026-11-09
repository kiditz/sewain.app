import type { Client } from "@stomp/stompjs";

export interface ProvisioningStatus {
  merchantId: string;
  status: "PROVISIONING" | "READY" | "FAILED";
  version: number;
}

/** Snapshot requests happen only on STOMP connection/reconnection, never on a polling timer. */
export function useMerchantProvisioning(merchantId: string, receive: (status: ProvisioningStatus) => void) {
  const config = useRuntimeConfig();
  const token = useCookie<string | null>("access_token");
  const connection = ref<"CONNECTING" | "CONNECTED" | "DISCONNECTED" | "AUTH_REQUIRED">("CONNECTING");
  const error = ref(false);
  let client: Client | undefined;
  let disposed = false;
  let version = -1;
  const send = (action: "status" | "retry") => {
    if (client?.connected) client.publish({ destination: `/app/provisioning/${action}`, body: JSON.stringify({ merchantId }) });
  };
  onMounted(async () => {
    const { Client, ReconnectionTimeMode } = await import("@stomp/stompjs");
    if (disposed) return;
    const url = new URL("/ws/provisioning", config.public.apiBaseUrl);
    url.protocol = url.protocol === "https:" ? "wss:" : "ws:";
    client = new Client({
      brokerURL: url.href,
      reconnectDelay: 1000,
      maxReconnectDelay: 30000,
      reconnectTimeMode: ReconnectionTimeMode.EXPONENTIAL,
      connectionTimeout: 15000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      // Tokens are sent in STOMP CONNECT headers, not in the URL or debug logs.
      beforeConnect: async () => {
        if (!token.value) {
          connection.value = "AUTH_REQUIRED";
          await client?.deactivate();
          return;
        }
        connection.value = "CONNECTING";
        client!.connectHeaders = { Authorization: `Bearer ${token.value}` };
      },
      onConnect: () => {
        if (disposed) return;
        connection.value = "CONNECTED"; error.value = false;
        client!.subscribe("/user/queue/provisioning", message => {
          if (disposed) return;
          try {
            const status = JSON.parse(message.body) as ProvisioningStatus & { error?: string };
            if (status.error) { error.value = true; return; }
            if (status.merchantId !== merchantId || !Number.isSafeInteger(status.version) || status.version < version ||
                !["PROVISIONING", "READY", "FAILED"].includes(status.status)) return;
            version = status.version; receive(status);
          } catch { error.value = true; }
        });
        // Server preserves inbound frame order: subscription is handled before this request.
        send("status");
      },
      onWebSocketClose: () => { if (!disposed && connection.value !== "AUTH_REQUIRED") connection.value = "DISCONNECTED"; },
      onStompError: () => {
        error.value = true;
        connection.value = "AUTH_REQUIRED";
        token.value = null;
        void client?.deactivate();
      },
    });
    client.activate();
  });
  onBeforeUnmount(() => { disposed = true; void client?.deactivate(); });
  return { connection, error, retry: () => { error.value = false; send("retry"); } };
}
