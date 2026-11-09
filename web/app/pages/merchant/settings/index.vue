<script setup lang="ts">
import { toTypedSchema } from "@vee-validate/zod";
import { useForm } from "vee-validate";
import { z } from "zod";
import type { Media } from "~/types/media";
import type { Merchant, MerchantCommand } from "~/types/merchant";

definePageMeta({
  layout: "merchant",
  middleware: ["merchant"]
});

type MediaTarget = "logo" | "favicon";

const { t } = useI18n();
useHead({ title: () => t("menu.storeSettings") });
const showMedia = ref(false);
const mediaTarget = ref<MediaTarget>("logo");
const store = useMerchantStore();

const schema = toTypedSchema(
  z.object({
    name: z.string().trim().min(1, t("store.validation.nameRequired")),
    slug: z
      .string()
      .trim()
      .min(1, t("store.validation.slugRequired"))
      .regex(
        /^[a-z0-9]+(?:-[a-z0-9]+)*$/,
        t("store.validation.slugRegex"),
      ),
    description: z.string().optional(),
    logoUrl: z.union([z.literal(""), z.string().url()]).optional(),
    faviconUrl: z.union([z.literal(""), z.string().url()]).optional(),
    contactEmail: z.union([z.literal(""), z.string().email()]).optional(),
    contactPhone: z.string().optional(),
    customDomain: z.string().optional(),
    published: z.boolean(),
    businessName: z.string().optional(),
    businessAddress: z.string().optional(),
    businessEmail: z.union([z.literal(""), z.string().email()]).optional(),
    businessPhone: z.string().optional(),
    taxId: z.string().optional(),
  }),
);

const { updateMerchant } = useMerchant();

const { current: merchant } = storeToRefs(useMerchantStore());
const { defineField, handleSubmit, errors, isSubmitting, meta, resetForm } =
  useForm({
    validationSchema: schema,
    initialValues: {
      name: "",
      slug: "",
      description: "",
      logoUrl: "",
      faviconUrl: "",
      contactEmail: "",
      contactPhone: "",
      customDomain: "",
      published: false,
      businessName: "",
      businessAddress: "",
      businessEmail: "",
      businessPhone: "",
      taxId: "",
    },
  });

const [name] = defineField("name");
const [slug] = defineField("slug");
const [description] = defineField("description");
const [logoUrl] = defineField("logoUrl");
const [faviconUrl] = defineField("faviconUrl");
const [contactEmail] = defineField("contactEmail");
const [contactPhone] = defineField("contactPhone");
const [customDomain] = defineField("customDomain");
const [published] = defineField("published");
const [businessName] = defineField("businessName");
const [businessAddress] = defineField("businessAddress");
const [businessEmail] = defineField("businessEmail");
const [businessPhone] = defineField("businessPhone");
const [taxId] = defineField("taxId");

const storeUrl = computed(() =>
  slug.value ? `${slug.value}.sewain.app` : "your-store.sewain.app",
);

function toFormValues(current: Merchant) {
  return {
    name: current.name,
    slug: current.slug,
    description: current.storeInformation?.description ?? "",
    logoUrl: current.storeInformation?.logoUrl ?? "",
    faviconUrl: current.storeInformation?.faviconUrl ?? "",
    contactEmail: current.storeInformation?.contactEmail ?? "",
    contactPhone: current.storeInformation?.contactPhone ?? "",
    customDomain: current.customDomain ?? "",
    published: current.published,
    businessName: current.businessInformation?.name ?? "",
    businessAddress: current.businessInformation?.address ?? "",
    businessEmail: current.businessInformation?.email ?? "",
    businessPhone: current.businessInformation?.phone ?? "",
    taxId: current.businessInformation?.taxId ?? "",
  };
}

watch(
  merchant,
  (current) => {
    if (current) {
      resetForm({ values: toFormValues(current) });
    }
  },
  { immediate: true },
);

function openMediaDialog(target: MediaTarget) {
  mediaTarget.value = target;
  showMedia.value = true;
}

