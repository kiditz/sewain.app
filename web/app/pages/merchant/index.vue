<script setup lang="ts">
import {
  CalendarOutlined,
  ArrowRightOutlined,
  ClockCircleOutlined,
  ShoppingOutlined,
  WalletOutlined,
  InboxOutlined,
} from "@ant-design/icons-vue";
import type { MerchantOrder, OrderStatus } from "~/types/order";
import type { PageResponse } from "~/types/pagination";

definePageMeta({ layout: "merchant", middleware: ["merchant"] });

const { t, locale } = useI18n();
const { formatMoney, loadRegionalSettings } = useSettings();
const api = useApi();
useHead({ title: t("dashboard.title") });

interface InventoryUnitSummary {
  id: string;
  status: string;
}
interface MerchantOrderActivity {
  id: string;
  orderId: string;
  orderNumber: string;
  customerName: string;
  eventType: "ORDER_CREATED" | "STATUS_CHANGED" | "PAYMENT_RECORDED";
  previousStatus?: OrderStatus | null;
  currentStatus?: OrderStatus | null;
  paymentStage?: string | null;
  amount?: number | null;
  createdAt: string;
}
const { data, pending, error } = await useAsyncData(
  "merchant-dashboard",
  async () => {
    const [ordersResponse, inventoryResponse, activities] = await Promise.all([
      api<PageResponse<MerchantOrder>>("merchant/orders", {
        query: { page: 0, size: 100, sortBy: "createdAt", direction: "desc" },
      }),
      api<PageResponse<InventoryUnitSummary>>("inventory", {
        query: { page: 0, size: 100 },
      }),
      api<MerchantOrderActivity[]>("merchant/orders/activities"),
    ]);
    return {
      orders: ordersResponse.items,
      orderCount: ordersResponse.totalItems,
      inventory: inventoryResponse.items,
      activities,
    };
  },
);

onMounted(loadRegionalSettings);

const orders = computed(() => data.value?.orders ?? []);
const inventory = computed(() => data.value?.inventory ?? []);
const activeRentalOrders = computed(() =>
  orders.value.filter((order) => order.status === "ACTIVE_RENTAL"),
);
const needsProcessing = computed(() =>
  orders.value.filter((order) =>
    ["CONFIRMED", "PROCESSING"].includes(order.status),
  ),
);
const availableUnits = computed(
  () => inventory.value.filter((unit) => unit.status === "AVAILABLE").length,
);
const monthlyRevenue = computed(() => {
  const now = new Date();
  return orders.value
    .flatMap((order) => order.payments ?? [])
    .filter(
      (payment) =>
        payment.status === "SUCCEEDED" &&
        payment.paidAt &&
        new Date(payment.paidAt).getMonth() === now.getMonth() &&
        new Date(payment.paidAt).getFullYear() === now.getFullYear(),
    )
    .reduce((sum, payment) => sum + payment.receivedAmount, 0);
});

const stats = computed(() => [
  {
    title: "dashboard.rented",
    value: activeRentalOrders.value.length,
    description: "dashboard.activeOrders",
    icon: ShoppingOutlined,
    color: "primary",
  },
  {
    title: "dashboard.needsProcessing",
    value: needsProcessing.value.length,
    description: "dashboard.orders",
    icon: ClockCircleOutlined,
    color: "warning",
  },
  {
    title: "dashboard.readyToRent",
    value: availableUnits.value,
    description: "dashboard.availableUnits",
    icon: InboxOutlined,
    color: "success",
  },
]);

