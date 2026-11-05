<script setup lang="ts">
import PageAutocomplete from "../../PageAutocomplete.vue";
import { paymentIcons } from "../../../utils/payment-icons";
import { contentIcons } from "../../../utils/content-icons";
import ProductSelector from "../../ProductSelector.vue";
import { slideSettings } from "../../../utils/slideshow";
import AppearanceField from "../../AppearanceField.vue";
import { computed } from "vue";
import { allPaddingZero, type StorefrontBlock } from "~/types/storefront";
import EditorFieldRow from "../../EditorFieldRow.vue";
import PaddingField from "../../PaddingField.vue";
import LayoutField from "../../LayoutField.vue";
import TextField from "./TextField.vue";
import SocialLinkFields from "./SocialLinkFields.vue";
import BlockLinkField from "./BlockLinkField.vue";
import { isBlockLinkField } from "../../../utils/block-link";
import MenuField from "./MenuField.vue";
import TextLayoutField from "./TextLayoutField.vue";
import ProductCardStyleField from "./ProductCardStyleField.vue";
import ImageField from "./ImageField.vue";
import BorderRadiusField from "../../BorderRadiusField.vue";
import { useStorefront } from "../../../composables/useStorefront";
import { productControlTypes, productControlDefaults, objectSettings } from "../../../utils/product-information-layout";

const props = defineProps<{
  block: StorefrontBlock;
  collections: Array<{ id: string; name: string }>;
  productImage?: boolean;
  collectionCardTemplate?: boolean;
  resourceCardLayout?: string;
}>();
const { t } = useI18n();
const storefront = useStorefront();

const emit = defineEmits<{
  update: [key: string, value: unknown];
}>();

const collectionItems = computed(() =>
  props.collections.map((collection) => ({
    title: collection.name,
    value: collection.id,
  })),
);

const aspectRatioItems = computed(() => [
  { title: t("ui.auto"), value: "auto" },
  { title: t("ui.portrait"), value: "portrait" },
  { title: t("ui.square"), value: "square" },
  { title: t("ui.landscape"), value: "landscape" },
]);

function selectedAspectRatio(value: unknown): string {
  const ratio = String(value ?? "auto");
  if (["auto", "portrait", "square", "landscape"].includes(ratio)) return ratio;
  if (ratio === "1/1") return "square";
  if (["3/4", "2/3"].includes(ratio)) return "portrait";
  return "landscape";
}

function asSettings(value: unknown): Record<string, unknown> {
  return value !== null && typeof value === "object" && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : {};
}

const editableSettings = computed<Record<string, unknown>>(() =>
  Object.fromEntries(
    Object.entries(asSettings(props.block.settings)).filter(
      ([key]) =>
        !(props.resourceCardLayout&&props.block.type==='IMAGE'&&key==='aspectRatio') &&
        !(props.block.type==='RECOMMENDED_PRODUCTS'&&props.block.settings.displayType==='editorial'&&['columns','mobileColumns'].includes(key)) &&
        key !== "linkTargets" && !['formKind','recommendation'].includes(key) && !(props.block.type==='EMAIL_SIGNUP'&&key==='requireConsent') && !(props.collectionCardTemplate&&key==='collectionId') &&
        !(
          key === "displayType" &&
          props.block.type === "IMAGE" &&
          props.productImage
        ) &&
        !(
          props.block.type === "SOCIAL_LINK" &&
          ["network", "url", "label"].includes(key)
        ),
    ),
  ),
);

