import type { MerchantReportResponse } from "~/types/merchant-report";

export const useMerchantReport = () => {
  const api = useApi();
  return {
    getReport: (days: number) =>
      api<MerchantReportResponse>("/merchant/reports", { query: { days } }),
  };
};
