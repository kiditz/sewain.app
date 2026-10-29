import type { InjectionKey } from "vue";
import type { useCheckoutState } from "./useCheckoutState";
export const checkoutContextKey: InjectionKey<ReturnType<typeof useCheckoutState>> = Symbol("checkout-context");
export function useCheckoutContext() {
  const context = inject(checkoutContextKey);
  if (!context) throw new Error("Checkout blocks must be rendered inside CheckoutFlow");
  return context;
}
