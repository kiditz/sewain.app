import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
} from "~/types/auth";

export const useAuth = () => {
  const config = useRuntimeConfig();
  const apiBaseUrl = import.meta.server
    ? config.apiBaseUrl || config.public.apiBaseUrl
    : config.public.apiBaseUrl;

  const token = useCookie<string | null>("access_token", {
    sameSite: "lax",
  });

  const isAuthenticated = computed(() => !!token.value);

  const clearCookies = () => {
    if (!import.meta.client) return;

    const names = ["access_token", "guest_token", "merchant", "user"];
    const pathSegments = window.location.pathname.split("/").filter(Boolean);
    const paths = new Set(["/"]);
    for (let index = 1; index <= pathSegments.length; index += 1) {
      paths.add(`/${pathSegments.slice(0, index).join("/")}`);
    }
    const hostSegments = window.location.hostname.split(".");
    const domains = [
      undefined,
      ...hostSegments.slice(1).map((_, index) => `.${hostSegments.slice(index + 1).join(".")}`),
    ];
    const secure = window.location.protocol === "https:" ? "; Secure" : "";

    for (const name of names) {
      for (const path of paths) {
        for (const domain of domains) {
          document.cookie = `${name}=; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=${path}${domain ? `; Domain=${domain}` : ""}; SameSite=Lax${secure}`;
        }
      }
    }
  };

  const login = async (request: LoginRequest) => {
    const response = await $fetch<LoginResponse>("/api/auth/login", {
      baseURL: apiBaseUrl,
      method: "POST",
      body: request,
    });

    token.value = response.accessToken;

    return response;
  };

  const register = async (request: RegisterRequest) => {
    return await $fetch("/api/auth/register", {
      baseURL: apiBaseUrl,
      method: "POST",
      body: request,
    });
  };

  const logout = async () => {
    token.value = null;
    useMerchantStore().clear();
    useUserStore().user = null;
    clearNuxtData();
    // Let persisted-state watchers finish writing cleared state before removing the cookies.
    await nextTick();
    clearCookies();
    if (import.meta.client) window.location.replace("/login");
    else return navigateTo("/login", { replace: true });
  };

  return {
    token,
    isAuthenticated,
    login,
    register,
    logout,
  };
};
