<script setup lang="ts">
import { useForm } from "vee-validate";
import { toTypedSchema } from "@vee-validate/zod";
import { z } from "zod";
import type { Category, CategoryCommand } from "~/types/category";
import type { Media } from "~/types/media";
const props = defineProps<{
  category?: Category | null;
}>();

const { t } = useI18n();
const emit = defineEmits<{
  created: [];
}>();
const initialValues = {
  name: "",
  slug: "",
  parentId: null,
  imageUrl: null,
};

const snackbar = useSnackbarStore();
const { apiErrorMessage } = useApiError();

const schema = toTypedSchema(
  z.object({
    name: z
      .string()
      .min(1, t("category.validation.nameRequired"))
      .max(100, t("category.validation.nameMax")),

    slug: z
      .string()
      .max(100, t("category.validation.slugMax"))
      .regex(/^[a-z0-9]+(?:-[a-z0-9]+)*$/, t("category.validation.slugInvalid"))
      .or(z.literal("")),
    parentId: z.string().uuid().nullable(),
    imageUrl: z.string().nullable(),
  }),
);

const { handleSubmit, defineField, errors, isSubmitting, resetForm } = useForm({
  validationSchema: schema,
  initialValues,
});

const [name] = defineField("name");
const [slug] = defineField("slug");
const [parentId] = defineField("parentId");
const [imageUrl] = defineField("imageUrl");

const { createCategory, updateCategory } = useCategory();
const categoryStore = useCategoryStore();
const { categories } = storeToRefs(categoryStore);
await categoryStore.loadCategories();
const onSubmit = handleSubmit(async (values) => {
  try {
    const payload = {
      ...values,
      slug: values.slug.trim() || slugify(values.name),
    };

    if (props.category) {
      await updateCategory(props.category.id, payload);
    } else {
      await createCategory(payload);
    }
    resetForm({
      values: {
        name: "",
        slug: "",
        parentId: null,
      },
    });
    snackbar.openSnackbar({
      type: "success",
      message: "category.create.success",
    });
    categoryStore.refresh();
    emit("created");
  } catch (error: any) {
    snackbar.openSnackbar({
      type: "error",
      message: apiErrorMessage(error),
    });
  }
});

watch(
  () => props.category,
  (category) => {
    resetForm({
      values: category
        ? {
            name: category.name,
            slug: category.slug,
            parentId: category.parentId,
            imageUrl: category.imageUrl,
          }
        : initialValues,
    });
  },
  { immediate: true },
);
const showMedia = ref(false);
const open = () => {
  showMedia.value = true;
};
const onSelected = (media: Media | Media[]) => {
  const selected = Array.isArray(media) ? media[0] : media;
  imageUrl.value = selected?.url;
};
</script>
<template>
  <media-dialog v-model="showMedia" @selected="onSelected" />
  <v-form @submit.prevent="onSubmit">
    <v-row no-gutters>
      <v-col cols="12">
        <v-label :text="t('category.name')" class="mb-2" />

        <v-text-field
          v-model="name"
          variant="outlined"
          :error-messages="errors.name"
          :placeholder="t('category.namePlaceholder')"
        />
      </v-col>

      <v-col cols="12">
        <v-label :text="t('category.slug')" class="mb-2" />

        <v-text-field
          v-model="slug"
          variant="outlined"
          :error-messages="errors.slug"
          :placeholder="t('category.slugPlaceholder')"
          :hint="t('category.slugHint')"
        />
      </v-col>

      <v-col cols="12">
        <v-label :text="t('category.parent')" class="mt-2" />

        <v-autocomplete
          v-model="parentId"
          :items="categories"
          item-title="name"
          item-value="id"
          clearable
          variant="outlined"
          :error-messages="errors.parentId"
          :placeholder="t('category.parentPlaceholder')"
        />
      </v-col>
      <v-col cols="12">
        <category-image v-model="imageUrl" @open="open" />
      </v-col>

      <v-col cols="12" class="d-flex justify-end">
        <v-btn type="submit" color="primary" :loading="isSubmitting">
          {{ t("common.save") }}
        </v-btn>
      </v-col>
    </v-row>
  </v-form>
</template>