const actions = computed(() => [
  {
    title: "dashboard.pendingPayment",
    description: "dashboard.pendingPaymentDescription",
    count: orders.value.filter((order) => order.status === "PENDING_PAYMENT")
      .length,
    color: "warning",
    icon: WalletOutlined,
    to: "/merchant/orders?status=PENDING_PAYMENT",
  },
  {
    title: "dashboard.todayPickups",
    description: "dashboard.todayPickupsDescription",
    count: todayEvents.value.filter(
      (item) => item.type === "pickup" && item.order.status !== "ACTIVE_RENTAL",
    ).length,
    color: "primary",
    icon: CalendarOutlined,
    to: "/merchant/orders",
  },
  {
    title: "dashboard.overdueReturns",
    description: "dashboard.overdueReturnsDescription",
    count: overdueReturns.value.length,
    color: "error",
    icon: ClockCircleOutlined,
    to: "/merchant/orders?status=ACTIVE_RENTAL",
  },
]);

const startOfToday = () => {
  const date = new Date();
  date.setHours(0, 0, 0, 0);
  return date;
};
const startOfTomorrow = () => {
  const date = startOfToday();
  date.setDate(date.getDate() + 1);
  return date;
};
const events = computed(() =>
  orders.value.flatMap((order) => {
    if (["COMPLETED", "CANCELLED", "EXPIRED"].includes(order.status)) {
      return [];
    }

    return [
      {
        order,
        type:
          order.fulfillmentType === "PICKUP"
            ? ("pickup" as const)
            : ("delivery" as const),
        at:
          order.fulfillmentType === "PICKUP"
            ? order.pickupAt
            : order.deliveryAt,
      },
      {
        order,
        type: "return" as const,
        at: order.returnAt,
      },
    ]
      .filter((event) => event.at)
      .map((event) => ({
        ...event,
        at: new Date(event.at!),
      }));
  }),
);

const todayEvents = computed(() =>
  events.value
    .filter(
      (event) => event.at >= startOfToday() && event.at < startOfTomorrow(),
    )
    .sort((a, b) => a.at.getTime() - b.at.getTime()),
);
const overdueReturns = computed(() =>
  orders.value.filter(
    (order) =>
      order.status === "ACTIVE_RENTAL" &&
      !!order.returnAt &&
      new Date(order.returnAt) < new Date(),
  ),
);
const recentOrders = computed(() => orders.value.slice(0, 5));
const recentActivities = computed(() => data.value?.activities ?? []);
const dateLabel = computed(() =>
  new Intl.DateTimeFormat(locale.value, {
    day: "numeric",
    month: "short",
    year: "numeric",
  }).format(new Date()),
);

const timeLabel = (value: Date) =>
  new Intl.DateTimeFormat(locale.value, {
    hour: "2-digit",
    minute: "2-digit",
  }).format(value);

const activityDateTime = (value: string) =>
  new Intl.DateTimeFormat(locale.value, {
    day: "numeric",
    month: "short",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));

const statusColor = (status: OrderStatus) =>
  ({
    PENDING_PAYMENT: "warning",
    CONFIRMED: "success",
    PROCESSING: "primary",
    READY: "info",
    ACTIVE_RENTAL: "primary",
    COMPLETED: "success",
    CANCELLED: "error",
    EXPIRED: "default",
  })[status];
</script>

