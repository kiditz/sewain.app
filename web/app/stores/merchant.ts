import { defineStore } from "pinia";
import type { Merchant } from "~/types/merchant";

export const useMerchantStore = defineStore(
  "merchant",
  () => {
    const current = ref<Merchant | null>(null);
    const merchants = ref<Merchant[]>([]);

    const setMerchants = (value: Merchant[]) => {
      merchants.value = value;
      // Refresh persisted selection from the server; cookies may contain an old merchant or slug.
      current.value = value.find((merchant) => merchant.id === current.value?.id) ?? value[0] ?? null;
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