const blockSettings = computed<Record<string, unknown>>(() => {
  if(props.resourceCardLayout&&['COLLECTION_CARD','PRODUCT_CARD'].includes(props.block.type)) {
    const {height,customHeight,direction,position,alignTextBaseline,...layout}=asSettings(editableSettings.value.layout);
    return {...editableSettings.value,...(editableSettings.value.layout?{layout}: {})};
  }
  if (props.block.type === "RECOMMENDED_PRODUCTS") return {...editableSettings.value,itemsGap:editableSettings.value.itemsGap??18};
  if (props.block.type === "SLIDE") return slideSettings(editableSettings.value);
  if (productControlTypes.includes(props.block.type)) {
    const defaults = productControlDefaults(props.block.type);
    return {
      ...editableSettings.value,
      layout: { ...defaults.layout, ...objectSettings(editableSettings.value.layout) },
      padding: { ...defaults.padding, ...objectSettings(editableSettings.value.padding) },
    };
  }
  if (["PRODUCT_TITLE", "PRODUCT_PRICE"].includes(props.block.type)) {
    return {
      ...editableSettings.value,
      padding: editableSettings.value.padding ?? allPaddingZero,
      layout: {
        width: "match",
        maxWidth: "normal",
        ...asSettings(editableSettings.value.layout),
      },
    };
  }

  if (!["TEXT", "HEADING", "EYEBROW", "JUMBO_TEXT", "COPYRIGHT", "PRODUCT_DESCRIPTION", "COLLECTION_TITLE"].includes(props.block.type))
    return editableSettings.value;

  const { color, backgroundColor, ...textSettings } = editableSettings.value;
  return {
    ...textSettings,
    borderRadius: {
      topLeft: 0,
      topRight: 0,
      bottomRight: 0,
      bottomLeft: 0,
      ...asSettings(textSettings.borderRadius),
    },
    style: {
      color: "",
      backgroundColor: "#00000000",
      ...asSettings(textSettings.style),
    },
    padding: editableSettings.value.padding ?? allPaddingZero,
    layout: {
      width: "match",
      maxWidth: "normal",
      ...asSettings(editableSettings.value.layout),
    },
  };
});

const settingFields = computed<Array<{ key: string; value: unknown }>>(() =>
  Object.entries(blockSettings.value).map(([key, value]) => ({ key, value })),
);

function selectedIds(value: unknown): string[] {
  return String(value ?? "")
    .split(",")
    .filter(Boolean);
}

function labelOf(key: string): string {
  if (['fontSize','height','prefix','open','showTitle','size','openInNewTab'].includes(key)) return t(`blockCatalog.fields.${key}`);
  if (['aspectRatio','beforeLabel','afterLabel','buttonLabel','successMessage','requireConsent'].includes(key)) return t(`sectionCatalog.fields.${key}`);
  if (['question','title','speed','reverse','thickness'].includes(key)) return t(`sectionCatalog.fields.${key}`);
  if (props.block.type === "SLIDE" && key === "name") return t("storefrontSlideshow.name");
  if (
    props.block.type === "SOCIAL_LINK" &&
    ["size", "openInNewTab"].includes(key)
  )
    return t(`storefrontFooter.${key}`);
  const labels: Record<string, string> = {
    collectionIds: "ui.collections_to_show",
    level: "ui.heading_level",
    style: "ui.button_style",
    url: "ui.url",
    src: "ui.image_url",
    alt: "ui.alt_text",
    aspectRatio: "ui.aspect_ratio",
  };
  if (labels[key]) return t(labels[key]!);
  const label = key.replace(/([A-Z])/g, " $1");
  return label.charAt(0).toUpperCase() + label.slice(1);
}

function update(key: string, value: unknown) {
  emit("update", key, value);
}
</script>

