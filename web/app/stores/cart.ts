
import { defineStore } from "pinia";
import type { CartItemCommand, CartResponse } from "~/types/cart";

export const useCartStore = defineStore("cart", () => {
  const api = useApi();

  const cart = ref<CartResponse | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);

  const items = computed(() => cart.value?.items ?? []);

  const itemCount = computed(() =>
    items.value.reduce((total, item) => total + item.quantity, 0),
  );

  const rentalSubtotal = computed(() =>
    items.value.reduce(
      (total, item) => total + item.rentalSubtotal,
      0,
    ),
  );

  const depositSubtotal = computed(() =>
    items.value.reduce(
      (total, item) => total + item.depositSubtotal,
      0,
    ),
  );

  const totalAmount = computed(
    () => rentalSubtotal.value + depositSubtotal.value,
  );

  async function execute<T>(request: () => Promise<T>): Promise<T> {
    loading.value = true;
    error.value = null;

    try {
      return await request();
    } catch (cause: unknown) {
      error.value =
        cause instanceof Error ? cause.message : "cart.request.failed";
      throw cause;
    } finally {
      loading.value = false;
    }
  }

  async function getCart(): Promise<void> {
    await execute(async () => {
      const response = await api<CartResponse>("cart", {
        method: "GET",
      });

      cart.value = response;
    });
  }

  async function addItem(command: CartItemCommand): Promise<void> {
    await execute(async () => {
      const response = await api<CartResponse>("cart/items", {
        method: "POST",
        body: command,
      });

      cart.value = response;
    });
  }

  async function updateItem(
    itemId: string,
    command: CartItemCommand,
  ): Promise<void> {
    await execute(async () => {
      const response = await api<CartResponse>(`cart/items/${itemId}`, {
        method: "PUT",
        body: command,
      });

      cart.value = response;
    });
  }

  async function removeItem(itemId: string): Promise<void> {
    await execute(async () => {
      const response = await api<CartResponse>(`cart/items/${itemId}`, {
        method: "DELETE",
      });

      cart.value = response;
    });
  }

  async function clearItems(): Promise<void> {
    await execute(async () => {
      const response = await api<CartResponse>("cart/items", {
        method: "DELETE",
      });

      cart.value = response;
    });
  }

  function reset(): void {
    cart.value = null;
    error.value = null;
  }

  return {
    cart,
    items,
    itemCount,
    loading,
    error,

    rentalSubtotal,
    depositSubtotal,
    totalAmount,

    getCart,
    addItem,
    updateItem,
    removeItem,
    clearItems,
    reset,
  };
});
