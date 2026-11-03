<script setup lang="ts">
import { computed } from "vue";
import {
  AppstoreOutlined,
  CreditCardOutlined,
  FileTextOutlined,
  HomeOutlined,
  PhoneOutlined,
  ShoppingCartOutlined,
  ShoppingOutlined,
} from "@ant-design/icons-vue";
import type { Component } from "vue";
import { useStorefront } from "../composables/useStorefront";

const props = defineProps<{
  modelValue: string | null;
  label?: string;
  kinds?: string[];
}>();

const { pages } = useStorefront();
const emit = defineEmits<{
  select: [page: (typeof pages.value)[number]];
}>();

const availablePages = computed(() => pages.value.filter(page => !props.kinds || props.kinds.includes(page.kind)));
const selectedPage = computed(() =>
  pages.value.find((page) => page.id === props.modelValue),
);

const icons: Record<string, Component> = {
  AppstoreOutlined,
  CreditCardOutlined,
  FileTextOutlined,
  HomeOutlined,
  PhoneOutlined,
  ShoppingCartOutlined,
  ShoppingOutlined,
};

function iconFor(name?: string): Component {
  return icons[name ?? ""] ?? FileTextOutlined;
}
</script>

<template>
  <v-menu>
    <template #activator="{ props: menuProps }">
      <v-btn v-bind="menuProps" variant="outlined">
        <component :is="iconFor(selectedPage?.icon)" class="page-menu__icon" />
        {{ selectedPage?.title ?? label }}
        <v-icon end icon="mdi-chevron-down" />
      </v-btn>
    </template>

    <v-list min-width="240" density="compact">
      <v-list-item
        v-for="page in availablePages"
        :key="page.id"
        :active="page.id === modelValue"
        :title="page.title"
        @click="emit('select', page)"
      >
        <template #prepend>
          <component class="mr-3" :is="iconFor(page.icon)" />
        </template>
      </v-list-item>
    </v-list>
  </v-menu>
</template>

<style scoped>
.page-menu__icon {
  margin-inline-end: 8px;
}
</style>
