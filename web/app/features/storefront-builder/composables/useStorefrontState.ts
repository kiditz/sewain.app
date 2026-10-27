/** Keep editor data and initialization flags separate when switching merchants. */
export function useStorefrontState<T>(key: string, initialize: () => T) {
  const scope = `${useRequestURL().host}:${useMerchantStore().current?.slug ?? "guest"}`;
  return useState<T>(`storefront:${scope}:${key}`, initialize);
}
