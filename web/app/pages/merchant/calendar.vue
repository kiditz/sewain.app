<script setup lang="ts">
import { computed, ref } from "vue";
import FullCalendar from "@fullcalendar/vue3";
import type {
  Calendar,
  CalendarOptions,
  EventClickArg,
} from "@fullcalendar/core";
import dayGridPlugin from "@fullcalendar/daygrid";
import timeGridPlugin from "@fullcalendar/timegrid";
import interactionPlugin from "@fullcalendar/interaction";
import idLocale from "@fullcalendar/core/locales/id";
import enGbLocale from "@fullcalendar/core/locales/en-gb";

definePageMeta({ layout: "merchant", middleware: ["merchant"] });

const { t, locale } = useI18n();
useHead({ title: () => t("menu.calendar") });
const api = useApi();
const { regional, loadRegionalSettings } = useSettings();

type CalendarEventType = "pickup" | "delivery" | "return" | "rental";
interface CalendarEntry {
  orderId: string;
  orderNumber: string;
  customerName: string;
  status: string;
  fulfillmentType: "PICKUP" | "DELIVERY";
  fulfillmentAt: string;
  returnAt: string;
  productName: string;
  variantName: string;
  quantity: number;
  inventoryUnitId: string;
  allocatedStartAt: string;
  allocatedEndAt: string;
  allocationStatus: "HELD" | "CONFIRMED" | "RELEASED" | "COMPLETED";
}
interface RentalEvent {
  id: string;
  title: string;
  start: string;
  end?: string;
  allDay: boolean;
  backgroundColor: string;
  borderColor: string;
  extendedProps: {
    orderId: string;
    orderNumber: string;
    customer: string;
    product: string;
    type: CalendarEventType;
    quantity: number;
    status: string;
    allocationStatus: string;
    at: string;
  };
}

const calendarRef = ref<{ getApi: () => Calendar } | null>(null);
const todayKey = () =>
  new Intl.DateTimeFormat("en-CA", {
    timeZone: regional.value.timezone,
  }).format(new Date());
const selectedDate = ref(todayKey());
const currentMonth = ref(selectedDate.value.slice(0, 7));
const currentView = ref("dayGridMonth");
const rentalEvents = ref<RentalEvent[]>([]);
const loading = ref(false);
const loadError = ref(false);
let requestId = 0;

const dateKey = (value: string) =>
  new Intl.DateTimeFormat("en-CA", {
    timeZone: regional.value.timezone,
  }).format(new Date(value));
const currentMonthTitle = computed(() => {
  const [year, month] = currentMonth.value.split("-").map(Number);
  return new Intl.DateTimeFormat(locale.value === "id" ? "id-ID" : "en-GB", {
    month: "long",
    year: "numeric",
  }).format(new Date(year ?? 1990, (month ?? 1) - 1, 1));
});
const selectedDateLabel = computed(() =>
  new Intl.DateTimeFormat(locale.value === "id" ? "id-ID" : "en-GB", {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric",
    timeZone: regional.value.timezone,
  }).format(new Date(`${selectedDate.value}T12:00:00`)),
);
const eventTime = (value: string) =>
  new Intl.DateTimeFormat(locale.value === "id" ? "id-ID" : "en-GB", {
    hour: "2-digit",
    minute: "2-digit",
    timeZone: regional.value.timezone,
  }).format(new Date(value));

const selectedEvents = computed(() =>
  rentalEvents.value.filter((event) => event.start === selectedDate.value),
);
const monthlyPickups = computed(
  () =>
    rentalEvents.value.filter(
      (event) =>
        event.start.startsWith(currentMonth.value) &&
        (event.extendedProps.type === "pickup" ||
          event.extendedProps.type === "delivery"),
    ).length,
);
const monthlyReturns = computed(
  () =>
    rentalEvents.value.filter(
      (event) =>
        event.start.startsWith(currentMonth.value) &&
        event.extendedProps.type === "return",
    ).length,
);