<template>
  <div class="settings-fields">
    <p v-if="resourceCardLayout" class="text-body-small text-medium-emphasis px-4">{{t('sectionCatalog.mediaLayoutHint')}}</p>
    <SocialLinkFields
      v-if="block.type === 'SOCIAL_LINK'"
      :settings="block.settings"
      @update="update"
    />
    <EditorFieldRow
      v-if="block.type === 'IMAGE' && productImage"
      :label="t('ui.image_display_type')"
    >
      <v-select
        :model-value="String(block.settings.displayType ?? 'single')"
        :items="[
          { title: t('ui.single_image'), value: 'single' },
          { title: t('productList.types.carousel'), value: 'carousel' },
        ]"
        variant="outlined"
        density="compact"
        hide-details
        @update:model-value="update('displayType', String($event ?? 'single'))"
      />
    </EditorFieldRow>

    <template
      v-for="{ key, value } in settingFields"
      :key="`${block.id}:${key}`"
    >
      <EditorFieldRow v-if="key === 'pageId'" :label="t('blockCatalog.page')"><PageAutocomplete :model-value="typeof value==='string'?value:null" :kinds="['CUSTOM']" :label="t('blockCatalog.choosePage')" @select="update('pageId',$event.id)" /></EditorFieldRow>
      <EditorFieldRow v-else-if="block.type === 'RECOMMENDED_PRODUCTS' && key === 'displayType'" :label="t('ui.layout')"><v-select :model-value="value" :items="['grid','carousel','editorial'].map(value=>({value,title:t(`productList.types.${value}`)}))" variant="outlined" density="compact" hide-details @update:model-value="update('displayType',$event)" /></EditorFieldRow>
      <EditorFieldRow v-else-if="block.type === 'RECOMMENDED_PRODUCTS' && key === 'itemsGap'" :label="t('ui.gap')"><v-number-input :model-value="Number(value)" :min="0" :max="96" variant="outlined" density="compact" hide-details @update:model-value="update('itemsGap',$event??18)" /></EditorFieldRow>
      <EditorFieldRow v-else-if="key === 'collectionId'" :label="t('blockCatalog.collection')"><v-autocomplete :model-value="value" :items="collectionItems" variant="outlined" density="compact" clearable hide-details @update:model-value="update('collectionId',$event||'')" /></EditorFieldRow>
      <EditorFieldRow v-else-if="block.type==='PAYMENT_ICONS' && key==='methods'" :label="t('blockCatalog.paymentMethods')"><v-select :model-value="Array.isArray(value)?value:[]" :items="Object.keys(paymentIcons).map(value=>({value,title:t(`blockCatalog.payments.${value}`)}))" multiple chips closable-chips hide-details variant="outlined" density="compact" @update:model-value="update('methods',$event||[])" /></EditorFieldRow>
      <EditorFieldRow v-else-if="key === 'productId'" :label="t('sectionCatalog.product')"><ProductSelector :model-value="value ? [String(value)] : []" :label="t('sectionCatalog.product')" @update:model-value="update('productId', $event.at(-1) || '')" /></EditorFieldRow>
      <EditorFieldRow v-else-if="['ICON_TEXT','ICON'].includes(block.type) && key === 'icon'" :label="t('sectionCatalog.icon')"><v-select :model-value="value" :items="Object.keys(contentIcons).map(value => ({value,title:t(`sectionCatalog.icons.${value}`)}))" variant="outlined" density="compact" hide-details @update:model-value="update('icon',$event)" /></EditorFieldRow>
      <EditorFieldRow v-else-if="block.type === 'FAQ_ITEM' && key === 'answer'" :label="t('sectionCatalog.answer')"><v-textarea :model-value="String(value || '')" variant="outlined" density="compact" rows="4" auto-grow hide-details @update:model-value="update('answer',$event)" /></EditorFieldRow>
      <EditorFieldRow v-else-if="block.type === 'HOTSPOT' && ['x','y'].includes(key)" :label="t(`sectionCatalog.fields.${key}`)"><v-slider :model-value="Number(value)" :min="0" :max="100" :step="1" thumb-label hide-details @update:model-value="update(key,$event)" /></EditorFieldRow>
      <EditorFieldRow v-else-if="['DIVIDER','ICON'].includes(block.type) && key === 'color'" :label="t('sectionCatalog.fields.color')"><v-color-input :model-value="String(value || (block.type==='ICON'?storefront.theme.value.colors.primary:storefront.theme.value.colors.secondary))" hide-details variant="outlined" density="compact" @update:model-value="update('color',$event)" /></EditorFieldRow>
      <div v-else-if="block.type === 'SLIDE' && key === 'image'">
        <ImageField :settings="asSettings(value)" @update="(field, selected) => update('image', { ...asSettings(value), [field]: selected })" />
        <EditorFieldRow :label="t('ui.alt_text')">
          <v-text-field :model-value="String(asSettings(value).alt || '')" variant="outlined" density="compact" hide-details @update:model-value="update('image', { ...asSettings(value), alt: $event })" />
        </EditorFieldRow>
      </div>
      <AppearanceField v-else-if="block.type === 'SLIDE' && key === 'appearance'" :settings="blockSettings" @update="update" />
      <EditorFieldRow
        v-else-if="key === 'collectionIds'"
        :label="labelOf(String(key))"
      >
        <v-autocomplete
          :model-value="selectedIds(value)"
          :items="collectionItems"
          variant="outlined"
          density="compact"
          chips
          closable-chips
          multiple
          hide-details
          :aria-label="labelOf(String(key))"
          @update:model-value="
            update(key, Array.isArray($event) ? $event.join(',') : '')
          "
        />
      </EditorFieldRow>

      <EditorFieldRow
        v-else-if="isBlockLinkField(String(key), value)"
        :label="labelOf(String(key)).replace(/url|href/gi, 'Link')"
      >
        <BlockLinkField
          :settings="block.settings"
          :field-key="String(key)"
          :label="labelOf(String(key)).replace(/url|href/gi, 'Link')"
          @update="update"
        />
      </EditorFieldRow>

      <!-- <EditorFieldRow v-else-if="block.type === 'MENU' && key === 'menuId'" label="Menu">
        <v-select :model-value="value ?? null" :items="[
          ...storefront.MenuItems.value.map(menu => ({ title: menu.name, value: menu.id })),
        ]" variant="outlined" density="compact" hide-details aria-label="Navigation menu" @update:model-value="update('menuId', $event)" />
      </EditorFieldRow> -->
      <MenuField
        v-else-if="block.type === 'MENU' && key === 'menuId'"
        :model-value="typeof value === 'string' ? value : null"
        @update="update"
      />

      <EditorFieldRow
        v-else-if="key === 'aspectRatio'"
        :label="labelOf(String(key))"
      >
        <v-select
          :model-value="selectedAspectRatio(value)"
          :items="aspectRatioItems"
          variant="outlined"
          density="compact"
          hide-details
          :aria-label="labelOf(String(key))"
          @update:model-value="update(String(key), String($event ?? 'auto'))"
        />
      </EditorFieldRow>

      <!-- <LogoField v-else-if="key === 'src' && block.type === 'LOGO'" :settings="block.settings" @update="update" /> -->

      <template v-else-if="key === 'src' && block.type === 'IMAGE'">
        <ImageField
          v-if="!productImage"
          :settings="block.settings"
          @update="update"
        />
      </template>

      <EditorFieldRow
        v-else-if="typeof value === 'boolean'"
        :label="labelOf(String(key))"
      >
        <v-switch
          :model-value="value"
          color="primary"
          hide-details
          :aria-label="labelOf(String(key))"
          @update:model-value="update(String(key), $event)"
        />
      </EditorFieldRow>

      <EditorFieldRow
        v-else-if="typeof value === 'number'"
        :label="labelOf(String(key))"
      >
        <v-number-input
          :model-value="value"
          variant="outlined"
          density="compact"
          control-variant="stacked"
          hide-details
          :aria-label="labelOf(String(key))"
          @update:model-value="update(String(key), Number($event))"
        />
      </EditorFieldRow>

      <div
        class="d-flex flex-column"
        v-else-if="
          key === 'style' && ['EYEBROW', 'HEADING', 'TEXT', 'JUMBO_TEXT', 'COPYRIGHT', 'PRODUCT_DESCRIPTION', 'COLLECTION_TITLE'].includes(block.type)
        "
      >
        <div class="text-title-medium font-weight-bold px-4 py-2">Style</div>
        <v-divider />
        <div class="d-flex flex-column py-2 ga-2">
          <EditorFieldRow
            v-for="styleKey in ['color', 'backgroundColor']"
            :key="styleKey"
            :label="
              styleKey === 'color' ? 'Warna teks' : t('ui.background_color')
            "
          >
            <v-color-input
              :model-value="
                String(
                  asSettings(value)[styleKey] ||
                    (styleKey === 'backgroundColor'
                      ? '#00000000'
                      : block.type === 'EYEBROW'
                        ? storefront.theme.value.colors.primary
                        : storefront.theme.value.colors.text),
                )
              "
              color-format="hexa"
              variant="outlined"
              density="compact"
              hide-details
              color-pip
              :aria-label="
                styleKey === 'color' ? 'Warna teks' : t('ui.background_color')
              "
              @update:model-value="
                update('style', {
                  ...asSettings(value),
                  [styleKey]: $event ?? '',
                })
              "
            />
          </EditorFieldRow>
        </div>
      </div>

      <TextField
        v-else-if="key === 'text' || key === 'label'"
        :block="block"
        :field-key="key === 'label' ? 'label' : 'text'"
        @update="update"
      />

      <EditorFieldRow v-else-if="key === 'level'" :label="labelOf(String(key))">
        <v-select
          :model-value="String(value ?? '')"
          :items="['h1', 'h2', 'h3']"
          variant="outlined"
          density="compact"
          hide-details
          :aria-label="labelOf(String(key))"
          @update:model-value="update(String(key), String($event))"
        />
      </EditorFieldRow>

      <ProductCardStyleField
        v-else-if="key === 'style' && block.type === 'PRODUCT_CARD'"
        :value="value"
        @update="update('style', $event)"
      />
      <EditorFieldRow v-else-if="key === 'style'" :label="labelOf(String(key))">
        <v-select
          :model-value="String(value ?? '')"
          :items="['primary', 'secondary', 'outline']"
          variant="outlined"
          density="compact"
          hide-details
          :aria-label="labelOf(String(key))"
          @update:model-value="update(String(key), String($event))"
        />
      </EditorFieldRow>

      <PaddingField
        v-else-if="key === 'padding'"
        :settings="blockSettings"
        @update="update"
      />
      <LayoutField
        v-else-if="
          key === 'layout' &&
          (['HEADER', 'MENU', 'SLIDE', 'COLUMN', 'GROUP', 'ACCORDION', 'POPUP_LINK', 'COLLECTION_CARD', 'COMPARISON_SLIDER', 'CONTACT_FORM', 'EMAIL_SIGNUP', 'RECOMMENDED_PRODUCTS'].includes(block.type) || productControlTypes.includes(block.type) ||
            block.type.startsWith('CHECKOUT_'))
        "
        :settings="blockSettings"
        @update="update"
      />
      <TextLayoutField
        v-else-if="key === 'layout'"
        :settings="blockSettings"
        @update="update"
      />
      <BorderRadiusField
        v-else-if="
          key === 'borderRadius' &&
          ['IMAGE', 'PRODUCT_CARD', 'EYEBROW', 'HEADING', 'TEXT', 'JUMBO_TEXT', 'COPYRIGHT', 'PRODUCT_DESCRIPTION', 'COLLECTION_TITLE'].includes(
            block.type,
          )
        "
        :settings="blockSettings"
        :default-value="
          ['EYEBROW', 'HEADING', 'TEXT', 'JUMBO_TEXT', 'COPYRIGHT', 'PRODUCT_DESCRIPTION', 'COLLECTION_TITLE'].includes(block.type)
            ? 0
            : storefront.theme.value.layout.borderRadius
        "
        @update="update"
      />
      <EditorFieldRow v-else :label="labelOf(String(key))">
        <v-text-field
          :model-value="String(value ?? '')"
          variant="outlined"
          density="compact"
          hide-details
          :aria-label="labelOf(String(key))"
          @update:model-value="update(String(key), String($event ?? ''))"
        />
      </EditorFieldRow>
    </template>
  </div>
</template>

<style scoped>
.settings-fields {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
