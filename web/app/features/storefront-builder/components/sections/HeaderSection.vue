<script setup lang="ts">
import { NuxtLink } from "#components";
import type { SectionLayout, StorefrontBlock, StorefrontPadding } from "~/types/storefront";
import { useStorefront } from "../../composables/useStorefront";
import { getLayoutStyle, getPaddingStyle } from "../../utils/block-settings";
import { headerDefaults, resolveHeaderMenu } from "../../utils/header-section";
import { resolveMenuItems } from "../../utils/storefront-menu";
import { storefrontBrandingKey, storefrontDrawerTargetKey } from "../../utils/storefront-branding";
import StorefrontMenuLinks from "~/components/storefront/StorefrontMenuLinks.vue";

const props = defineProps<{
  settings: Record<string, unknown>;
  blocks: StorefrontBlock[];
  preview?: boolean;
  selectedBlockId?: string | null;
  product?: unknown;
}>();
const storefront = useStorefront();
const branding = inject(storefrontBrandingKey, null);
const drawerTarget = inject(storefrontDrawerTargetKey, "body");
const { current: merchant } = storeToRefs(useMerchantStore());
const merchantName = computed(() => branding?.merchantName.value || merchant.value?.name || "Your Store");
const logoUrl = computed(() => branding?.logoUrl.value ?? merchant.value?.storeInformation?.logoUrl);
const menuId = computed(() => props.settings.menuId as string | null | undefined);
const menu = computed(() => resolveHeaderMenu(storefront.MenuItems.value, menuId.value, storefront.headerMenuId.value));
const menuItems = computed(() => resolveMenuItems(menu.value?.items ?? [], storefront.pages.value));
const defaults = headerDefaults();
const layoutStyle = computed(() => ({
  ...getLayoutStyle({ ...defaults.layout, ...(props.settings.layout as Partial<SectionLayout>) }, storefront.theme.value),
  ...getPaddingStyle({ ...defaults.padding, ...(props.settings.padding as StorefrontPadding) }, storefront.theme.value),
}));
const drawer = ref(false);
const mobileToggle = ref<{ $el: HTMLButtonElement }>();
const drawerCloseButton = ref<{ $el: HTMLButtonElement }>();
watch(drawer, async (open, previouslyOpen) => {
  await nextTick();
  if (open) drawerCloseButton.value?.$el.focus();
  else if (previouslyOpen) mobileToggle.value?.$el.focus();
});
const drawerId = `storefront-menu-${useId()}`;
const header = ref<HTMLElement>();
const route = useRoute();
watch(() => route.fullPath, () => { drawer.value = false; });
watch(menuItems, items => { if (!items.length) drawer.value = false; });

let observer: ResizeObserver | undefined;
onMounted(() => {
  if (!header.value) return;
  observer = new ResizeObserver(entries => {
    if ((entries[0]?.contentRect.width ?? 0) >= 600) drawer.value = false;
  });
  observer.observe(header.value);
});
const { itemCount } = storeToRefs(useCartStore());
const cartBump = ref(false);
let bumpTimer: ReturnType<typeof setTimeout> | undefined;
watch(itemCount, (next, previous) => {
  if (next <= previous) return;
  cartBump.value = true;
  if (bumpTimer) clearTimeout(bumpTimer);
  bumpTimer = setTimeout(() => { cartBump.value = false; }, 500);
});
onBeforeUnmount(() => {
  observer?.disconnect();
  if (bumpTimer) clearTimeout(bumpTimer);
});
</script>

