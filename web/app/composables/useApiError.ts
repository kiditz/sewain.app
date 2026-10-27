/** Resolve backend ErrorCode values to localized API error keys. */
export const useApiError = () => {
  const { te } = useI18n();

  const apiErrorMessage = (
    error: unknown,
    fallback = "errors.general",
  ): string => {
    const value = error as {
      data?: { message?: unknown; errors?: Record<string, unknown> };
      response?: { _data?: { message?: unknown; errors?: Record<string, unknown> } };
    } | null;
    const payload = value?.data ?? value?.response?._data;
    const firstFieldError = payload?.errors
      ? Object.values(payload.errors).find((item): item is string => typeof item === "string")
      : undefined;
    const code = typeof payload?.message === "string" && payload.message !== "Validation failed"
      ? payload.message
      : firstFieldError;

    if (!code || code.includes(" ")) return fallback;

    const apiKey = `apiErrors.${code.replace(/\./g, "_")}`;
    if (te(apiKey)) return apiKey;

    const validationKey = `errors.${code}`;
    return te(validationKey) ? validationKey : fallback;
  };

  return { apiErrorMessage };
};
