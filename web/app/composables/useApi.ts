export const useApi = () => {
  const config = useRuntimeConfig();
  const token = useCookie<string | null>("access_token");
  const merchantStore = useMerchantStore();
  const url = useRequestURL();
  const guestToken = useCookie<string | null>("guest_token");

  const hostname = url.hostname.toLowerCase();

  const tenant = hostname.endsWith(".sewain.app.localhost")
    ? hostname.slice(0, -".sewain.app.localhost".length).split(".")[0]
    : hostname.endsWith(".sewain.app")
      ? hostname.slice(0, -".sewain.app".length).split(".")[0]
      : null;

  const apiBaseUrl = import.meta.server
    ? config.apiBaseUrl || config.public.apiBaseUrl
    : config.public.apiBaseUrl;

  return $fetch.create({
    baseURL: `${apiBaseUrl}/api/`,

    onRequest({ options }) {
      const headers = new Headers(options.headers);
      if (!guestToken.value) {
        guestToken.value = crypto.randomUUID();
      }

      if (token.value) {
        headers.set("Authorization", `Bearer ${token.value}`);
      }
      if (guestToken.value) {
        headers.set("X-Guest-Token", guestToken.value);
      }

      if (tenant && tenant !== "www") {
        headers.set("X-Tenant", tenant);
      } else if (merchantStore.current?.slug) {
        headers.set("X-Tenant", merchantStore.current.slug);
      }
      

      options.headers = headers;
    },

    onResponseError({ response }) {
      if (response.status === 401) {
        token.value = null;
      }
    },
  });
};
