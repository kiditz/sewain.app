const formatNumber = (value: number | null) => {
  if (value === null) {
    return "";
  }

  return new Intl.NumberFormat("id-ID").format(value);
};

const parseNumber = (value: string) => {
  const parsed = Number(value.replace(/\./g, ""));
  return Number.isNaN(parsed) ? null : parsed;
};

export const money = (amount: number) =>
  new Intl.NumberFormat("id-ID", {
    style: "currency",
    currency: "IDR",
    maximumFractionDigits: 0,
  }).format(amount);
