import { shouldNoindex } from "#shared/utils/seo-indexing";

export default defineEventHandler((event) => {
  const url = getRequestURL(event);
  if (shouldNoindex(url.pathname, url.searchParams.keys())) {
    // Also applies when route middleware redirects before rendering page metadata.
    setResponseHeader(event, "X-Robots-Tag", "noindex, follow");
  }
});
