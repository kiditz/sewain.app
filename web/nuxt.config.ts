export default defineNuxtConfig({
  compatibilityDate: "2025-07-15",
  // devServer: {
  //   host: "0.0.0.0",
  //   port: 3000,
  // },

  app: {
    head: {
      title: "Sewain",
      titleTemplate: "%s | Sewain",
      link: [
        {
          key: "favicon",
          rel: "icon",
          type: "image/x-icon",
          href: "/favicon.ico",
        },
      ],
    },
  },

  devtools: {
    enabled: false,
  },

  modules: [
    "vuetify-nuxt-module",
    "@nuxtjs/i18n",
    "@pinia/nuxt",
    "pinia-plugin-persistedstate/nuxt",
    "@davestewart/nuxt-scrollbar",
  ],

  vuetify: {
    moduleOptions: {
      prefixComposables: ["useLayout"],
    },
    vuetifyOptions: {
      theme: {
        defaultTheme: "light",
        themes: {
          light: {
            colors: {
              primary: "#3f51b5",
              secondary: "#8c8c8c",
              info: "#13c2c2",
              success: "#52c41a",
              accent: "#FFAB91",
              warning: "#faad14",
              error: "#ff4d4f",
              lightprimary: "#e6f4ff",
              lightsecondary: "#f5f5f5",
              lightsuccess: "#EAFCD4",
              lighterror: "#FFE7D3",
              lightwarning: "#FFF6D0",
              darkText: "#212121",
              lightText: "#8c8c8c",
              darkprimary: "#0958d9",
              darksecondary: "#7a7878",
              borderLight: "#e6ebf1",
              inputBorder: "#a1a1a5",
              containerBg: "#fafafb",
              surface: "#fff",
              facebook: "#4267b2",
              twitter: "#1da1f2",
              linkedin: "#0e76a8",
              gray100: "#f5f5f5",
              primary200: "#a1d2ff",
              secondary200: "#eeeeee",
            },
          },
        },
      },
    },
  },
  css: ["~/scss/style.scss"],

  i18n: {
    // Language follows the merchant preference without changing storefront URLs.
    strategy: "no_prefix",
    defaultLocale: "en",
    locales: [
      {
        code: "id",
        name: "Indonesia",
        file: "id/index.ts",
      },
      {
        code: "en",
        name: "English",
        file: "en/index.ts",
      },
    ],
    vueI18n: "./i18n.config.ts",
  },

  runtimeConfig: {
    public: {
      apiBaseUrl: "http://localhost:8080",
    },
  },
});
