export const socialPlatforms = [
  { title: "Instagram", value: "instagram" },
  { title: "Facebook", value: "facebook" },
  { title: "X / Twitter", value: "twitter" },
  { title: "YouTube", value: "youtube" },
  { title: "LinkedIn", value: "linkedin" },
  { title: "WhatsApp", value: "whatsapp" },
  { title: "Telegram", value: "telegram" },
  { title: "GitHub", value: "github" },
  { title: "Website", value: "website" },
];
export function socialLinkHref(value: unknown): string {
  if (typeof value !== "string" || !value.trim()) return "";
  try {
    const url = new URL(value.trim());
    return ["https:", "http:"].includes(url.protocol) ? url.href : "";
  } catch { return ""; }
}
