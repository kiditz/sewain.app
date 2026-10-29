<script setup lang="ts">
import type { StorefrontBlock } from "~/types/storefront";
import { useCheckoutContext } from "~/features/storefront-checkout/composables/checkout-context";
import { useCheckoutBlockStyle } from "../../composables/useCheckoutBlockStyle";
const props = defineProps<{ block: StorefrontBlock; preview?: boolean }>();
const { t, notes, notesAttrs } = useCheckoutContext();
const blockStyle = useCheckoutBlockStyle(() => props.block);
</script>
<template>
<section class="checkout-card" :style="blockStyle">
            <div class="checkout-section-heading">
              <div class="checkout-section-heading__icon">
                <v-icon icon="mdi-note-text-outline" size="21" />
              </div>
              <div>
                <h2>{{ block.settings.title || t("checkout.notesTitle") }}</h2>
                <p>{{ t("checkout.notesDescription") }}</p>
              </div>
            </div>
            <v-textarea
              v-model="notes"
              v-bind="notesAttrs"
              :label="t('checkout.notes')"
              :placeholder="t('checkout.notesPlaceholder')"
              variant="outlined"
              rows="2"
              auto-grow
              hide-details
            />
          </section>
</template>
