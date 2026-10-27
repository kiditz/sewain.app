<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
} from "@ant-design/icons-vue";
import type {
  Attribute,
  AttributeCommand,
  AttributeValue,
} from "~/types/attribute";

const emit = defineEmits<{
  "attribute-updated": [attribute: Attribute];
}>();

const attributeIds = defineModel<string[]>("attributeIds", {
  default: () => [],
});

const valuesByAttribute = defineModel<Record<string, string[]>>(
  "valuesByAttribute",
  {
    default: () => ({}),
  },
);

const { findAttributes, createAttribute, updateAttribute, deleteAttribute } =
  useAttribute();
const { t } = useI18n();
const { apiErrorMessage } = useApiError();

const attributes = ref<Attribute[]>([]);
const loadingAttributes = ref(false);
const loadingAttributeId = ref<string | null>(null);
const savingEdit = ref(false);
const errorMessage = ref("");
const searchAttribute = ref("");
const creatingAttribute = ref(false);
const searchByAttribute = reactive<Record<string, string>>({});

const editDialog = ref(false);
const editingAttribute = ref<Attribute | null>(null);
const editingValue = ref<AttributeValue | null>(null);
const editingValueText = ref("");
const deleteDialog = ref(false);
const deletingValue = ref(false);
const pendingDelete = ref<{
  attribute: Attribute;
  value: AttributeValue;
} | null>(null);

function confirmDeleteValue(attribute: Attribute, value: AttributeValue) {
  pendingDelete.value = { attribute, value };
  deleteDialog.value = true;
}

const selectedAttributes = computed(() =>
  attributeIds.value
    .map((id) => attributes.value.find((attribute) => attribute.id === id))
    .filter((attribute): attribute is Attribute => attribute !== undefined),
);

onMounted(loadAttributes);
async function createNewAttribute() {
  const name = searchAttribute.value.trim();

  if (!name || creatingAttribute.value) return;

  const existing = attributes.value.find(
    (attribute) => attribute.name.trim().toLowerCase() === name.toLowerCase(),
  );

  if (existing) {
    if (!attributeIds.value.includes(existing.id)) {
      attributeIds.value = [...attributeIds.value, existing.id];
    }

    searchAttribute.value = "";
    return;
  }

  creatingAttribute.value = true;
  errorMessage.value = "";

  try {
    const created = await createAttribute({
      name,
      slug: slugify(name),
      description: "",
      values: [],
    });

    attributes.value = [...attributes.value, created];
    attributeIds.value = [...attributeIds.value, created.id];
    emit("attribute-updated", created);

    valuesByAttribute.value = {
      ...valuesByAttribute.value,
      [created.id]: [],
    };

    searchAttribute.value = "";
  } catch (error: any) {
    errorMessage.value = t(apiErrorMessage(error));
  } finally {
    creatingAttribute.value = false;
  }
}
async function loadAttributes() {
  loadingAttributes.value = true;
  errorMessage.value = "";

  try {
    attributes.value = await findAttributes();

    // Buang pilihan yang sudah tidak ada di backend.
    const validAttributeIds = new Set(
      attributes.value.map((attribute) => attribute.id),
    );

    attributeIds.value = attributeIds.value.filter((id) =>
      validAttributeIds.has(id),
    );

    // Pertahankan hanya value ID yang masih tersedia.
    const updatedSelections = { ...valuesByAttribute.value };

    for (const [attributeId, selectedIds] of Object.entries(
      updatedSelections,
    )) {
      const attribute = attributes.value.find(
        (item) => item.id === attributeId,
      );

      if (!attribute) {
        delete updatedSelections[attributeId];
        continue;
      }

      const validValueIds = new Set(attribute.values.map((value) => value.id));

      updatedSelections[attributeId] = selectedIds.filter((id) =>
        validValueIds.has(id),
      );
    }

    valuesByAttribute.value = updatedSelections;
  } catch (error: any) {
    errorMessage.value = t(apiErrorMessage(error));
  } finally {
    loadingAttributes.value = false;
  }
}

function slugify(value: string): string {
  return value
    .trim()
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-|-$/g, "");
}

function toCommand(
  attribute: Attribute,
  values: AttributeValue[],
): AttributeCommand {
  return {
    name: attribute.name,
    slug: attribute.slug,
    description: attribute.description,
    values: values.map((value) => ({
      ...(value.id ? { id: value.id } : {}),
      value: value.value,
      slug: value.slug,
    })),
  };
}

function setSelectedValues(attributeId: string, ids: string[]) {
  valuesByAttribute.value = {
    ...valuesByAttribute.value,
    [attributeId]: ids,
  };
}

function isDuplicateValue(attribute: Attribute, value: string) {
  return attribute.values.some(
    (item) => item.value.trim().toLowerCase() === value.trim().toLowerCase(),
  );
}

