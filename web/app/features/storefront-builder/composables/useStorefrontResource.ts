interface SaveState { saving: boolean; dirty: boolean; error: boolean }
interface Writer { timer?: ReturnType<typeof setTimeout>; queue: Promise<unknown>; revision: number; flush?: () => Promise<unknown> }
const writers = new WeakMap<object, Map<string, Writer>>();

/** Each resource has its own queue; queues belong to the Nuxt app, never another SSR request. */
export function useStorefrontResourceFactory(domain: string) {
  const app = useNuxtApp();
  const api = useApi();
  const themeId = useRequestURL().searchParams.get("themeId");
  const themeScope = ["pages", "theme"].includes(domain) ? themeId ?? "active" : "store";
  const scope = `${useRequestURL().host}:${useMerchantStore().current?.slug ?? "guest"}:${themeScope}`;
  // Capture Nuxt state synchronously during setup. On SSR, runWithContext()
  // returns a Promise even when its callback returns a ref synchronously.
  const statuses = useState<Record<string, SaveState>>(`storefront-save:${scope}:${domain}`, () => ({}));
  return (id: string, endpoint: string | (() => string)) => {
    function requestEndpoint() {
      const path = typeof endpoint === "function" ? endpoint() : endpoint;
      return themeId && !path.includes("?") && /^\/storefront\/(pages|theme)(\/|$)/.test(path) ? `${path}?themeId=${encodeURIComponent(themeId)}` : path;
    }
    const status = computed(() => {
      statuses.value ??= {};
      return statuses.value[id] ??= { saving: false, dirty: false, error: false };
    });
    function writer() {
      let entries = writers.get(app);
      if (!entries) { entries = new Map(); writers.set(app, entries); }
      const key = `${scope}:${domain}:${id}:${requestEndpoint()}`;
      let entry = entries.get(key);
      if (!entry) { entry = { queue: Promise.resolve(), revision: 0 }; entries.set(key, entry); }
      return entry;
    }
    function enqueue(payload: unknown, method: "PUT" | "DELETE") {
      const entry = writer();
      if (entry.timer) clearTimeout(entry.timer);
      entry.flush = undefined;
      const revision = ++entry.revision;
      const url = requestEndpoint();
      status.value.dirty = true;
      const task = entry.queue.catch(() => undefined).then(async () => {
        status.value.saving = true;
        try {
          await api(url, { method, ...(method === "PUT" ? { body: { configuration: payload } } : {}) });
          status.value.error = false;
          if (entry.revision === revision) status.value.dirty = false;
        } catch (error) {
          status.value.error = true;
          throw error;
        } finally { status.value.saving = false; }
      });
      entry.queue = task;
      // Keep autosave failures represented by status, without unhandled rejections.
      void task.catch(() => undefined);
      return task;
    }
    function schedule(payload: unknown) {
      if (!import.meta.client) return;
      const entry = writer();
      if (entry.timer) clearTimeout(entry.timer);
      entry.revision++;
      status.value.dirty = true;
      const snapshot = JSON.parse(JSON.stringify(payload));
      entry.flush = () => enqueue(snapshot, "PUT");
      entry.timer = setTimeout(() => { void entry.flush?.().catch(() => undefined); }, 600);
    }
    return {
      status,
      saving: computed(() => status.value.saving),
      saveError: computed(() => status.value.error),
      hasUnsavedChanges: computed(() => status.value.dirty),
      schedule,
      save: (payload: unknown) => enqueue(JSON.parse(JSON.stringify(payload)), "PUT"),
      remove: () => enqueue(undefined, "DELETE"),
    };
  };
}

export function useStorefrontResource(domain: string, id: string, endpoint: string | (() => string)) {
  return useStorefrontResourceFactory(domain)(id, endpoint);
}

/** Finish this store's pending autosaves before creating a published snapshot. */
export function useStorefrontWriteFlush() {
  const app = useNuxtApp();
  const scope = `${useRequestURL().host}:${useMerchantStore().current?.slug ?? "guest"}:`;
  return async () => {
    const entries = [...(writers.get(app)?.entries() ?? [])].filter(([key]) => key.startsWith(scope));
    await Promise.all(entries.map(([, entry]) => entry.flush ? entry.flush() : entry.queue));
  };
}
