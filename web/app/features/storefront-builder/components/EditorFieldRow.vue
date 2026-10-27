<script setup lang="ts">
defineProps<{ label: string }>();
</script>

<template>
  <div class="editor-row">
    <div class="editor-row__label text-body-medium">{{ label }}</div>
    <div class="editor-row__field">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.editor-row {
  display: grid;
  grid-template-columns: 100px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  padding-inline: 16px;
}

.editor-row__field {
  display: flex;
  justify-content: flex-end;
  min-width: 0;
}

.editor-row__field :deep(.v-input) {
  width: 100%;
}

.editor-row__field > * {
  width: 100%;
  min-width: 0;
}

.editor-row__field :deep(.v-switch) {
  width: auto;
  margin-inline-start: auto;
}

.editor-row__field :deep(.v-btn-toggle) {
  width: 100%;
  min-width: 0;
  overflow: hidden;
}

.editor-row__field :deep(.v-btn) {
  flex: 1 1 0;
  min-width: 0;
  font-size: small;
}
</style>
