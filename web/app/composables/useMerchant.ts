import type { Merchant, MerchantCommand } from "~/types/merchant";

export const useMerchant = () => {
  const config = useRuntimeConfig();
  const token = useCookie<string | null>("access_token");
  const api = useApi();

  const findMerchants = async () => {
    return await api<Merchant[]>("/merchants");
  };

  const addMerchant = async (command: MerchantCommand) => {
    return await api<Merchant>("/merchants", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token.value}`,
      },
      body: command,
    });
  };

  const updateMerchant = async (id: string, command: MerchantCommand) => {
    return await api<Merchant>(`/merchants/${id}`, {
      method: "PUT",
      body: command,
    });
  };

  return {
    findMerchants,
    addMerchant,
    updateMerchant,
  };
};