function toEvent(
  entry: CalendarEntry,
  type: CalendarEventType,
  at: string,
  suffix: string,
  end?: string,
): RentalEvent {
  const color =
    type === "return"
      ? "#E67E22"
      : type === "delivery"
        ? "#7B1FA2"
        : type === "rental"
          ? allocationColor(entry.allocationStatus)
          : "#1976D2";
  const product = [entry.productName, entry.variantName]
    .filter(Boolean)
    .join(" · ");
  return {
    id: `${entry.orderId}-${suffix}`,
    title: `${type === "rental" ? product : t(`calendar.${type}`)} · ${entry.orderNumber}`,
    start: type === "rental" ? dateKey(at) : at,
    end,
    allDay: type === "rental",
    backgroundColor: color,
    borderColor: color,
    extendedProps: {
      orderId: entry.orderId,
      orderNumber: entry.orderNumber,
      customer: entry.customerName,
      product,
      type,
      quantity: entry.quantity,
      status: entry.status,
      allocationStatus: entry.allocationStatus,
      at,
    },
  };
}

function allocationColor(status: CalendarEntry["allocationStatus"]) {
  return status === "HELD"
    ? "#F9A825"
    : status === "COMPLETED"
      ? "#607D8B"
      : "#3949AB";
}

function nextDateKey(key: string): string {
  const [year, month, day] = key.split("-").map(Number);

  if (!year || !month || !day) {
    throw new Error(`Invalid date key: ${key}`);
  }

  const date = new Date(Date.UTC(year, month - 1, day + 1));

  return [
    date.getUTCFullYear(),
    String(date.getUTCMonth() + 1).padStart(2, "0"),
    String(date.getUTCDate()).padStart(2, "0"),
  ].join("-");
}

function groupEntries(entries: CalendarEntry[]) {
  const allocations = new Map<string, CalendarEntry>();
  const fulfillments = new Map<string, CalendarEntry>();
  const returns = new Map<string, CalendarEntry>();
  const increment = (
    map: Map<string, CalendarEntry>,
    key: string,
    entry: CalendarEntry,
  ) => {
    const current = map.get(key);
    if (current) current.quantity += 1;
    else map.set(key, { ...entry, quantity: 1 });
  };
  for (const entry of entries) {
    increment(
      allocations,
      [
        entry.orderId,
        entry.productName,
        entry.variantName,
        entry.allocatedStartAt,
        entry.allocatedEndAt,
        entry.allocationStatus,
      ].join("|"),
      entry,
    );
    increment(
      fulfillments,
      [
        entry.orderId,
        entry.productName,
        entry.variantName,
        entry.fulfillmentAt,
      ].join("|"),
      entry,
    );
    increment(
      returns,
      [
        entry.orderId,
        entry.productName,
        entry.variantName,
        entry.returnAt,
      ].join("|"),
      entry,
    );
  }
  return {
    allocations: [...allocations.values()],
    fulfillments: [...fulfillments.values()],
    returns: [...returns.values()],
  };
}

async function loadCalendarRange(start: Date, end: Date) {
  const sequence = ++requestId;
  loading.value = true;
  loadError.value = false;
  // Pad the query range to include date events at the edge of the merchant timezone.
  const from = new Date(start);
  from.setDate(from.getDate() - 2);
  const to = new Date(end);
  to.setDate(to.getDate() + 2);
  try {
    const entries = await api<CalendarEntry[]>("merchant/orders/calendar", {
      query: { from: from.toISOString(), to: to.toISOString() },
    });
    if (sequence !== requestId) return;
    const grouped = groupEntries(entries);
    rentalEvents.value = [
      ...grouped.allocations.map((entry) =>
        toEvent(
          entry,
          "rental",
          entry.allocatedStartAt,
          `rental-${entry.productName}-${entry.variantName}-${entry.allocationStatus}`,
          nextDateKey(dateKey(entry.allocatedEndAt)),
        ),
      ),
      ...grouped.fulfillments.map((entry) =>
        toEvent(
          entry,
          entry.fulfillmentType === "DELIVERY" ? "delivery" : "pickup",
          entry.fulfillmentAt,
          `fulfillment-${entry.productName}-${entry.variantName}`,
        ),
      ),
      ...grouped.returns.map((entry) =>
        toEvent(
          entry,
          "return",
          entry.returnAt,
          `return-${entry.productName}-${entry.variantName}`,
        ),
      ),
    ];
  } catch {
    if (sequence === requestId) loadError.value = true;
  } finally {
    if (sequence === requestId) loading.value = false;
  }
}

