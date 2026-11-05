<script setup lang="ts">
defineProps<{
  modelValue:boolean; target?:HTMLElement; title:string; searchLabel:string;
  search:string; selected:string|null; addLabel:string; emptyLabel:string;
  categories:{id:string;title:string;entries:{type:string;title:string;description?:string}[]}[];
}>();
const emit=defineEmits<{
  'update:modelValue':[value:boolean];'update:search':[value:string];
  'update:selected':[value:string];add:[];
}>();
const scrollbar=ref<{ $refs: {el?:{update:()=>void}}}>();
async function refresh(){await nextTick();scrollbar.value?.$refs.el?.update();}
</script>
<template>
  <v-menu :model-value="modelValue" :target="target" location="end top" :offset="8" :close-on-content-click="false" :width="760" :max-width="'calc(100vw - 24px)'" @update:model-value="emit('update:modelValue',$event)" @after-enter="refresh">
    <v-card class="catalog-picker" rounded="lg">
      <div class="catalog-picker__heading"><strong>{{title}}</strong><v-btn icon="mdi-close" size="x-small" variant="text" :aria-label="$t('common.cancel')" @click="emit('update:modelValue',false)" /></div>
      <div class="catalog-picker__body">
        <div class="catalog-picker__list">
          <v-text-field :model-value="search" :label="searchLabel" prepend-inner-icon="mdi-magnify" clearable hide-details variant="outlined" density="compact" class="ma-3" @update:model-value="emit('update:search',$event||'');refresh()" />
          <nuxt-scrollbar ref="scrollbar" class="catalog-picker__scroll" :options="{suppressScrollX:true,wheelPropagation:false}">
            <v-list density="compact" class="py-0">
              <template v-for="category in categories" :key="category.id">
                <v-list-subheader>{{category.title}}</v-list-subheader>
                <v-list-item v-for="entry in category.entries" :key="entry.type" :title="entry.title" :active="selected===entry.type" color="primary" @mouseenter="emit('update:selected',entry.type)" @focusin="emit('update:selected',entry.type)" @click="emit('update:selected',entry.type)">
                  <template #append><v-icon size="16">mdi-chevron-right</v-icon></template>
                </v-list-item>
              </template>
              <v-list-item v-if="!categories.length" :title="emptyLabel" />
            </v-list>
          </nuxt-scrollbar>
        </div>
        <div class="catalog-picker__preview"><slot v-if="selected" /><div v-else class="catalog-picker__empty"><v-icon size="36">mdi-view-dashboard-outline</v-icon><span>{{title}}</span></div></div>
      </div>
      <v-divider />
      <v-card-actions><v-spacer /><v-btn variant="text" @click="emit('update:modelValue',false)">{{$t('common.cancel')}}</v-btn><v-btn color="primary" variant="flat" :disabled="!selected" @click="emit('add')">{{addLabel}}</v-btn></v-card-actions>
    </v-card>
  </v-menu>
</template>
<style scoped>
.catalog-picker {display:flex;flex-direction:column;max-height:calc(100dvh - 32px);overflow:hidden}
.catalog-picker__heading {display:flex;align-items:center;justify-content:space-between;padding:12px 16px}
.catalog-picker__body {display:grid;grid-template-columns:280px minmax(0,1fr);min-height:0;height:min(500px,calc(100dvh - 180px))}
.catalog-picker__list {display:flex;flex-direction:column;min-height:0;border-inline-end:1px solid rgba(var(--v-theme-on-surface),.12)}
.catalog-picker__scroll {position:relative;flex:1;min-height:0}
.catalog-picker__preview {min-width:0;overflow:auto;background:rgba(var(--v-theme-on-surface),.04);padding:16px}
.catalog-picker__empty {display:flex;height:100%;align-items:center;justify-content:center;flex-direction:column;gap:12px;opacity:.6}
@media(max-width:600px){.catalog-picker__body{grid-template-columns:minmax(0,1fr) minmax(0,1fr)}.catalog-picker__preview{padding:8px}}
</style>
