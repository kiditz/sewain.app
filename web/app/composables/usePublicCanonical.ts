/** Canonical URLs stay on the current request origin and exclude tracking/filter parameters. */
export function usePublicCanonical(enabled: () => boolean = () => true) {
  const route = useRoute();
  const origin = useRequestURL().origin;

  useHead(() => {
    if (!enabled()) return { link: [] };
    const url = new URL(origin);
    url.pathname = route.path;
    // Keep explicit collection pagination; page 1 uses the unparameterized URL.
    if (route.path === "/collections" || route.path.startsWith("/collections/")) {
      const page = route.query.page;
      if (typeof page === "string" && /^[1-9]\d*$/.test(page) && Number.isSafeInteger(Number(page)) && Number(page) > 1) {
        url.searchParams.set("page", String(Number(page)));
      }
    }
    return { link: [{ key: "canonical", rel: "canonical", href: url.href }] };
  });
}
