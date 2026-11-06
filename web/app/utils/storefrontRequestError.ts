/** Preserve genuine missing/access errors without exposing upstream internals or calling outages 404s. */
export function storefrontRequestError(error: unknown, notFoundMessage: string) {
  const failure = error as { statusCode?: number; status?: number; response?: { status?: number } } | null;
  const status = failure?.response?.status ?? failure?.statusCode ?? failure?.status;
  if (status === 404) return createError({ statusCode: 404, statusMessage: notFoundMessage });
  if (status === 401 || status === 403) return createError({ statusCode: status, statusMessage: "Store access denied" });
  return createError({ statusCode: 503, statusMessage: "Store temporarily unavailable" });
}
