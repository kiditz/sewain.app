<script setup lang="ts">
import { storeToRefs } from "pinia";

const store = useStorefrontProductStore();

const { availableAttributes, selectedAttributes } = storeToRefs(store);
</script>

<template>
  <div class="attribute-selector">
    <div
      v-for="attribute in availableAttributes"
      :key="attribute.name"
      class="attribute-group"
    >
      <label class="attribute-label">
        {{ attribute.name }}
        <span v-if="selectedAttributes[attribute.name]">
          : {{ selectedAttributes[attribute.name] }}
        </span>
      </label>

      <div class="attribute-options">
        <button
          v-for="value in attribute.values"
          :key="value"
          type="button"
          class="attribute-option"
          :class="{
            'attribute-option--active':
              selectedAttributes[attribute.name] === value,
          }"
          @click="store.selectAttribute(attribute.name, value)"
        >
          {{ value }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.attribute-selector {
  display: grid;
  gap: 20px;
}

.attribute-group {
  display: grid;
  gap: 10px;
}

.attribute-label {
  font-size: 14px;
  font-weight: 600;
}

.attribute-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.attribute-option {
  padding: 10px 16px;
  border: 1px solid #dedede;
  border-radius: 8px;
  background: transparent;
  color: inherit;
  cursor: pointer;
}

.attribute-option--active {
  border-color: var(--store-primary, #315c4c);
  background: color-mix(
    in srgb,
    var(--store-primary, #315c4c) 7%,
    transparent
  );
}
</style>