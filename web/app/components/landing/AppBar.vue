<script setup lang="ts">
import logo from "~/assets/images/logo.svg";

const { t } = useI18n();
</script>

<template>
  <v-app-bar
    elevation="0"
    color="transparent"
    class="landing-app-bar"
  >
    <v-container
      class="d-flex align-center"
      max-width="1200"
    >
      <!-- Logo -->
      <nuxt-link
        to="/"
        class="d-flex align-center text-decoration-none"
      >
        <img
          :src="logo"
          width="200"
          height="35"
          alt="Sewain"
        />
      </nuxt-link>

      <v-spacer />

      <!-- Desktop navigation -->
      <div class="d-none d-md-flex align-center ga-2">
        <v-btn
          variant="text"
          href="#features"
        >
          {{ $t("ui.features") }}
        </v-btn>

        <v-btn
          variant="text"
          href="#how-it-works"
        >
          {{ $t("ui.how_it_works") }}
        </v-btn>

        <v-btn
          variant="text"
          href="#pricing"
        >
          {{ $t("ui.pricing") }}
        </v-btn>
      </div>

      <v-spacer class="d-none d-md-flex" />

      <!-- Actions -->
      <div class="d-none d-sm-flex align-center ga-2">
        <v-btn
          to="/login"
          variant="text"
        >
          {{ $t("login.submit") }}
        </v-btn>

        <v-btn
          to="/register"
          color="primary"
          variant="flat"
          rounded="lg"
        >
          {{ $t("ui.get_started") }}
        </v-btn>
      </div>

      <!-- Mobile menu -->
      <v-menu>
        <template #activator="{ props }">
          <v-btn
            v-bind="props"
            icon="mdi-menu"
            variant="text"
            class="d-flex d-sm-none"
          />
        </template>

        <v-list min-width="220">
          <v-list-item
            href="#features"
            :title="$t('ui.features')"
          />

          <v-list-item
            href="#how-it-works"
            :title="$t('ui.how_it_works')"
          />

          <v-list-item
            href="#pricing"
            :title="$t('ui.pricing')"
          />

          <v-divider class="my-2" />

          <v-list-item
            to="/login"
            :title="$t('login.submit')"
          />

          <v-list-item
            to="/register"
            :title="$t('ui.get_started')"
          />
        </v-list>
      </v-menu>
    </v-container>
  </v-app-bar>
</template>