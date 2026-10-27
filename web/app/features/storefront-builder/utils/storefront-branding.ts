import type { ComputedRef, InjectionKey } from "vue";

export const storefrontDrawerTargetKey: InjectionKey<string> = Symbol("storefront-drawer-target");

export const storefrontBrandingKey: InjectionKey<{
  merchantName: ComputedRef<string>;
  logoUrl: ComputedRef<string | null | undefined>;
}> = Symbol("storefront-branding");