<template>
  <v-container fluid class="pa-4 pa-md-6 dashboard">
    <div
      class="d-flex flex-column flex-sm-row justify-space-between align-sm-center ga-3 mb-6"
    >
      <div>
        <h1 class="text-headline-small font-weight-bold mb-1">
          {{ t("dashboard.title") }}
        </h1>
        <p class="text-body-medium text-medium-emphasis mb-0">
          {{ t("dashboard.subtitle") }}
        </p>
      </div>
      <div
        class="d-flex align-center ga-2 text-body-medium text-medium-emphasis"
      >
        <CalendarOutlined /><span>{{ dateLabel }}</span>
      </div>
    </div>

    <v-alert v-if="error" type="error" variant="tonal" class="mb-4">{{
      t("dashboard.loadError")
    }}</v-alert>
    <v-row class="mb-2">
      <v-col v-for="stat in stats" :key="stat.title" cols="12" sm="6" lg="3">
        <v-card rounded="lg" border flat class="stat-card h-100">
          <v-card-text class="pa-5">
            <div class="d-flex align-start justify-space-between mb-4">
              <span class="text-body-medium text-medium-emphasis">{{
                t(stat.title)
              }}</span
              ><v-avatar
                :color="`${stat.color}-lighten-5`"
                rounded="lg"
                size="42"
              >
                <component
                  :is="stat.icon"
                  class="stat-icon"
                  :style="{ color: `rgb(var(--v-theme-${stat.color}))` }"
                />
              </v-avatar>
            </div>
            <div class="text-headline-medium font-weight-bold mb-1">
              {{ pending ? "—" : stat.value }}
            </div>
            <div class="text-body-small text-medium-emphasis">
              {{ t(stat.description) }}
            </div>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="12" sm="6" lg="3">
        <v-card rounded="lg" flat class="stat-card revenue-card h-100"
          ><v-card-text class="pa-5">
            <div class="d-flex align-start justify-space-between mb-4">
              <span class="text-body-medium text-medium-emphasis">{{
                t("dashboard.monthlyRevenue")
              }}</span
              ><v-avatar color="primary-lighten-5" rounded="lg" size="42">
                <WalletOutlined class="stat-icon text-primary" />
              </v-avatar>
            </div>
            <div class="text-headline-small font-weight-bold mb-1">
              {{ pending ? "—" : formatMoney(monthlyRevenue) }}
            </div>
            <div class="text-body-small text-medium-emphasis">
              {{ t("dashboard.receivedThisMonth") }}
            </div>
          </v-card-text></v-card
        >
      </v-col>
    </v-row>

    <v-row>
      <v-col cols="12" lg="5">
        <v-card rounded="lg" border flat class="h-100">
          <v-card-item
            ><template #title
              ><span class="text-title-medium font-weight-bold">{{
                t("dashboard.needsAction")
              }}</span></template
            ><template #append
              ><v-chip size="small" variant="tonal">{{
                t("dashboard.categories", { count: actions.length })
              }}</v-chip></template
            ></v-card-item
          >
          <v-divider />
          <v-list class="py-0" lines="two"
            ><v-list-item
              v-for="(action, index) in actions"
              :key="action.title"
              :to="action.to"
              :class="{ 'border-b': index !== actions.length - 1 }"
              class="action-item py-3"
            >
              <template #prepend
                ><v-avatar
                  :color="`${action.color}-lighten-5`"
                  rounded="lg"
                  size="44"
                >
                  <component
                    :is="action.icon"
                    class="action-icon"
                    :style="{
                      color: `rgb(var(--v-theme-${action.color}))`,
                    }"
                  /> </v-avatar
              ></template>
              <v-list-item-title class="text-body-medium font-weight-medium">{{
                t(action.title)
              }}</v-list-item-title
              ><v-list-item-subtitle class="text-body-small mt-1 text-wrap">{{
                t(action.description, { count: action.count })
              }}</v-list-item-subtitle>
              <template #append>
                <div class="d-flex align-center ga-2">
                  <v-badge
                    :content="action.count"
                    :color="action.color"
                    inline
                  />
                  <ArrowRightOutlined class="text-medium-emphasis" />
                </div>
              </template> </v-list-item
          ></v-list>
        </v-card>
      </v-col>

      <v-col cols="12" lg="7">
        <v-card rounded="lg" border flat class="h-100"
          ><v-card-item
            ><template #title
              ><span class="text-title-medium font-weight-bold">{{
                t("dashboard.todaySchedule")
              }}</span></template
            ><template #append>
              <NuxtLink
                to="/merchant/orders"
                class="text-primary text-body-medium font-weight-medium text-decoration-none"
                >{{ t("dashboard.viewAll") }}
                <ArrowRightOutlined class="ml-1" />
              </NuxtLink> </template></v-card-item
          ><v-divider />
          <v-card-text class="pa-4">
            <div v-if="todayEvents.length" class="d-flex flex-column ga-3">
              <div
                v-for="event in todayEvents"
                :key="`${event.order.id}-${event.type}`"
                class="d-flex align-center ga-3"
              >
                <v-avatar
                  :color="
                    event.type === 'pickup'
                      ? 'primary'
                      : event.type === 'delivery'
                        ? 'info'
                        : 'warning'
                  "
                  variant="tonal"
                  rounded="lg"
                >
                  <v-icon>
                    {{
                      event.type === "pickup"
                        ? "mdi-package-variant-closed"
                        : event.type === "delivery"
                          ? "mdi-truck-delivery-outline"
                          : "mdi-package-check"
                    }}
                  </v-icon>
                </v-avatar>

                <div class="flex-grow-1 min-width-0">
                  <div class="d-flex align-center ga-2 flex-wrap">
                    <span class="font-weight-medium">
                      {{
                        event.type === "pickup"
                          ? "Pickup"
                          : event.type === "delivery"
                            ? "Delivery"
                            : "Return"
                      }}
                    </span>

                    <v-chip
                      size="small"
                      :color="
                        event.type === 'pickup'
                          ? 'primary'
                          : event.type === 'delivery'
                            ? 'info'
                            : 'warning'
                      "
                      variant="tonal"
                    >
                      {{ event.order.orderNumber }}
                    </v-chip>
                  </div>

                  <div class="text-medium-emphasis">
                    {{ event.order.customerName }}
                  </div>
                </div>

                <div class="text-right flex-shrink-0">
                  <div class="font-weight-medium">
                    {{
                      event.at.toLocaleTimeString("id-ID", {
                        hour: "2-digit",
                        minute: "2-digit",
                      })
                    }}
                  </div>

                  <div class="text-medium-emphasis">
                    {{ t(`orders.status.${event.order.status}`) }}
                  </div>
                </div>
              </div>
            </div>

            <v-empty-state
              v-else
              icon="mdi-calendar-blank-outline"
              :title="$t('ui.no_events_today')"
              text="There are no pickup, delivery, or return schedules for today."
            />
            <v-alert
              v-if="!pending && !todayEvents.length"
              type="info"
              variant="tonal"
              density="compact"
              >{{ t("dashboard.noSchedule") }}</v-alert
            >
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12"
        ><v-card rounded="lg" border flat
          ><v-card-item
            ><template #title
              ><span class="text-title-medium font-weight-bold">{{
                t("dashboard.recentOrders")
              }}</span></template
            ><template #append>
              <NuxtLink
                to="/merchant/orders"
                class="text-primary text-body-medium font-weight-medium text-decoration-none"
                >{{ t("dashboard.viewAll") }}
                <ArrowRightOutlined class="ml-1" />
              </NuxtLink> </template></v-card-item
          ><v-divider />
          <v-table>
            <thead>
              <tr>
                <th>{{ t("dashboard.order") }}</th>
                <th>{{ t("dashboard.productCustomer") }}</th>
                <th>{{ t("dashboard.total") }}</th>
                <th>{{ t("dashboard.status") }}</th>
                <th />
              </tr>
            </thead>
            <tbody>
              <tr v-for="order in recentOrders" :key="order.id">
                <td class="font-weight-medium">{{ order.orderNumber }}</td>
                <td>
                  <div class="font-weight-medium">
                    {{
                      order.items
                        ?.map(
                          (item) => `${item.productName} ${item.variantName}`,
                        )
                        .join(", ") || "—"
                    }}
                  </div>
                  <div class="text-body-small text-medium-emphasis">
                    {{ order.customerName }}
                  </div>
                </td>
                <td>{{ formatMoney(order.totalAmount) }}</td>
                <td>
                  <v-chip
                    :color="statusColor(order.status)"
                    variant="tonal"
                    size="small"
                    >{{ t(`orders.status.${order.status}`) }}</v-chip
                  >
                </td>
                <td class="text-right">
                  <v-btn
                    :to="`/merchant/orders/${order.id}`"
                    variant="text"
                    color="primary"
                    size="small"
                    icon
                    :aria-label="t('dashboard.viewOrderDetails')"
                  >
                    <ArrowRightOutlined />
                  </v-btn>
                </td>
              </tr>
              <tr v-if="!pending && !recentOrders.length">
                <td colspan="5" class="text-center text-medium-emphasis py-6">
                  {{ t("orders.noOrders") }}
                </td>
              </tr>
            </tbody>
          </v-table>
          <v-card-text
            v-if="data"
            class="text-body-small text-medium-emphasis"
            >{{
              t("dashboard.orderCount", { count: data.orderCount })
            }}</v-card-text
          >
        </v-card></v-col
      >

      <v-col cols="12"
        ><v-card rounded="lg" border flat
          ><v-card-item
            ><template #title
              ><span class="text-title-medium font-weight-bold">{{
                t("dashboard.recentActivity")
              }}</span></template
            ></v-card-item
          ><v-divider />
          <v-list v-if="recentActivities.length" lines="two" class="py-0">
            <v-list-item
              v-for="(activity, index) in recentActivities"
              :key="activity.id"
              :to="`/merchant/orders/${activity.orderId}`"
              :class="{ 'border-b': index !== recentActivities.length - 1 }"
            >
              <template #prepend
                ><v-avatar
                  :color="
                    activity.eventType === 'PAYMENT_RECORDED'
                      ? 'success-lighten-5'
                      : 'primary-lighten-5'
                  "
                  rounded="lg"
                >
                  <WalletOutlined
                    v-if="activity.eventType === 'PAYMENT_RECORDED'"
                    class="text-success"
                  />
                  <ClockCircleOutlined v-else class="text-primary" /> </v-avatar
              ></template>
              <v-list-item-title class="text-body-medium font-weight-medium">{{
                t(`dashboard.activityTypes.${activity.eventType}`, {
                  orderNumber: activity.orderNumber,
                  customerName: activity.customerName,
                })
              }}</v-list-item-title>
              <v-list-item-subtitle class="text-body-small mt-1">
                <template v-if="activity.eventType === 'STATUS_CHANGED'"
                  >{{ t(`orders.status.${activity.previousStatus}`) }} →
                  {{ t(`orders.status.${activity.currentStatus}`) }}</template
                >
                <template v-else-if="activity.eventType === 'PAYMENT_RECORDED'"
                  >{{
                    t(`orders.detail.paymentStages.${activity.paymentStage}`)
                  }}
                  · {{ formatMoney(activity.amount ?? 0) }}</template
                >
                <template v-else>{{
                  activityDateTime(activity.createdAt)
                }}</template>
              </v-list-item-subtitle>
              <template #append
                ><span class="text-caption text-medium-emphasis">{{
                  activityDateTime(activity.createdAt)
                }}</span></template
              >
            </v-list-item>
          </v-list>
          <v-card-text
            v-else-if="!pending"
            class="text-body-medium text-medium-emphasis"
            >{{ t("dashboard.noActivity") }}</v-card-text
          >
        </v-card></v-col
      >
    </v-row>
  </v-container>
</template>

<style scoped>
.dashboard {
  max-width: 1800px;
}

.stat-card {
  border-color: rgba(var(--v-border-color), var(--v-border-opacity)) !important;
}

.revenue-card {
  border: 1px solid rgba(var(--v-theme-primary), 0.25) !important;
}

.stat-icon,
.action-icon {
  font-size: 20px;
}

.action-item {
  min-height: 100px;
}

.schedule-item {
  position: relative;
}

.schedule-time {
  width: 52px;
  flex-shrink: 0;
}

.schedule-indicator {
  width: 3px;
  border-radius: 2px;
  flex-shrink: 0;
}
</style>
