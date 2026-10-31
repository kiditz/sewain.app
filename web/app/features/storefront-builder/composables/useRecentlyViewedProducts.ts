import { useStorefrontState } from "./useStorefrontState";

const historyLimit = 12;
const productId = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export function useRecentlyViewedProducts() {
  const scope = `${useRequestURL().host}:${useMerchantStore().current?.slug ?? "guest"}`;
  const storageKey = `storefront:recently-viewed:${scope}`;
  const ids = useStorefrontState<string[]>("recently-viewed-products", () => []);

  function load() {
    if (!import.meta.client) return;
    try {
      const value: unknown = JSON.parse(localStorage.getItem(storageKey) || "[]");
      ids.value = Array.isArray(value)
        ? [...new Set(value.filter((id): id is string => typeof id === "string" && productId.test(id)))].slice(0, historyLimit)
        : [];
    } catch { /* Keep the session history when browser storage is unavailable. */ }
  }

  function remember(id: string) {
    if (!import.meta.client || !productId.test(id)) return;
    load();
    ids.value = [id, ...ids.value.filter(existing => existing !== id)].slice(0, historyLimit);
    try { localStorage.setItem(storageKey, JSON.stringify(ids.value)); }
    catch { /* Browsing and session history still work without persistent storage. */ }
  }

  return { ids, load, remember };
}
