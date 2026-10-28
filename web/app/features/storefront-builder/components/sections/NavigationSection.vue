<script setup lang="ts">
import { UserOutlined, ShoppingCartOutlined } from "@ant-design/icons-vue";
import BlockRenderer from "~/components/storefront/BlockRenderer.vue";
import type {
  SectionLayout,
  StorefrontBlock,
  StorefrontPadding,
} from "~/types/storefront";
import { useStorefront } from "../../composables/useStorefront";
import { getLayoutStyle, getPaddingStyle } from "../../utils/block-settings";
import {
  navigationDefaults,
  resolveNavigationMenu,
} from "../../utils/navigation-section";
import { resolveMenuItems } from "../../utils/storefront-menu";
import {
  storefrontBrandingKey,
  storefrontDrawerTargetKey,
} from "../../utils/storefront-branding";
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
const merchantName = computed(
  () => branding?.merchantName.value || merchant.value?.name || "Your Store",
);
const menuBlocks = computed(() =>
  props.blocks.filter((block) => block.enabled && block.type === "MENU"),
);
const menus = computed(() =>
  menuBlocks.value
    .map((block) => ({
      block,
      menu: resolveNavigationMenu(
        storefront.MenuItems.value,
        block.settings.menuId,
        storefront.headerMenuId.value,
      ),
    }))
    .filter(
      ({ menu }) =>
        resolveMenuItems(menu?.items ?? [], storefront.pages.value).length,
    ),
);
const hasMenu = computed(() => menus.value.length > 0);
function escapedText(text: string) {
  return text.replace(
    /[&<>"']/g,
    (char) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        char
      ]!,
  );
}
const navigationBlocks = computed(() =>
  props.blocks.map((block) => {
    if (block.type === "HEADER")
      return {
        ...block,
        settings: {
          ...block.settings,
          text: block.settings.text || escapedText(merchantName.value),
        },
      };
    return block;
  }),
);
const defaults = navigationDefaults();
const navigationLayout = computed(() => ({
  ...defaults.layout,
  ...(props.settings.layout as Partial<SectionLayout>),
}));
const layoutStyle = computed(() => ({
  ...getLayoutStyle(navigationLayout.value, storefront.theme.value),
  ...getPaddingStyle(
    { ...defaults.padding, ...(props.settings.padding as StorefrontPadding) },
    storefront.theme.value,
  ),
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
watch(
  () => route.fullPath,
  () => {
    drawer.value = false;
  },
);
watch(hasMenu, (visible) => {
  if (!visible) drawer.value = false;
});

let observer: ResizeObserver | undefined;
onMounted(() => {
  if (!header.value) return;
  observer = new ResizeObserver((entries) => {
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
  bumpTimer = setTimeout(() => {
    cartBump.value = false;
  }, 500);
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
        v-if="hasMenu"
        ref="mobileToggle"
        class="store-header__mobile-toggle"
        icon="mdi-menu"
        variant="text"
        aria-label="Buka menu"
        :aria-expanded="drawer"
        :aria-controls="drawerId"
        @click="drawer = !drawer"
      />
      <BlockRenderer
        class="store-header__blocks"
        :blocks="navigationBlocks"
        :layout="navigationLayout"
        :preview="preview"
        :selected-block-id="selectedBlockId"
      />
      <div class="store-header__actions">
        <v-btn
          icon
          variant="text"
          disabled
          aria-label="Login dengan email (segera hadir)"
          class="store-header__account"
        >
          <UserOutlined aria-hidden="true" />
        </v-btn>
        <v-btn
          :to="preview ? undefined : '/cart'"
          :href="preview ? '/cart' : undefined"
          icon
          variant="text"
          class="store-header__cart"
          :aria-label="`${$t('ui.cart')} (${itemCount})`"
          :class="{ 'cart-bump': cartBump }"
          @click="preview && $event.preventDefault()"
        >
          <v-badge
            :model-value="itemCount > 0"
            :content="itemCount"
            :max="99"
            color="primary"
            floating
          >
            <ShoppingCartOutlined aria-hidden="true" />
          </v-badge>
        </v-btn>
      </div>
    </div>
  </header>
  <ClientOnly>
    <Teleport :to="drawerTarget" :disabled="!preview" defer>
      <v-navigation-drawer
        v-if="hasMenu"
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
        :aria-label="menus[0]?.menu?.name || 'Menu'"
        @keydown.esc="drawer = false"
      >
        <div class="store-header-drawer__heading">
          <strong>{{ menus[0]?.menu?.name || "Menu" }}</strong>
          <v-btn
            ref="drawerCloseButton"
            icon="mdi-close"
            variant="text"
            aria-label="Tutup menu"
            @click="drawer = false"
          />
        </div>
        <StorefrontMenuLinks
          v-for="entry in menus"
          :key="entry.block.id"
          :menu-id="entry.block.settings.menuId as string | null | undefined"
          vertical
          :preview="preview"
          @navigate="drawer = false"
        />
      </v-navigation-drawer>
    </Teleport>
  </ClientOnly>
</template>

<style scoped>
.store-header {
  container: storefront-header / inline-size;
  background: var(--store-surface);
  color: var(--store-text);
  border-bottom: 1px solid #00000012;
}
.store-header__inner {
  display: flex;
  min-width: 0;
}
.store-header__blocks {
  flex: 1;
  min-width: 0;
}
.store-header__blocks :deep([data-block-type="LOGO"]) {
  min-width: 0;
  flex: 0 1 auto;
}
.store-header__blocks :deep(.block-heading) {
  font-size: 24px;
  font-weight: 700;
}
.store-header__blocks :deep([data-block-type="HEADING"]) {
  min-width: 0;
}
.store-header__blocks :deep([data-block-type="MENU"]) {
  min-width: 0;
}
.store-header__mobile-toggle {
  display: none;
  flex-shrink: 0;
}
.store-header__actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  color: var(--store-text);
}
.store-header__actions :deep(.anticon) {
  font-size: 22px;
}
.store-header__cart {
  color: inherit;
}
.store-header-drawer {
  background: var(--store-surface);
  color: var(--store-text);
}
.store-header-drawer__heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
}
.store-header-drawer :deep(.store-menu) {
  padding: 12px;
}
.cart-bump {
  animation: cart-pop 500ms ease;
}
@keyframes cart-pop {
  50% {
    transform: scale(1.08);
  }
}
@media (prefers-reduced-motion: reduce) {
  .cart-bump {
    animation: none;
  }
}
@container storefront-header (max-width: 599px) {
  .store-header__inner {
    flex-direction: row !important;
    align-items: center !important;
    justify-content: space-between !important;
    gap: 8px !important;
    height: auto !important;
  }
  .store-header__mobile-toggle {
    display: inline-flex;
  }
  .store-header__blocks {
    flex: 1 1 auto;
    flex-direction: row !important;
    align-items: center !important;
    gap: 8px !important;
  }
  .store-header__blocks :deep([data-block-type="MENU"]) {
    display: none;
  }
  .store-header__blocks :deep([data-block-type="LOGO"]) {
    max-width: 80px;
    --navigation-logo-max-width: 80px;
    --navigation-logo-max-height: 36px;
  }
  .store-header__blocks :deep(.block-heading) {
    font-size: 18px;
  }
}
</style>