async function refreshAttribute(updated: Attribute) {
  const index = attributes.value.findIndex(
    (attribute) => attribute.id === updated.id,
  );

  if (index === -1) {
    await loadAttributes();
    return;
  }

  attributes.value[index] = updated;
  emit("attribute-updated", updated);
}

async function createValue(attribute: Attribute) {
  const value = (searchByAttribute[attribute.id] ?? "").trim();

  if (!value || loadingAttributeId.value) return;

  errorMessage.value = "";

  if (isDuplicateValue(attribute, value)) {
    errorMessage.value = `Value "${value}" sudah tersedia.`;
    return;
  }

  loadingAttributeId.value = attribute.id;

  try {
    const updated = await updateAttribute(
      attribute.id,
      toCommand(attribute, [
        ...attribute.values,
        {
          id: "",
          value,
          slug: slugify(value),
        },
      ]),
    );

    await refreshAttribute(updated);

    const createdValue = updated.values.find(
      (item) => item.slug === slugify(value),
    );

    if (createdValue) {
      const selectedIds = valuesByAttribute.value[attribute.id] ?? [];

      setSelectedValues(
        attribute.id,
        selectedIds.includes(createdValue.id)
          ? selectedIds
          : [...selectedIds, createdValue.id],
      );
    }

    searchByAttribute[attribute.id] = "";
  } catch (error: any) {
    errorMessage.value =
      t(apiErrorMessage(error));
  } finally {
    loadingAttributeId.value = null;
  }
}
async function saveValue() {
  const attribute = editingAttribute.value;
  const value = editingValue.value;
  const newValue = editingValueText.value.trim();

  if (!attribute || !value || !newValue || savingEdit.value) return;

  errorMessage.value = "";

  if (
    attribute.values.some(
      (item) =>
        item.id !== value.id &&
        item.value.trim().toLowerCase() === newValue.toLowerCase(),
    )
  ) {
    errorMessage.value = `Value "${newValue}" sudah tersedia.`;
    return;
  }

  savingEdit.value = true;

  try {
    const updated = await updateAttribute(
      attribute.id,
      toCommand(
        attribute,
        attribute.values.map((item) =>
          item.id === value.id
            ? {
                ...item,
                value: newValue,
                slug: slugify(newValue),
              }
            : item,
        ),
      ),
    );

    await refreshAttribute(updated);

    editDialog.value = false;
    editingAttribute.value = null;
    editingValue.value = null;
    editingValueText.value = "";
  } catch (error: unknown) {
    const apiError = error as {
      data?: { message?: string };
    };

    errorMessage.value =
      t(apiErrorMessage(apiError));
  } finally {
    savingEdit.value = false;
  }
}
function openEditDialog(attribute: Attribute, value: AttributeValue) {
  editingAttribute.value = attribute;
  editingValue.value = value;
  editingValueText.value = value.value;
  errorMessage.value = "";
  editDialog.value = true;
}
async function onConfirmDeleteValue() {
  const pending = pendingDelete.value;

  if (!pending || deletingValue.value) return;

  deletingValue.value = true;

  try {
    await deleteValue(pending.attribute, pending.value);
    deleteDialog.value = false;
    pendingDelete.value = null;
  } finally {
    deletingValue.value = false;
  }
}
async function deleteValue(attribute: Attribute, value: AttributeValue) {
  if (loadingAttributeId.value) return;

  errorMessage.value = "";
  loadingAttributeId.value = attribute.id;

  try {
    const updated = await updateAttribute(
      attribute.id,
      toCommand(
        attribute,
        attribute.values.filter((item) => item.id !== value.id),
      ),
    );

    await refreshAttribute(updated);

    setSelectedValues(
      attribute.id,
      (valuesByAttribute.value[attribute.id] ?? []).filter(
        (id) => id !== value.id,
      ),
    );
  } catch (error: any) {
    errorMessage.value =
      t(apiErrorMessage(error));
  } finally {
    loadingAttributeId.value = null;
  }
}
const deletingAttribute = ref(false);

const pendingDeleteAttribute = ref<Attribute | null>(null);

function confirmDeleteAttribute(attribute: Attribute) {
  pendingDeleteAttribute.value = attribute;
  deleteDialog.value = true;
}

