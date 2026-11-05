<script setup lang="ts">
import type {BlockType,SectionType} from '~/types/storefront';
import type {Product} from '~/types/product';
import BlockRenderer from '~/components/storefront/BlockRenderer.vue';
import {createSection} from '../composables/useStorefrontPages';
import {createDefaultBlock} from '../utils/block-registry';
import {sectionRegistry} from '../utils/section-registry';
import {builderDndKey} from '../utils/builder-dnd';
import {useStorefront} from '../composables/useStorefront';
const props=defineProps<{kind:'section'|'block';type:SectionType|BlockType;title:string}>();
// Never let the catalog preview reorder the page being edited.
provide(builderDndKey,null);
const store=useStorefront();
const frame=ref<HTMLElement>();
const scale=ref(.5);
let observer:ResizeObserver|undefined;
onMounted(()=>{
  observer=new ResizeObserver(entries=>{
    const width=entries[0]?.contentRect.width;
    if(width)scale.value=width/800;
  });
  if(frame.value)observer.observe(frame.value);
});
onBeforeUnmount(()=>observer?.disconnect());
const section=computed(()=>props.kind==='section'?createSection(props.type as SectionType):null);
const block=computed(()=>props.kind==='block'?createDefaultBlock(props.type as BlockType):null);
const sampleProduct:Product={id:'catalog-preview',name:'Sample product',slug:'sample-product',media:'',description:'Discover the details of this product.',status:'ACTIVE',variants:[],createdAt:'',updatedAt:''};
const style=computed(()=>({
 '--store-primary':store.theme.value.colors.primary,'--store-secondary':store.theme.value.colors.secondary,
 '--store-background':store.theme.value.colors.background,'--store-surface':store.theme.value.colors.surface,
 '--store-image-background':store.theme.value.colors.surface,'--store-text':store.theme.value.colors.text,
 '--store-on-primary':'rgb(var(--v-theme-on-primary))','--store-heading-font':store.theme.value.typography.headingFont,
 '--store-radius':`${store.theme.value.layout.borderRadius}px`,'--store-width':`${store.theme.value.layout.contentMaxWidth}px`,
 '--store-spacing':`${store.theme.value.layout.sectionSpacing}px`,fontFamily:store.theme.value.typography.bodyFont,
 background:store.theme.value.colors.background,color:store.theme.value.colors.text,
}));
</script>
<template>
  <div><strong class="catalog-entry-title">{{title}}</strong>
    <div ref="frame" class="catalog-entry-frame" :style="style" inert>
      <div class="catalog-entry-canvas" :style="{zoom:scale}">
        <Suspense :key="`${kind}:${type}`">
          <component v-if="section" :is="sectionRegistry[section.type].component" :settings="section.settings" :blocks="section.blocks" :preview="true" :product="sampleProduct" />
          <BlockRenderer v-else-if="block" :blocks="[block]" :preview="true" :product-context="sampleProduct" />
          <template #fallback><div class="pa-8 text-center"><v-progress-circular indeterminate size="24" /></div></template>
        </Suspense>
      </div>
    </div>
  </div>
</template>
<style scoped>
.catalog-entry-title{display:block;margin-bottom:12px;font-size:14px}
.catalog-entry-frame{overflow:hidden;border:1px solid rgba(var(--v-theme-on-surface),.12);border-radius:8px;pointer-events:none;min-height:160px;min-width:0}
.catalog-entry-canvas{width:800px;min-height:320px}

</style>
