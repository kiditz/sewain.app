import type { Setting, SettingCommand } from "~/types/settings";

export interface RegionalSettings {
  countryId: number | null;
  currency: string;
  currencyPosition: "before" | "after";
  timezone: string;
  locale: "id" | "en";
  dateFormat: string;
  firstDayOfWeek: "monday" | "sunday";
}

const DEFAULT_REGIONAL_SETTINGS: RegionalSettings = {
  countryId: null,
  currency: "IDR",
  currencyPosition: "before",
  timezone: "Asia/Jakarta",
  locale: "id",
  dateFormat: "DD/MM/YYYY",
  firstDayOfWeek: "monday",
};

export const useSettings = () => {
  const api = useApi();

  const scope = `${useRequestURL().host}:${useMerchantStore().current?.slug ?? "guest"}`;
  const regional = useState<RegionalSettings>(`regional-settings:${scope}`, () => ({
    ...DEFAULT_REGIONAL_SETTINGS,
  }));

  const findAll = () => api<Setting[]>("/settings");

  const findByKey = (key: string) => api<Setting>(`/settings/${key}`);

  const create = (command: SettingCommand) =>
    api<Setting>("/settings", {
      method: "POST",
      body: command,
    });

  const update = (id: string, command: SettingCommand) =>
    api<Setting>(`/settings/${id}`, {
      method: "PUT",
      body: command,
    });

  const deleteSetting = (id: string) =>
    api<void>(`/settings/${id}`, {
      method: "DELETE",
    });

  async function loadRegionalSettings() {
    const setting = await findByKey("REGIONAL").catch((error: unknown) => {
      const failure = error as { response?: { status?: number }; statusCode?: number };
      if ((failure.response?.status ?? failure.statusCode) === 404) return null;
      throw error;
    });

    if (setting?.value && typeof setting.value === "object") {
      regional.value = {
        ...DEFAULT_REGIONAL_SETTINGS,
        ...(setting.value as Partial<RegionalSettings>),
      };
    }

    return regional.value;
  }

  function formatDate(
    value: string | Date | number | null | undefined,
    includeTime = false,
  ): string {
    if (value == null || value === "") return "-";

    const dateOnly = typeof value === "string" && /^\d{4}-\d{2}-\d{2}$/.test(value);
    const date = parseApiDate(value);

    if (Number.isNaN(date.getTime())) return "-";

    const { locale, timezone, dateFormat } = regional.value;
    const timeZone = dateOnly ? "UTC" : timezone || "UTC";

    const parts = new Intl.DateTimeFormat(locale, {
      timeZone,
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    }).formatToParts(date);

    const values = Object.fromEntries(
      parts.map(({ type, value }) => [type, value]),
    );

    const day = String(Number(values.day));

    const monthShort = new Intl.DateTimeFormat(locale, {
      timeZone,
      month: "short",
    }).format(date);

    const monthLong = new Intl.DateTimeFormat(locale, {
      timeZone,
      month: "long",
    }).format(date);

    const weekdayShort = new Intl.DateTimeFormat(locale, {
      timeZone,
      weekday: "short",
    }).format(date);

    const weekdayLong = new Intl.DateTimeFormat(locale, {
      timeZone,
      weekday: "long",
    }).format(date);

    const formats: Record<string, string> = {
      "DD/MM/YYYY": `${values.day}/${values.month}/${values.year}`,
      "MM/DD/YYYY": `${values.month}/${values.day}/${values.year}`,
      "YYYY-MM-DD": `${values.year}-${values.month}-${values.day}`,
      "DD MMM YYYY": `${day} ${monthShort} ${values.year}`,
      "DD MMMM YYYY": `${day} ${monthLong} ${values.year}`,
      "ddd, DD MMM YYYY": `${weekdayShort}, ${day} ${monthShort} ${values.year}`,
      "dddd, DD MMMM YYYY": `${weekdayLong}, ${day} ${monthLong} ${values.year}`,
    };

    const formattedDate =
      formats[dateFormat] ??
      new Intl.DateTimeFormat(locale, {
        timeZone,
        dateStyle: "medium",
      }).format(date);

    if (!includeTime) return formattedDate;

    const formattedTime = new Intl.DateTimeFormat(locale, {
      timeZone,
      hour: "2-digit",
      minute: "2-digit",
      hour12: false,
    }).format(date);

    return `${formattedDate}, ${formattedTime}`;
  }
  function parseMoney(value: string | null | undefined): number | null {
    if (value == null || value.trim() === "") return null;

    const parts = new Intl.NumberFormat(regional.value.locale).formatToParts(1234.5);
    const group = parts.find((part) => part.type === "group")?.value;
    const decimal = parts.find((part) => part.type === "decimal")?.value;
    let normalized = value;

    if (group) normalized = normalized.split(group).join("");
    if (decimal) normalized = normalized.split(decimal).join(".");
    normalized = normalized.replace(/[^0-9.\-+]/g, "");

    if (!/^[+-]?(?:\d+(?:\.\d*)?|\.\d+)$/.test(normalized)) return null;

    const amount = Number(normalized);
    return Number.isFinite(amount) ? amount : null;
  }

  function formatMoney(amount: number | string | null | undefined): string {
    if (amount == null) {
      return "";
    }

    const numericAmount = Number(amount);

    if (!Number.isFinite(numericAmount)) return "-";

    const { locale, currency, currencyPosition } = regional.value;
    const currencyCode = currency || "IDR";

    const parts = new Intl.NumberFormat(locale, {
      style: "currency",
      currency: currencyCode,
      currencyDisplay: "symbol",
      minimumFractionDigits: 0,
      maximumFractionDigits: 2,
    }).formatToParts(numericAmount);

    if (currencyPosition === "after") {
      const number = parts
        .filter((part) => !["currency", "literal"].includes(part.type))
        .map((part) => part.value)
        .join("");

      const symbol = parts.find((part) => part.type === "currency")?.value;

      return symbol ? `${number} ${symbol}` : number;
    }

    return parts.map((part) => part.value).join("");
  }

  return {
    findAll,
    findByKey,
    create,
    update,
    deleteSetting,
    regional,
    loadRegionalSettings,
    formatDate,
    formatMoney,
    parseMoney,
  };
};