async function onConfirmDeleteAttribute() {
  const attribute = pendingDeleteAttribute.value;

  if (!attribute || deletingAttribute.value) return;

  deletingAttribute.value = true;
  errorMessage.value = "";

  try {
    await deleteAttribute(attribute.id);

    attributes.value = attributes.value.filter(
      (item) => item.id !== attribute.id,
    );

    attributeIds.value = attributeIds.value.filter((id) => id !== attribute.id);

    const nextSelections = { ...valuesByAttribute.value };
    delete nextSelections[attribute.id];
    valuesByAttribute.value = nextSelections;

    pendingDeleteAttribute.value = null;
    deleteDialog.value = false;
  } catch (error: unknown) {
    const apiError = error as {
      data?: { message?: string };
    };

    errorMessage.value = t(apiErrorMessage(apiError));
  } finally {
    deletingAttribute.value = false;
  }
}
</script>
<template>
  <div class="d-flex flex-column ga-4">
    <v-alert
      v-if="errorMessage"
      type="error"
      variant="tonal"
      closable
      @click:close="errorMessage = ''"
    >
      {{ errorMessage }}
    </v-alert>

    <v-autocomplete
      v-model="attributeIds"
      v-model:search="searchAttribute"
      :items="attributes"
      item-title="name"
      item-value="id"
      :label="$t('ui.options')"
      :placeholder="$t('ui.select_or_create_an_attribute')"
      multiple
      chips
      closable-chips
      variant="outlined"
      density="compact"
      :loading="loadingAttributes || creatingAttribute"
      :disabled="loadingAttributes || creatingAttribute"
      hide-details
    >
      <template #item="{ props: itemProps, item }">
        <v-list-item v-bind="itemProps">
          <template #append>
            <v-btn
              icon
              variant="text"
              size="small"
              color="error"
              :disabled="deletingAttribute"
              @click.stop.prevent="confirmDeleteAttribute(item)"
            >
              <DeleteOutlined />
            </v-btn>
          </template>
        </v-list-item>
      </template>
      <template #no-data>
        <v-list-item
          v-if="searchAttribute.trim()"
          :title="`Create '${searchAttribute.trim()}'`"
          :prepend-icon="PlusOutlined"
          ,
          @click="createNewAttribute"
        />
        <v-list-item v-else :title="$t('ui.no_attributes_available')" />
      </template>
    </v-autocomplete>

    <div
      v-for="attribute in selectedAttributes"
      :key="attribute.id"
      class="d-flex flex-column ga-2"
    >
      <v-autocomplete
        v-model="valuesByAttribute[attribute.id]"
        v-model:search="searchByAttribute[attribute.id]"
        :items="attribute.values"
        item-title="value"
        item-value="id"
        :label="attribute.name"
        :placeholder="$t('ui.select_or_search_a_value')"
        multiple
        chips
        closable-chips
        variant="outlined"
        density="compact"
        :loading="loadingAttributeId === attribute.id"
        :disabled="loadingAttributeId === attribute.id"
        hide-details
      >
        <template #item="{ props: itemProps, item }">
          <v-list-item v-bind="itemProps">
            <template #append>
              <v-btn
                icon
                variant="text"
                size="small"
                color="error"
                :disabled="loadingAttributeId === attribute.id"
                class="value-delete-button"
                @click.stop.prevent="confirmDeleteValue(attribute, item)"
              >
                <DeleteOutlined />
              </v-btn>
              <v-btn
                icon
                variant="text"
                size="small"
                color="info"
                :disabled="loadingAttributeId === attribute.id"
                @click.stop.prevent="openEditDialog(attribute, item)"
              >
                <EditOutlined />
              </v-btn>
            </template>
          </v-list-item>
        </template>

        <template #no-data>
          <v-list-item
            v-if="(searchByAttribute[attribute.id] ?? '').trim()"
            :title="`Create '${(searchByAttribute[attribute.id] ?? '').trim()}'`"
            prepend-icon="mdi-plus"
            @click="createValue(attribute)"
          />
          <v-list-item v-else :title="$t('ui.no_values_available')" />
        </template>
      </v-autocomplete>
    </div>
    <ConfirmDeleteDialog
      v-model="deleteDialog"
      :title="
        pendingDeleteAttribute ? 'Delete attribute?' : 'Delete attribute value?'
      "
      :message="
        pendingDeleteAttribute
          ? `Are you sure you want to delete '${pendingDeleteAttribute.name}'?`
          : `Are you sure you want to delete '${pendingDelete?.value.value ?? ''}'?`
      "
      @confirmed="
        pendingDeleteAttribute
          ? onConfirmDeleteAttribute()
          : onConfirmDeleteValue()
      "
    />
    <v-dialog v-model="editDialog" max-width="420">
      <v-card :title="$t('ui.edit_attribute_value')">
        <v-card-text>
          <v-text-field
            v-model="editingValueText"
            :label="$t('collections.condition.value')"
            :placeholder="$t('attribute.valuePlaceholder')"
            variant="outlined"
            autofocus
            :disabled="savingEdit"
            @keyup.enter="saveValue"
          />
        </v-card-text>

        <v-card-actions>
          <v-spacer />

          <v-btn
            variant="text"
            :disabled="savingEdit"
            @click="editDialog = false"
          >
            {{ $t("common.cancel") }}
          </v-btn>

          <v-btn
            color="primary"
            variant="flat"
            :loading="savingEdit"
            :disabled="!editingValueText.trim()"
            @click="saveValue"
          >
            {{ $t("common.save") }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>
