import { defineStore } from "pinia";
import type { Merchant } from "~/types/merchant";

export const useMerchantStore = defineStore(
  "merchant",
  () => {
    const current = ref<Merchant | null>(null);
    const merchants = ref<Merchant[]>([]);

    const setMerchants = (value: Merchant[]) => {
      merchants.value = value;
    };

    const setCurrent = (merchant: Merchant | undefined) => {
      if (merchant) {
        current.value = merchant;
      }
    };

    const clear = () => {
      merchants.value = [];
      current.value = null;
    };

    return {
      current,
      merchants,
      setMerchants,
      setCurrent,
      clear,
    };
  },
  {
    persist: {
      storage: piniaPluginPersistedstate.cookies(),
      pick: ['current']
    },
  },
);
