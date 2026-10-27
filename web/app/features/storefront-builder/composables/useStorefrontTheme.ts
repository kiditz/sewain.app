import { useStorefrontState as useState } from "./useStorefrontState";
import type { StorefrontTheme } from "~/types/storefront";
import { initialTheme } from "../utils/storefront-defaults";
import { useStorefrontResource } from "./useStorefrontResource";

export function useStorefrontTheme() {
  const theme = useState<StorefrontTheme>("storefront-theme", () => structuredClone(initialTheme));
  const initialized = useState("storefront-theme-initialized", () => false);
  const loading = useState("storefront-theme-loading", () => false);
  const resource = useStorefrontResource("theme", "theme", "/storefront/theme");
  const api = useApi();
  async function load() {
    if (initialized.value) return;
    loading.value = true;
    try {
      const response = await api<{ configuration: Partial<StorefrontTheme> }>("/storefront/theme");
      if (response.configuration?.colors) theme.value = response.configuration as StorefrontTheme;
      initialized.value = true;
    } finally { loading.value = false; }
  }
  return { theme, initialized, loading, ...resource, load, scheduleSave: () => resource.schedule(theme.value) };
}