const onSelected = (media: Media | Media[]) => {
  const selected = Array.isArray(media) ? media[0] : media;
  if (!selected) return;

  if (mediaTarget.value === "logo") {
    logoUrl.value = selected.url;
  } else {
    faviconUrl.value = selected.url;
  }
};


const snackbar = useSnackbarStore()

const saveSettings = handleSubmit(async (values) => {
  

  if (!merchant.value) {
    snackbar.openSnackbar({message: t("store.messages.noStore"), type: 'error'});
    return;
  }

  const payload: MerchantCommand = {
    name: values.name,
    slug: values.slug,
    storeInformation: {
      description: values.description || null,
      logoUrl: values.logoUrl || null,
      faviconUrl: values.faviconUrl || null,
      contactEmail: values.contactEmail || null,
      contactPhone: values.contactPhone || null,
    },
    businessInformation: {
      name: values.businessName || null,
      address: values.businessAddress || null,
      email: values.businessEmail || null,
      phone: values.businessPhone || null,
      taxId: values.taxId || null,
    },
    customDomain: values.customDomain || null,
    published: values.published,
  };

  try {
    console.log(merchant.value)
    const updated = await updateMerchant(merchant.value.id, payload);
    store.setCurrent(updated)
    merchant.value = updated;
    resetForm({ values: toFormValues(updated) });
    snackbar.openSnackbar({message: t("store.messages.success"), type: "success"})
  } catch {
    snackbar.openSnackbar({message: t("store.messages.error"), type: "error"})
  }
});
</script>
<template>
  <MediaDialog v-model="showMedia" @selected="onSelected" />
  <v-container fluid class="pa-4 pa-md-6">
    <div class="d-flex flex-wrap align-center justify-space-between ga-4 mb-6">
      <div>
        <span class="text-headline-small font-weight-bold">{{ t("store.title") }}</span>
        <p class="text-body-medium text-medium-emphasis">
          {{ t("store.subtitle") }}
        </p>
      </div>

      <v-btn
        color="primary"
        prepend-icon="mdi-content-save-outline"
        :loading="isSubmitting"
        :disabled="!meta.dirty"
        @click="saveSettings"
      >
        {{ t("store.saveChanges") }}
      </v-btn>
    </div>

    <v-form @submit.prevent="saveSettings">
      <v-row>
        <v-col cols="12" lg="8">
          <!-- Store information -->
          <v-card variant="outlined" rounded="lg" flat class="mb-5">
            <v-card-item>
              <template #prepend>
                <v-avatar color="primary" variant="tonal" rounded="lg">
                  <v-icon icon="mdi-store-outline" />
                </v-avatar>
              </template>
              <v-card-title class="text-title-large">{{ t("store.info.title") }}</v-card-title>
              <v-card-subtitle class="text-body-small">
                {{ t("store.info.subtitle") }}
              </v-card-subtitle>
            </v-card-item>

            <v-divider />

            <v-card-text class="pa-5">
              <v-row>
                <v-col cols="12">
                  <div class="text-title-small mb-2">{{ t("store.info.logoLabel") }}</div>

                  <div class="d-flex align-center ga-4">
                    <v-avatar size="76" rounded="lg">
                      <v-img v-if="logoUrl" :src="logoUrl" cover :alt="t('store.info.logoLabel')" />
                      <v-icon v-else icon="mdi-image-outline" size="30" />
                    </v-avatar>

                    <div class="d-flex flex-column ga-2">
                      <v-btn
                        variant="outlined"
                        prepend-icon="mdi-image-multiple-outline"
                        @click="openMediaDialog('logo')"
                      >
                        {{ t("store.info.selectImage") }}
                      </v-btn>

                      <v-btn
                        v-if="logoUrl"
                        variant="text"
                        color="error"
                        size="small"
                        @click="logoUrl = ''"
                      >
                        {{ t("store.info.removeLogo") }}
                      </v-btn>
                    </div>
                  </div>
                </v-col>

                <v-col cols="12" md="6">
                  <v-text-field
                    v-model="name"
                    :label="t('store.info.nameLabel')"
                    :placeholder="t('store.info.namePlaceholder')"
                    variant="outlined"
                    :error-messages="errors.name"
                    required
                  />
                </v-col>

                <v-col cols="12" md="6">
                  <v-text-field
                    v-model="slug"
                    :label="t('store.info.slugLabel')"
                    :placeholder="t('store.info.slugPlaceholder')"
                    variant="outlined"
                    :error-messages="errors.slug"
                    :hint="t('store.info.slugHint')"
                    persistent-hint
                    required
                  />
                </v-col>

                <v-col cols="12">
                  <div class="text-body-medium text-medium-emphasis mb-2">
                    {{ t("store.info.urlPreview") }}
                  </div>
                  <div class="d-flex align-center ga-2">
                    <v-icon icon="mdi-web" color="primary" />
                    <span class="text-body-medium">{{ storeUrl }}</span>
                  </div>
                </v-col>

                <v-col cols="12">
                  <v-textarea
                    v-model="description"
                    :label="t('store.info.descriptionLabel')"
                    :placeholder="t('store.info.descriptionPlaceholder')"
                    variant="outlined"
                    rows="3"
                    auto-grow
                    :error-messages="errors.description"
                  />
                </v-col>

                <v-col cols="12" md="12">
                  <div class="text-title-small mb-2">{{ t("store.info.faviconLabel") }}</div>

                  <div class="d-flex align-center ga-3">
                    <v-avatar size="40" rounded="md" color="surface-variant">
                      <v-img
                        v-if="faviconUrl"
                        :src="faviconUrl"
                        :alt="t('store.info.faviconLabel')"
                      />
                      <v-icon v-else icon="mdi-web" />
                    </v-avatar>

                    <v-btn
                      variant="outlined"
                      prepend-icon="mdi-image-multiple-outline"
                      @click="openMediaDialog('favicon')"
                    >
                      {{ t("store.info.selectFavicon") }}
                    </v-btn>
                  </div>
                </v-col>

                <v-col cols="12" md="6">
                  <v-text-field
                    v-model="contactEmail"
                    :label="t('store.info.contactEmailLabel')"
                    :placeholder="t('store.info.contactEmailPlaceholder')"
                    prepend-inner-icon="mdi-email-outline"
                    variant="outlined"
                    :error-messages="errors.contactEmail"
                  />
                </v-col>

                <v-col cols="12" md="6">
                  <v-text-field
                    v-model="contactPhone"
                    :label="t('store.info.contactPhoneLabel')"
                    :placeholder="t('store.info.contactPhonePlaceholder')"
                    prepend-inner-icon="mdi-phone-outline"
                    variant="outlined"
                    :error-messages="errors.contactPhone"
                  />
                </v-col>
              </v-row>
            </v-card-text>
          </v-card>

          <!-- Online store -->
          <v-card rounded="lg" border flat class="mb-5">
            <v-card-item>
              <template #prepend>
                <v-avatar color="primary" variant="tonal" rounded="lg">
                  <v-icon icon="mdi-web" />
                </v-avatar>
              </template>
              <v-card-title class="text-title-large">{{ t("store.online.title") }}</v-card-title>
              <v-card-subtitle class="text-body-small">
                {{ t("store.online.subtitle") }}
              </v-card-subtitle>
            </v-card-item>

            <v-divider />

            <v-card-text class="pa-5">
              <v-text-field
                v-model="customDomain"
                :label="t('store.online.customDomainLabel')"
                :placeholder="t('store.online.customDomainPlaceholder')"
                prepend-inner-icon="mdi-web"
                variant="outlined"
                :error-messages="errors.customDomain"
                :hint="t('store.online.customDomainHint')"
                persistent-hint
              />

              <v-divider class="my-4" />

              <div class="d-flex align-center justify-space-between ga-4">
                <div>
                  <div class="text-title-small">{{ t("store.online.publishLabel") }}</div>
                  <div class="text-body-medium text-medium-emphasis">
                    {{ t("store.online.publishDesc") }}
                  </div>
                </div>
                <v-switch
                  v-model="published"
                  color="primary"
                  hide-details
                  inset
                />
              </div>
            </v-card-text>
          </v-card>

          <!-- Business information -->
          <v-card rounded="lg" border flat>
            <v-card-item>
              <template #prepend>
                <v-avatar color="primary" variant="tonal" rounded="lg">
                  <v-icon icon="mdi-domain" />
                </v-avatar>
              </template>
              <v-card-title class="text-title-large">{{ t("store.business.title") }}</v-card-title>
              <v-card-subtitle class="text-body-small">
                {{ t("store.business.subtitle") }}
              </v-card-subtitle>
            </v-card-item>

            <v-divider />

            <v-card-text class="pa-5">
              <v-row>
                <v-col cols="12">
                  <v-text-field
                    v-model="businessName"
                    :label="t('store.business.nameLabel')"
                    :placeholder="t('store.business.namePlaceholder')"
                    variant="outlined"
                    :error-messages="errors.businessName"
                  />
                </v-col>

                <v-col cols="12">
                  <v-textarea
                    v-model="businessAddress"
                    :label="t('store.business.addressLabel')"
                    :placeholder="t('store.business.addressPlaceholder')"
                    variant="outlined"
                    rows="3"
                    auto-grow
                    :error-messages="errors.businessAddress"
                  />
                </v-col>

                <v-col cols="12" md="6">
                  <v-text-field
                    v-model="businessEmail"
                    :label="t('store.business.emailLabel')"
                    :placeholder="t('store.business.emailPlaceholder')"
                    variant="outlined"
                    :error-messages="errors.businessEmail"
                  />
                </v-col>

                <v-col cols="12" md="6">
                  <v-text-field
                    v-model="businessPhone"
                    :label="t('store.business.phoneLabel')"
                    :placeholder="t('store.business.phonePlaceholder')"
                    variant="outlined"
                    :error-messages="errors.businessPhone"
                  />
                </v-col>

                <v-col cols="12">
                  <v-text-field
                    v-model="taxId"
                    :label="t('store.business.taxIdLabel')"
                    :placeholder="t('store.business.taxIdPlaceholder')"
                    variant="outlined"
                    :error-messages="errors.taxId"
                  />
                </v-col>
              </v-row>
            </v-card-text>
          </v-card>

          <!-- <div class="d-flex justify-end mt-5">
            <v-btn
              color="primary"
              type="submit"
              prepend-icon="mdi-content-save-outline"
              :loading="isSubmitting"
              :disabled="!meta.dirty"
            >
              {{ t("store.saveChanges") }}
            </v-btn>
          </div> -->
        </v-col>

        <!-- Store status sidebar -->
        <v-col cols="12" lg="4">
          <v-card rounded="lg" border flat>
            <v-card-item>
              <v-card-title class="text-title-large">{{ t("store.status.title") }}</v-card-title>
            </v-card-item>

            <v-divider />

            <v-card-text>
              <div class="d-flex align-center justify-space-between mb-4">
                <span class="text-body-medium text-medium-emphasis">
                  {{ t("store.status.storefront") }}
                </span>
                <v-chip
                  :color="published ? 'success' : 'default'"
                  size="small"
                  variant="tonal"
                >
                  {{ published ? t("store.status.published") : t("store.status.unpublished") }}
                </v-chip>
              </div>

              <div class="text-body-medium text-medium-emphasis mb-1">{{ t("store.status.urlLabel") }}</div>
              <div class="d-flex align-center ga-2 mb-4">
                <v-icon icon="mdi-link-variant" size="small" />
                <span class="text-body-medium text-break">{{ storeUrl }}</span>
              </div>

              <v-divider class="mb-4" />

              <div class="text-body-medium text-medium-emphasis">
                {{ t("store.status.footerNote") }}
              </div>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </v-form>
  </v-container>
</template>