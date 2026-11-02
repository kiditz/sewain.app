import { useStorefrontState as useState } from "./useStorefrontState";
import type { StorefrontTheme } from "~/types/storefront";
import { initialTheme } from "../utils/storefront-defaults";
import { useStorefrontResource } from "./useStorefrontResource";

export function useStorefrontTheme() {
  const theme = useState<StorefrontTheme>("storefront-theme", () => structuredClone(initialTheme));
  const initialized = useState("storefront-theme-initialized", () => false);
  const loading = useState("storefront-theme-loading", () => false);
  const resolvedTheme = useState<string | null>("storefront-resolved-theme", () => useRequestURL().searchParams.get("themeId"));
  let targetThemeId = resolvedTheme.value;
  const resource = useStorefrontResource("theme", "theme", () => {
    if (!targetThemeId) throw new Error("Load the storefront before saving theme settings");
    return `/storefront/theme?themeId=${encodeURIComponent(targetThemeId)}`;
  });
  const api = useApi();
  const themeId = useRequestURL().searchParams.get("themeId");
  async function load() {
    if (initialized.value) return;
    loading.value = true;
    try {
      const response = await api<{ configuration: Partial<StorefrontTheme>; themeId: string }>("/storefront/theme", { query: themeId ? { themeId } : undefined });
      targetThemeId = response.themeId;
      resolvedTheme.value = targetThemeId;
      theme.value = response.configuration?.colors ? response.configuration as StorefrontTheme : structuredClone(initialTheme);
      initialized.value = true;
    } finally { loading.value = false; }
  }
  return { theme, initialized, loading, ...resource, load, scheduleSave: () => resource.schedule(theme.value) };
}
