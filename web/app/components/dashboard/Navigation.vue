<script setup lang="ts">
import {
  DashboardOutlined,
  CalendarOutlined,
  ShoppingOutlined,
  InboxOutlined,
  TeamOutlined,
  DollarOutlined,
  BarChartOutlined,
  ShopOutlined,
  FileTextOutlined,
  SettingOutlined,
  CreditCardOutlined,
  GlobalOutlined,
  FieldTimeOutlined,
  AppstoreOutlined
} from "@ant-design/icons-vue";

import logoSm from "~/assets/images/logo-sm.svg";
import logo from "~/assets/images/logo.svg";

const { t } = useI18n();
const route = useRoute();
const customizer = useCustomizerStore();

const menuSections = computed(() => [
  {
    title: t("menu.overview"),
    items: [
      {
        title: t("menu.dashboard"),
        icon: DashboardOutlined,
        to: "/merchant",
      },
      {
        title: t("menu.calendar"),
        icon: CalendarOutlined,
        to: "/merchant/calendar",
      },
    ],
  },
  {
    title: t("menu.rentalOperations"),
    items: [
      {
        title: t("menu.orders"),
        icon: ShoppingOutlined,
        to: "/merchant/orders",
      },
      {
        title: t("menu.customers"),
        icon: TeamOutlined,
        to: "/merchant/customers",
      },
    ],
  },
  {
    title: t("menu.inventoryCatalog"),
    items: [
      {
        title: t("menu.products"),
        icon: ShoppingOutlined,
        to: "/merchant/products",
      },
      {
        title: t("menu.collections"),
        icon: AppstoreOutlined,
        to: "/merchant/collections",
      },
      {
        title: t("menu.inventory"),
        icon: InboxOutlined,
        to: "/merchant/inventory",
      },
    ],
  },
  {
    title: t("menu.finance"),
    items: [
      {
        title: t("menu.financeOverview"),
        icon: DollarOutlined,
        to: "/merchant/finance",
      },
      {
        title: t("menu.reports"),
        icon: BarChartOutlined,
        to: "/merchant/reports",
      },
    ],
  },
  {
    title: t("menu.storeManagement"),
    items: [
      {
        title: t("menu.storefront"),
        icon: ShopOutlined,
        to: "/merchant/storefront",
      },
      {
        title: "Pages",
        icon: FileTextOutlined,
        to: "/merchant/pages",
      },
    ],
  },
  {
    title: t("menu.settings"),
    items: [
      {
        title: t("menu.storeSettings"),
        icon: SettingOutlined,
        to: "/merchant/settings",
      },
      {
        title: t("menu.integrations"),
        icon: AppstoreOutlined,
        to: "/merchant/integrations",
      },
      {
        title: t("menu.regionalSettings"),
        icon: GlobalOutlined,
        to: "/merchant/settings/regional",
      },
      {
        title: t("menu.rentalSettings"),
        icon: CalendarOutlined,
        to: "/merchant/settings/rental",
      },
      {
        title: t("menu.rentalPlans"),
        icon: FieldTimeOutlined,
        to: "/merchant/settings/rental-plans",
      },
      {
        title: t("menu.paymentSettings"),
        icon: CreditCardOutlined,
        to: "/merchant/settings/payments",
      },
    ],
  },
]);

function isActive(to: string) {
  if (to === "/merchant" || to === "/merchant/settings") {
    return route.path === to;
  }

  return route.path === to || route.path.startsWith(`${to}/`);
}
</script>

<template>
  <v-navigation-drawer
    v-model="customizer.drawer"
    class="leftSidebar"
    :rail="customizer.miniSidebar"
    :rail-width="62"
    :width="256"
    mobile-breakpoint="lg"
    :permanent="!$vuetify.display.mobile"
    :temporary="$vuetify.display.mobile"
  >
    <div class="sidebar-brand">
      <NuxtLink to="/merchant" class="d-flex align-center">
        <img
          v-if="customizer.miniSidebar"
          :src="logoSm"
          width="28"
          height="32"
          alt="Sewain"
        />
        <img
          v-else
          :src="logo"
          width="195"
          height="32"
          alt="Sewain"
        />
      </NuxtLink>
    </div>

    <v-divider />

    <v-list nav class="sidebar-menu">
      <template v-for="section in menuSections" :key="section.title">
        <v-list-subheader v-if="!customizer.miniSidebar">
          {{ section.title }}
        </v-list-subheader>

        <v-list-item
          v-for="item in section.items"
          :key="item.to"
          :to="item.to"
          :active="isActive(item.to)"
          :title="item.title"
          rounded="lg"
          class="sidebar-menu-item"
        >
          <template #prepend>
            <component :is="item.icon" class="sidebar-menu-icon" />
          </template>
        </v-list-item>
      </template>
    </v-list>
  </v-navigation-drawer>
</template>

<style scoped>
.leftSidebar {
  border-right: 1px solid rgba(0, 0, 0, 0.08);
}

.sidebar-brand {
  display: flex;
  align-items: center;
  min-height: 64px;
  padding: 0 20px;
}

.sidebar-menu {
  padding: 12px;
}

.sidebar-menu-item {
  margin-bottom: 4px;
}

.sidebar-menu-icon {
  width: 20px;
  height: 20px;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

}

.sidebar-menu :deep(.v-list-subheader) {
  min-height: 40px;
  padding-inline: 12px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  opacity: 0.65;
}

.sidebar-menu :deep(.v-list-item--active) {
  color: rgb(var(--v-theme-primary));
  background: rgba(var(--v-theme-primary), 0.08);
}

.sidebar-menu :deep(.v-list-item__prepend) {
  margin-inline-end: 14px;
  align-items: center;
}

.sidebar-menu :deep(.v-list-item__prepend > .v-icon),
.sidebar-menu :deep(.v-list-item__prepend > svg) {
  margin-inline: 0;
}
</style>
