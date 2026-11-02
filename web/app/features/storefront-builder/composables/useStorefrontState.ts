/** Keep editor data and initialization flags separate when switching merchants. */
export function useStorefrontState<T>(key: string, initialize: () => T) {
  const scope = `${useRequestURL().host}:${useMerchantStore().current?.slug ?? "guest"}`;
  const themeId = useRequestURL().searchParams.get("themeId") ?? "active";
  const themeScoped = /^(storefront-pages(?:-|$)|storefront-theme(?:-|$)|storefront-resolved-theme$)/.test(key);
  return useState<T>(`storefront:${scope}:${themeScoped ? `${themeId}:` : ""}${key}`, initialize);
}
