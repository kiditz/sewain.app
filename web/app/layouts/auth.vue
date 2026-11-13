<script setup lang="ts">
import LogoDark from "~/components/LogoDark.vue";
</script>

<template>
  <v-row class="bg-containerBg position-relative auth-layout">
    <div class="blur-logo">
      <svg
        width="100%"
        height="calc(100vh - 380px)"
        viewBox="0 0 130 120"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <path d="M10.4 66.82L0 55.38L15.6 52.26L10.4 66.82Z" fill="#0052D4" />
        <path
          d="M67.6001 6.23999C96.2001 6.23999 122.2 27.04 122.2 58.24C122.2 89.44 98.8001 112.84 67.6001 112.84C44.2001 112.84 20.8001 94.64 15.6001 71.24"
          stroke="url(#paint0_linear_6_32)"
          stroke-width="12.48"
          stroke-linecap="round"
        />
        <path
          d="M29.0952 50.44L61.1 24.44L93.1047 50.44"
          stroke="url(#paint1_linear_6_32)"
          stroke-width="11.44"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
        <path
          d="M61.1 78C66.8848 78 71.5743 73.8094 71.5743 68.64C71.5743 63.4706 66.8848 59.28 61.1 59.28C55.3152 59.28 50.6257 63.4706 50.6257 68.64C50.6257 73.8094 55.3152 78 61.1 78Z"
          stroke="url(#paint2_linear_6_32)"
          stroke-width="9.36"
        />
        <path d="M61.1001 78V92.04ZM61.1001 86.84H68.6649Z" fill="black" />
        <path
          d="M61.1001 78V92.04M61.1001 86.84H68.6649"
          stroke="url(#paint3_linear_6_32)"
          stroke-width="8.32"
          stroke-linecap="round"
        />
        <defs>
          <linearGradient
            id="paint0_linear_6_32"
            x1="15.6001"
            y1="6.23999"
            x2="122.2"
            y2="112.84"
            gradientUnits="userSpaceOnUse"
          >
            <stop stop-color="#00D2FF" />
            <stop offset="1" stop-color="#0052D4" />
          </linearGradient>
          <linearGradient
            id="paint1_linear_6_32"
            x1="29.0952"
            y1="24.44"
            x2="93.1047"
            y2="24.44"
            gradientUnits="userSpaceOnUse"
          >
            <stop stop-color="#FFD200" />
            <stop offset="1" stop-color="#F7971E" />
          </linearGradient>
          <linearGradient
            id="paint2_linear_6_32"
            x1="50.6257"
            y1="59.28"
            x2="69.2279"
            y2="80.0967"
            gradientUnits="userSpaceOnUse"
          >
            <stop stop-color="#00D2FF" />
            <stop offset="1" stop-color="#0052D4" />
          </linearGradient>
          <linearGradient
            id="paint3_linear_6_32"
            x1="61.1001"
            y1="78"
            x2="72.8256"
            y2="84.3177"
            gradientUnits="userSpaceOnUse"
          >
            <stop stop-color="#00D2FF" />
            <stop offset="1" stop-color="#0052D4" />
          </linearGradient>
        </defs>
      </svg>
    </div>
    <v-col cols="12">
      <div class="pt-6 pl-6">
        <logo-dark />
      </div>
    </v-col>
    <v-col cols="12" lg="12" class="d-flex align-center">
      <v-container>
        <div
          class="d-flex align-center justify-center"
          style="min-height: calc(100vh - 148px)"
        >
          <v-row justify="center">
            <v-col cols="12" md="12">
              <v-card elevation="0" class="loginBox">
                <v-card elevation="24">
                  <v-card-text class="pa-8">
                    <!---Login Form-->
                    <slot />
                    <!-- <v-btn
                      @click="store.loginWithGoogle"
                      color="secondary"
                      :loading="store.loading"
                      block
                      class="mt-5"
                      variant="outlined"
                      size="large"
                      type="submit"
                    >
                      <v-avatar
                        size="36"
                        variant="flat"
                        color="none"
                        class="mr-3 py-2"
                      >
                        <img src="@/assets/images/google.svg" />
                      </v-avatar>
                      {{ $t("login.continueGoogle") }}
                    </v-btn> -->
                    <!---Login Form-->
                  </v-card-text>
                </v-card>
              </v-card>
            </v-col>
          </v-row>
        </div>
      </v-container>
    </v-col>
  </v-row>
</template>

<style scoped lang="scss">
.auth-layout {
  min-height: 100vh;
  width: 100%;
  margin: 0;
  overflow-x: clip;
}

.auth-layout > .v-col {
  min-width: 0;
}

.auth-layout .blur-logo {
  pointer-events: none;
}
.loginBox {
  max-width: 475px;
  margin: 0 auto;
}
</style>
