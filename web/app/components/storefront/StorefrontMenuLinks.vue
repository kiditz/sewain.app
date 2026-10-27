<script setup lang="ts">
import { useStorefront } from "~/features/storefront-builder/composables/useStorefront";
import { resolveHeaderMenu } from "~/features/storefront-builder/utils/header-section";
import { resolveMenuItems } from "~/features/storefront-builder/utils/storefront-menu";
const props = defineProps<{ menuId?: string | null; vertical?: boolean; preview?: boolean }>();
const emit = defineEmits<{ navigate: [] }>();
const { MenuItems, headerMenuId, pages } = useStorefront();
const headerMenu = computed(() => resolveHeaderMenu(MenuItems.value, props.menuId, headerMenuId.value));
function navigate(event: MouseEvent) {
  if (props.preview) event.preventDefault();
  emit("navigate");
}
const items = computed(() => resolveMenuItems(headerMenu.value?.items ?? [], pages.value));
</script>

<template>
  <nav :aria-label="headerMenu?.name || 'Main menu'" class="store-menu" :class="{ 'store-menu--vertical': vertical }">
    <template v-for="item in items" :key="item.id">
      <a v-if="preview" :href="item.url" class="store-menu__link" @click.prevent="navigate">{{ item.label }}</a>
      <NuxtLink v-else :to="item.url" class="store-menu__link" @click="navigate">{{ item.label }}</NuxtLink>
    </template>
  </nav>
</template>

<style scoped>
.store-menu { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.store-menu--vertical { flex-direction: column; align-items: stretch; }
.store-menu__link { color: var(--store-text); padding: 8px 12px; text-decoration: none; border-radius: var(--store-radius); }
.store-menu__link:hover, .store-menu__link:focus-visible { text-decoration: underline; }
</style>
