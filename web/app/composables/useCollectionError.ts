/** Collection API errors, including the backend's legacy category codes. */
export const useCollectionError = () => {
  const { apiErrorMessage } = useApiError();
  const snackbar = useSnackbarStore();
  const aliases: Record<string, string> = {
    "category.notfound": "apiErrors.collection_notfound",
    "collection.notfound": "apiErrors.collection_notfound",
    "category.inuse": "apiErrors.collection_inuse",
    "collection.inuse": "apiErrors.collection_inuse",
    "category.reorder.invalid": "apiErrors.collection_reorder_invalid",
    "collection.reorder.invalid": "apiErrors.collection_reorder_invalid",
    "collection.slug.already.exists": "apiErrors.collection_slug_already_exists",
    "dataIntegrityViolation": "apiErrors.collection_conflict",
    "name.notblank": "collections.validation.titleRequired",
    "slug.notblank": "collections.validation.slugRequired",
    "slug.slug": "collections.validation.slugInvalid",
  };
  function collectionErrorMessage(error: unknown, fallback = "collections.errors.saveFailed") {
    const value = error as { data?: { message?: unknown; errors?: Record<string, unknown>; status?: number }; response?: { _data?: { message?: unknown; errors?: Record<string, unknown> }; status?: number }; statusCode?: number; status?: number; name?: string } | null;
    const payload = value?.data ?? value?.response?._data;
    const codes = [payload?.message, ...Object.values(payload?.errors ?? {})];
    for (const code of codes) if (typeof code === "string" && aliases[code]) return aliases[code];
    const resolved = apiErrorMessage(error, fallback);
    if (resolved !== fallback) return resolved;
    const status = value?.statusCode ?? value?.status ?? value?.response?.status ?? value?.data?.status;
    if (status === 401) return "collections.errors.sessionExpired";
    if (status === 403) return "collections.errors.forbidden";
    if (status === 404) return "apiErrors.collection_notfound";
    if (status === 0 || (!payload && !value?.response && value?.name === "FetchError")) return "collections.errors.connectionFailed";
    return fallback;
  }
  function notifyCollectionError(error: unknown, fallback?: string) {
    snackbar.openSnackbar({ type: "error", message: collectionErrorMessage(error, fallback) });
  }
  function watchCollectionErrors(sources: Array<{ error: () => unknown; fallback: string }>) {
    let mounted = false;
    function notifyFirst() {
      const source = sources.find(source => source.error());
      if (source) notifyCollectionError(source.error(), source.fallback);
    }
    onMounted(() => { mounted = true; notifyFirst(); });
    watch(() => sources.map(source => source.error()), () => { if (mounted) notifyFirst(); });
  }
  return { collectionErrorMessage, notifyCollectionError, watchCollectionErrors };
};
