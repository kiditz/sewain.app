export default defineNuxtRouteMiddleware(async () => {
  const token = useCookie<string | null>("access_token", {
    sameSite: "lax",
  });
  if (!token.value) {
    return navigateTo("/login");
  }
  const { findMerchants } = useMerchant();
  const store = useMerchantStore();
  const merchants = await findMerchants();

  if (merchants.length === 0) {
    return navigateTo("/merchant/create");
  }
  store.setMerchants(merchants);
  if (!store.current) {
    store.setCurrent(merchants[0]);
  }
});