const calendarOptions = computed<CalendarOptions>(() => ({
  plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin],
  initialView: "dayGridMonth",
  initialDate: selectedDate.value,
  locale: locale.value === "id" ? idLocale : enGbLocale,
  firstDay: regional.value.firstDayOfWeek === "monday" ? 1 : 0,
  headerToolbar: false,
  events: rentalEvents.value,
  height: "auto",
  fixedWeekCount: false,
  dayMaxEvents: 3,
  selectable: false,
  editable: false,
  eventDisplay: "block",
  dateClick(info) {
    selectedDate.value = info.dateStr.slice(0, 10);
  },
  eventClick(info: EventClickArg) {
    selectedDate.value = info.event.startStr.slice(0, 10);
  },
  datesSet(info) {
    const date = info.view.currentStart;
    currentMonth.value = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`;
    void loadCalendarRange(info.start, info.end);
  },
}));

function statusColor(status: string) {
  switch (status) {
    case "CONFIRMED":
      return "success";
    case "PROCESSING":
    case "READY":
      return "info";
    case "PENDING_PAYMENT":
      return "warning";
    case "ACTIVE_RENTAL":
      return "primary";
    case "COMPLETED":
      return "success";
    default:
      return "default";
  }
}

function goToToday() {
  calendarRef.value?.getApi().today();
  selectedDate.value = todayKey();
}

function changeView(view: string) {
  currentView.value = view;
  calendarRef.value?.getApi().changeView(view);
}

onMounted(async () => {
  await loadRegionalSettings();
  selectedDate.value = todayKey();
  calendarRef.value?.getApi().gotoDate(selectedDate.value);
});
</script>

<template>
  <v-container fluid class="pa-4 pa-md-6">
    <div
      class="d-flex flex-column flex-sm-row align-sm-center justify-space-between ga-3 mb-6"
    >
      <div>
        <h1 class="text-headline-small font-weight-bold">
          {{ t("calendar.title") }}
        </h1>
        <p class="text-body-medium text-medium-emphasis mt-1 mb-0">
          {{ t("calendar.subtitle") }}
        </p>
      </div>
      <v-btn
        color="primary"
        prepend-icon="mdi-plus"
        to="/merchant/orders/create"
        >{{ t("calendar.createOrder") }}</v-btn
      >
    </div>

    <v-alert v-if="loadError" type="error" variant="tonal" class="mb-4">{{
      t("calendar.loadError")
    }}</v-alert>
    <v-row class="mb-2">
      <v-col cols="12" sm="6">
        <v-card rounded="lg" variant="outlined"
          ><v-card-text class="d-flex align-center ga-4">
            <v-avatar color="blue-lighten-5" rounded="lg" size="48"
              ><v-icon icon="mdi-calendar-arrow-right" color="blue-darken-2"
            /></v-avatar>
            <div>
              <div class="text-body-medium text-medium-emphasis">
                {{ t("calendar.monthlyPickups") }}
              </div>
              <div class="text-headline-small font-weight-bold">
                {{ loading ? "—" : monthlyPickups }}
              </div>
            </div>
          </v-card-text></v-card
        >
      </v-col>
      <v-col cols="12" sm="6">
        <v-card rounded="lg" variant="outlined"
          ><v-card-text class="d-flex align-center ga-4">
            <v-avatar color="orange-lighten-5" rounded="lg" size="48"
              ><v-icon icon="mdi-calendar-arrow-left" color="orange-darken-2"
            /></v-avatar>
            <div>
              <div class="text-body-medium text-medium-emphasis">
                {{ t("calendar.monthlyReturns") }}
              </div>
              <div class="text-headline-small font-weight-bold">
                {{ loading ? "—" : monthlyReturns }}
              </div>
            </div>
          </v-card-text></v-card
        >
      </v-col>
    </v-row>

    <v-row>
      <v-col cols="12" xl="8">
        <v-card rounded="lg" variant="outlined"
          ><v-card-text class="pa-3 pa-md-5">
            <div
              class="calendar-toolbar d-flex flex-wrap align-center justify-space-between ga-3 mb-4"
            >
              <div class="d-flex align-center ga-2">
                <v-btn-toggle divided rounded="lg" variant="outlined">
                  <v-btn
                    icon="mdi-chevron-left"
                    :aria-label="t('calendar.previousMonth')"
                    @click="calendarRef?.getApi().prev()"
                  />
                  <v-btn
                    icon="mdi-chevron-right"
                    :aria-label="t('calendar.nextMonth')"
                    @click="calendarRef?.getApi().next()"
                  />
                </v-btn-toggle>
                <v-btn variant="outlined" @click="goToToday">{{
                  t("calendar.today")
                }}</v-btn>
              </div>
              <div
                class="text-title-large font-weight-bold text-center flex-grow-1"
              >
                {{ currentMonthTitle }}
              </div>
              <v-btn-toggle
                v-model="currentView"
                divided
                rounded="lg"
                mandatory
                variant="outlined"
              >
                <v-btn
                  value="dayGridMonth"
                  @click="changeView('dayGridMonth')"
                  >{{ t("calendar.month") }}</v-btn
                >
                <v-btn
                  value="timeGridWeek"
                  @click="changeView('timeGridWeek')"
                  >{{ t("calendar.week") }}</v-btn
                >
                <v-btn value="timeGridDay" @click="changeView('timeGridDay')">{{
                  t("calendar.day")
                }}</v-btn>
              </v-btn-toggle>
            </div>
            <div class="d-flex flex-wrap align-center ga-4 mb-4">
              <div class="d-flex align-center ga-2">
                <span class="calendar-legend pickup" /><span
                  class="text-body-medium"
                  >{{ t("calendar.pickup") }}</span
                >
              </div>
              <div class="d-flex align-center ga-2">
                <span class="calendar-legend delivery" /><span
                  class="text-body-medium"
                  >{{ t("calendar.delivery") }}</span
                >
              </div>
              <div class="d-flex align-center ga-2">
                <span class="calendar-legend rental" /><span
                  class="text-body-medium"
                  >{{ t("calendar.rentalPeriod") }}</span
                >
              </div>
              <div class="d-flex align-center ga-2">
                <span class="calendar-legend return" /><span
                  class="text-body-medium"
                  >{{ t("calendar.return") }}</span
                >
              </div>
              <v-progress-circular
                v-if="loading"
                indeterminate
                size="18"
                width="2"
              />
            </div>
            <ClientOnly>
              <FullCalendar ref="calendarRef" :options="calendarOptions" />
              <template #fallback
                ><v-skeleton-loader type="image" height="480"
              /></template>
            </ClientOnly> </v-card-text
        ></v-card>
      </v-col>

      <v-col cols="12" xl="4">
        <v-card rounded="lg" variant="outlined">
          <v-card-title class="pa-4"
            ><div class="text-title-medium font-weight-bold">
              {{ t("calendar.agenda") }}
            </div>
            <div
              class="text-body-medium text-medium-emphasis font-weight-regular"
            >
              {{ selectedDateLabel }}
            </div></v-card-title
          >
          <v-divider />
          <v-card-text class="pa-4">
            <div v-if="selectedEvents.length" class="d-flex flex-column ga-4">
              <div
                v-for="event in selectedEvents"
                :key="event.id"
                class="agenda-event"
                :class="event.extendedProps.type"
              >
                <div class="d-flex align-start justify-space-between ga-2">
                  <div>
                    <div class="text-subtitle-2 font-weight-bold">
                      {{ event.extendedProps.orderNumber }}
                    </div>
                    <div class="text-body-medium mt-1">
                      {{ event.extendedProps.customer }}
                    </div>
                  </div>
                  <v-chip
                    size="small"
                    variant="tonal"
                    :color="
                      event.extendedProps.type === 'pickup'
                        ? 'blue'
                        : event.extendedProps.type === 'delivery'
                          ? 'purple'
                          : event.extendedProps.type === 'rental'
                            ? 'indigo'
                            : 'orange'
                    "
                    >{{ t(`calendar.${event.extendedProps.type}`) }}</v-chip
                  >
                </div>
                <div class="text-body-medium text-medium-emphasis mt-3">
                  {{ event.extendedProps.product }}
                </div>
                <div
                  v-if="event.extendedProps.type !== 'rental'"
                  class="text-body-small text-medium-emphasis mt-2"
                >
                  {{ eventTime(event.extendedProps.at) }}
                </div>
                <div class="d-flex align-center justify-space-between mt-3">
                  <span class="text-body-medium">{{
                    t("calendar.unitCount", {
                      count: event.extendedProps.quantity,
                    })
                  }}</span
                  ><v-chip
                    size="x-small"
                    :color="
                      event.extendedProps.type === 'rental'
                        ? 'indigo'
                        : statusColor(event.extendedProps.status)
                    "
                    variant="tonal"
                    >{{
                      event.extendedProps.type === "rental"
                        ? t(
                            `calendar.allocationStatus.${event.extendedProps.allocationStatus}`,
                          )
                        : t(`orders.status.${event.extendedProps.status}`)
                    }}</v-chip
                  >
                </div>
                <v-btn
                  class="mt-3"
                  block
                  variant="outlined"
                  size="small"
                  :to="`/merchant/orders/${event.extendedProps.orderId}`"
                  >{{ t("calendar.viewOrder") }}</v-btn
                >
              </div>
            </div>
            <v-empty-state
              v-else
              icon="mdi-calendar-blank-outline"
              :title="t('calendar.noEvents')"
              :text="t('calendar.noEventsDescription')"
            />
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<style scoped>
.calendar-toolbar {
  min-height: 44px;
}
.calendar-legend {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 3px;
}
.calendar-legend.pickup {
  background: #1976d2;
}
.calendar-legend.delivery {
  background: #7b1fa2;
}
.calendar-legend.rental {
  background: #3949ab;
}
.calendar-legend.return {
  background: #e67e22;
}
.agenda-event {
  padding: 14px;
  border: 1px solid rgba(var(--v-border-color), var(--v-border-opacity));
  border-left: 3px solid;
  border-radius: 8px;
}
.agenda-event.pickup {
  border-left-color: #1976d2;
}
.agenda-event.delivery {
  border-left-color: #7b1fa2;
}
.agenda-event.rental {
  border-left-color: #3949ab;
}
.agenda-event.return {
  border-left-color: #e67e22;
}
:deep(.fc) {
  font-family: inherit;
  font-size: 0.875rem;
}
:deep(.fc .fc-toolbar) {
  flex-wrap: wrap;
  gap: 12px;
}
:deep(.fc .fc-toolbar-title) {
  font-size: 1.15rem;
}
:deep(.fc .fc-button) {
  font-size: 0.8rem;
  padding: 0.35rem 0.6rem;
}
:deep(.fc .fc-daygrid-event) {
  border-radius: 4px;
  padding: 2px 4px;
}
</style>