<template>
  <header ref="header" class="store-header">
    <div class="store-header__inner" :style="layoutStyle">
      <v-btn
        v-if="menuItems.length"
        ref="mobileToggle"
        class="store-header__mobile-toggle"
        icon="mdi-menu"
        variant="text"
        aria-label="Buka menu"
        :aria-expanded="drawer"
        :aria-controls="drawerId"
        @click="drawer = !drawer"
      />
      <component :is="preview ? 'a' : NuxtLink" :to="preview ? undefined : '/'" :href="preview ? '/' : undefined" class="store-header__brand" @click="preview && $event.preventDefault()">
        <img v-if="logoUrl" :src="logoUrl" alt="" class="store-header__logo" />
        <span>{{ merchantName }}</span>
      </component>
      <StorefrontMenuLinks v-if="menuItems.length" class="store-header__desktop-menu" :menu-id="menuId" :preview="preview" />
      <component :is="preview ? 'a' : NuxtLink" :to="preview ? undefined : '/cart'" :href="preview ? '/cart' : undefined" class="store-header__cart" :aria-label="`${$t('ui.cart')} (${itemCount})`" :class="{ 'cart-bump': cartBump }" @click="preview && $event.preventDefault()">
        <v-icon icon="mdi-cart-outline" aria-hidden="true" />
        <span class="store-header__cart-label">{{ $t("ui.cart") }}</span>
        <span>({{ itemCount }})</span>
      </component>
    </div>
  </header>
  <ClientOnly>
  <Teleport :to="drawerTarget" :disabled="!preview" defer>
  <v-navigation-drawer
    v-if="menuItems.length"
    :id="drawerId"
    v-model="drawer"
    temporary
    retain-focus
    capture-focus
    mobile
    touchless
    disable-resize-watcher
    :absolute="preview"
    :width="280"
    location="start"
    class="store-header-drawer"
    role="dialog"
    :aria-modal="drawer || undefined"
    :aria-label="menu?.name || 'Menu'"
    @keydown.esc="drawer = false"
  >
    <div class="store-header-drawer__heading">
      <strong>{{ merchantName }}</strong>
      <v-btn ref="drawerCloseButton" icon="mdi-close" variant="text" aria-label="Tutup menu" @click="drawer = false" />
    </div>
    <StorefrontMenuLinks :menu-id="menuId" vertical :preview="preview" @navigate="drawer = false" />
  </v-navigation-drawer>
  </Teleport>
  </ClientOnly>
</template>

<style scoped>
.store-header { container: storefront-header / inline-size; background: var(--store-surface); color: var(--store-text); border-bottom: 1px solid #00000012; }
.store-header__inner { display: flex; min-width: 0; }
.store-header__brand { display: inline-flex; align-items: center; min-width: 0; gap: 12px; color: var(--store-primary); font-family: var(--store-heading-font, Georgia, serif); font-size: 24px; font-weight: 700; text-decoration: none; }
.store-header__brand span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.store-header__logo { width: auto; max-width: 140px; height: 48px; object-fit: contain; }
.store-header__desktop-menu { min-width: 0; }
.store-header__mobile-toggle { display: none; flex-shrink: 0; }
.store-header__cart { display: inline-flex; align-items: center; gap: 6px; flex-shrink: 0; padding: 8px; color: var(--store-text); text-decoration: none; border-radius: var(--store-radius); }
.store-header__cart:hover, .store-header__cart:focus-visible { text-decoration: underline; }
.store-header-drawer { background: var(--store-surface); color: var(--store-text); }
.store-header-drawer__heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 12px; }
.store-header-drawer :deep(.store-menu) { padding: 12px; }
.cart-bump { animation: cart-pop 500ms ease; }
@keyframes cart-pop { 50% { transform: scale(1.08); } }
@media (prefers-reduced-motion: reduce) { .cart-bump { animation: none; } }
@container storefront-header (max-width: 599px) {
  .store-header__inner { flex-direction: row !important; align-items: center !important; justify-content: space-between !important; gap: 8px !important; height: auto !important; }
  .store-header__mobile-toggle { display: inline-flex; }
  .store-header__desktop-menu, .store-header__cart-label { display: none; }
  .store-header__brand { font-size: 18px; gap: 8px; flex: 1 1 auto; }
  .store-header__logo { max-width: 80px; height: 36px; }
}
</style>
