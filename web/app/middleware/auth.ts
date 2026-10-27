export default defineNuxtRouteMiddleware(() => {
  const token = useCookie<string | null>("access_token", {
    sameSite: "lax",
  });
  if (!token.value) {
    return navigateTo("/login");
  }
});
