<script setup lang="ts">
import type { Attribute } from "~/types/attribute";
import type { ProductAttributeCommand } from "~/types/product";
const props = defineProps<{
  modelValue: ProductAttributeCommand[];
}>();

const emit = defineEmits<{
  "update:modelValue": [value: ProductAttributeCommand[]];
}>();
const { findAttributes } = useAttribute();
const attributes = ref<Attribute[]>([]);
const selectedValues = computed<Record<string, string[]>>(() => {
  const result: Record<string, string[]> = {};
  for (const attribute of attributes.value) {
    result[attribute.id] = attribute.values
      .filter((value) =>
        props.modelValue.some((item) => item.attributeValueId === value.id),
      )
      .map((value) => value.id);
  }
  return result;
});
const updateAttribute = (attribute: Attribute, valueIds: string[]) => {
  const selected = new Set(
    props.modelValue.map((item) => item.attributeValueId),
  );
  for (const value of attribute.values) {
    if (valueIds.includes(value.id)) {
      selected.add(value.id);
    } else {
      selected.delete(value.id);
    }
  }
  emit(
    "update:modelValue",
    [...selected].map((attributeValueId) => ({ attributeValueId })),
  );
};
const loadAttributes = async () => {
  attributes.value = await findAttributes();
};
await loadAttributes();
</script>

<template>
  <div>
    <div v-for="attribute in attributes" :key="attribute.id" class="mb-6">
      <div class="text-title-medium font-weight-medium mb-2">
        {{ attribute.name }}
      </div>

      <v-chip-group
        :model-value="selectedValues[attribute.id]"
        multiple
        @update:model-value="updateAttribute(attribute, $event)"
      >
        <v-chip
          v-for="value in attribute.values"
          :key="value.id"
          :value="value.id"
          filter
          variant="outlined"
        >
          {{ value.value }}
        </v-chip>
      </v-chip-group>
    </div>

    <div v-if="!attributes.length" class="text-medium-emphasis">
      {{ $t("ui.no_attributes_available") }}
    </div>
  </div>
</template>
