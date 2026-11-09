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

  store.setMerchants(merchants);
  if (merchants.length === 0) {
    return navigateTo("/merchant/create");
  }
  if (store.current?.provisioningStatus !== "READY") {
    return navigateTo("/merchant/setup");
  }
});
