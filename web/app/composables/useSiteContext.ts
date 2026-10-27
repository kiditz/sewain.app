export const useSiteContext = () => {
  const url = useRequestURL();

  const host = url.hostname.replace(/\.localhost$/, "");

  const isLanding = host === "sewain.app";

  const isMerchant = host.endsWith(".sewain.app");

  const merchantSlug = isMerchant ? host.replace(".sewain.app", "") : null;

  return {
    host,
    isLanding,
    isMerchant,
    merchantSlug,
  };
};
