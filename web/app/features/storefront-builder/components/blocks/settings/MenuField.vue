<script setup lang="ts">
import { computed, ref, nextTick } from "vue";
import { DeleteOutlined, EditOutlined, LinkOutlined, MenuOutlined, SwapOutlined } from "@ant-design/icons-vue";
import { useStorefront } from "~/features/storefront-builder/composables/useStorefront";
import EditorFieldRow from "../../EditorFieldRow.vue";

const route = useRoute();
type MenuAction = "edit" | "replace" | "remove";
const props = withDefaults(defineProps<{ modelValue?: string | null }>(), { modelValue: null });
const emit = defineEmits<{
  update: [key: string, value: unknown];
  action: [action: MenuAction, menuId: string | null];
}>();
const { MenuItems, headerMenuId } = useStorefront();
const { t } = useI18n();
const actionMenu = ref(false);
const menu = ref(false);
const confirmDeleteDialog = ref(false);
const trigger = ref<{ $el: HTMLElement }>();
const selectedMenu = computed(() => MenuItems.value.find(item => item.id === (props.modelValue ?? headerMenuId.value)));
const actions = computed(() => [
  { title: t("common.edit"), value: "edit" as const, icon: EditOutlined },
  { title: t("common.replace"), value: "replace" as const, icon: SwapOutlined },
  { title: t("common.remove"), value: "remove" as const, icon: DeleteOutlined },
]);
async function handleAction(value: MenuAction) {
  actionMenu.value = false;
  if (value === "remove") { confirmDeleteDialog.value = true; return; }
  if (value === "edit") {
    emit("action", "edit", selectedMenu.value?.id ?? null);
    await navigateTo({ path: "/merchant/storefront/menus", query: route.query });
    return;
  }
  await nextTick();
  menu.value = true;
}
function handleSelect(id: string) {
  emit("update", "menuId", id);
  emit("action", "replace", id);
  menu.value = false;
}
function handleDelete() {
  const previousId = selectedMenu.value?.id ?? null;
  emit("update", "menuId", "__none__");
  emit("action", "remove", previousId);
  confirmDeleteDialog.value = false;
}
</script>
<template>
  <EditorFieldRow label="Menu">
    <div class="menu-field">
      <v-menu v-model="actionMenu" :close-on-content-click="true" location="bottom start">
        <template #activator="{ props: activator }">
          <v-btn ref="trigger" v-bind="activator" class="menu-field__trigger" variant="outlined" append-icon="mdi-chevron-down" aria-label="Pilih menu navigation">
            <template #prepend><MenuOutlined /></template>
            {{ selectedMenu?.name || t('common.select') }}
          </v-btn>
        </template>
        <v-list density="compact" min-width="240">
          <v-list-item v-for="action in actions" :key="action.value" :title="action.title" @click="handleAction(action.value)">
            <template #prepend><component :is="action.icon" class="mr-3" /></template>
            <template #append><LinkOutlined v-if="action.value === 'edit'" /></template>
          </v-list-item>
        </v-list>
      </v-menu>
      <v-menu v-model="menu" :target="trigger?.$el" :close-on-content-click="true" location="bottom start">
        <v-list density="compact" min-width="240" class="menu-field__options">
          <v-list-item v-for="item in MenuItems" :key="item.id" :title="item.name" :active="selectedMenu?.id === item.id" @click="handleSelect(item.id)">
            <template #prepend><MenuOutlined class="mr-3" /></template>
          </v-list-item>
          <v-list-item v-if="!MenuItems.length" title="Belum ada menu." />
        </v-list>
      </v-menu>
    </div>
    <ConfirmDeleteDialog v-model="confirmDeleteDialog" title="Lepaskan menu?" message="Menu akan dilepas dari blok ini. Menu tetap tersedia di daftar menu toko." @confirmed="handleDelete" />
  </EditorFieldRow>
</template>
<style scoped>
.menu-field { width: 100%; min-width: 0; }
.menu-field__trigger { width: 100%; min-width: 0; height: 40px; justify-content: flex-start; text-transform: none; letter-spacing: normal; border-color: #bdbdbd; border-radius: 12px; }
.menu-field__trigger :deep(.v-btn__content) { display: block; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; text-align: left; }
.menu-field__options { max-height: 320px; overflow-y: auto; }
</style>
