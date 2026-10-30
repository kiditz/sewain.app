import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
} from "~/types/auth";

export const useAuth = () => {
  const token = useCookie<string | null>("access_token", {
    sameSite: "lax",
  });

  const isAuthenticated = computed(() => !!token.value);

  const clearCookies = () => {
    if (!import.meta.client) return;

    const names = document.cookie
      .split(";")
      .map((cookie) => cookie.split("=", 1)[0]?.trim())
      .filter((name): name is string => Boolean(name));
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
    const config = useRuntimeConfig();

    const response = await $fetch<LoginResponse>("/api/auth/login", {
      baseURL: config.public.apiBaseUrl,
      method: "POST",
      body: request,
    });

    token.value = response.accessToken;

    return response;
  };

  const register = async (request: RegisterRequest) => {
    const config = useRuntimeConfig();

    return await $fetch("/api/auth/register", {
      // baseURL: config.public.apiBaseUrl,
      method: "POST",
      body: request,
    });
  };

  const logout = () => {
    token.value = null;
    useMerchantStore().clear();
    useUserStore().user = null;
    clearCookies();
  };

  return {
    token,
    isAuthenticated,
    login,
    register,
    logout,
  };
};
