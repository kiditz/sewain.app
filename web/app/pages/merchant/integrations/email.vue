<script setup lang="ts">
import type { EmailIntegration, EmailMessagePage, IntegrationEmailMessage } from "~/composables/useEmailIntegration";
definePageMeta({ layout: "merchant", middleware: ["merchant"] });
const { t } = useI18n();
const mail = useEmailIntegration();
const { formatDate: regionalDate } = useSettings();
const { data: saved, error: loadError, refresh } = await useAsyncData("merchant-email-integration", mail.get);
const config = reactive({
  outboundEnabled: false, inboundEnabled: false, fromAddress: "", fromName: "",
  smtpHost: "", smtpPort: 587, smtpSecurity: "STARTTLS" as "SSL" | "STARTTLS", smtpUsername: "",
  imapHost: "", imapPort: 993, imapSecurity: "SSL" as "SSL" | "STARTTLS", imapUsername: "", imapFolder: "INBOX",
});
const enabled = ref(false);
const smtpPassword = ref("");
const imapPassword = ref("");
function apply(value: EmailIntegration | null | undefined) {
  if (!value) return;
  Object.assign(config, value.configuration);
  enabled.value = value.enabled;
  smtpPassword.value = ""; imapPassword.value = "";
}
watch(saved, apply, { immediate: true });
const tab = ref("settings");
const busy = ref("");
const notice = ref("");
const failure = ref("");
const noticeType = ref<"success" | "warning">("success");
const dirty = computed(() => !!smtpPassword.value || !!imapPassword.value || enabled.value !== saved.value?.enabled || Object.keys(config).some(key => config[key as keyof typeof config] !== saved.value?.configuration[key as keyof typeof config]));
const outboundReady = computed(() => !!saved.value?.enabled && saved.value.configuration.outboundEnabled);
const inboundReady = computed(() => !!saved.value?.enabled && saved.value.configuration.inboundEnabled);
const direction = computed(() => tab.value === "inbox" ? "INBOUND" : "OUTBOUND");
const page = ref(1);
const listing = ref<EmailMessagePage>({ items: [], total: 0, page: 0, pages: 0 });
const selected = ref<{ message: IntegrationEmailMessage; body: string } | null>(null);
const reading = ref(false);
const composing = ref(false);
const draft = reactive({ to: "", subject: "", body: "" });
let requestId: string | null = null;
const deliveryUncertain = ref(false);

function errorMessage(error: unknown) {
  const data = (error as { data?: { message?: string; errors?: Record<string, string> } })?.data;
  const key = data?.message;
  if (data?.errors && Object.keys(data.errors).length) return t("integrations.email.invalidConfiguration");
  return key && key.startsWith("integrations.") ? t(key) : t("integrations.operationFailed");
}
async function run(key: string, action: () => Promise<void>) {
  if (busy.value) return;
  busy.value = key; failure.value = ""; notice.value = ""; noticeType.value = "success";
  try { await action(); } catch (error) { failure.value = errorMessage(error); }
  finally { busy.value = ""; }
}
async function save() {
  await run("save", async () => {
    saved.value = await mail.save({ enabled: enabled.value, configuration: { ...config }, smtpPassword: smtpPassword.value, imapPassword: imapPassword.value });
    apply(saved.value); notice.value = t("integrations.saved");
    await refreshNuxtData("merchant-integrations");
  });
}
async function check(channel: "smtp" | "imap") {
  await run(channel, async () => { await mail.check(channel); notice.value = t("integrations.connectionOk"); });
}
async function loadMessages() {
  listing.value = await mail.messages(direction.value, page.value - 1);
}
watch(tab, () => { page.value = 1; if (tab.value !== "settings") void run("messages", loadMessages); });
watch(page, () => { if (tab.value !== "settings") void run("messages", loadMessages); });
async function sync() {
  await run("sync", async () => {
    const result = await mail.sync();
    notice.value = t("integrations.synced", { imported: result.imported, skipped: result.skipped });
    await loadMessages();
  });
}
async function openMessage(item: IntegrationEmailMessage) {
  await run("detail", async () => { selected.value = await mail.detail(item.id); reading.value = true; });
}
function compose() {
  Object.assign(draft, { to: "", subject: "", body: "" });
  requestId = null; deliveryUncertain.value = false; composing.value = true;
}
async function send() {
  if (deliveryUncertain.value) return;
  await run("send", async () => {
    requestId ??= crypto.randomUUID();
    const result = await mail.send({ ...draft, requestId });
    if (result.status !== "SENT") {
      deliveryUncertain.value = true; noticeType.value = "warning"; notice.value = t("integrations.deliveryUnknown");
    } else {
      composing.value = false; requestId = null; notice.value = t("integrations.sent");
    }
    await loadMessages();
  });
}
const confirmDisconnect = ref(false);
async function disconnect() {
  await run("disconnect", async () => {
    await mail.disconnect(); await refresh(); confirmDisconnect.value = false;
    await refreshNuxtData("merchant-integrations"); notice.value = t("integrations.disconnected");
  });
}
function formatDate(value: string) { return regionalDate(value, true); }
</script>

<template>
  <v-container fluid class="pa-6 email-page">
    <v-btn variant="text" prepend-icon="mdi-arrow-left" to="/merchant/integrations" class="mb-4">{{ t("integrations.title") }}</v-btn>
    <div class="d-flex align-center justify-space-between flex-wrap ga-3 mb-2">
      <h1 class="text-headline-small">{{ t("integrations.emailTitle") }}</h1>
      <v-chip :color="saved?.enabled ? 'success' : undefined" variant="tonal">{{ t(saved?.enabled ? 'integrations.active' : 'integrations.inactive') }}</v-chip>
    </div>
    <p class="text-body-medium text-medium-emphasis mb-6">{{ t("integrations.emailDescription") }}</p>
    <v-alert v-if="loadError" type="error" variant="tonal" class="mb-4">
      {{ t("integrations.loadFailed") }} <v-btn variant="text" @click="refresh()">{{ t("integrations.retry") }}</v-btn>
    </v-alert>
    <v-alert v-if="failure" type="error" variant="tonal" closable class="mb-4" @click:close="failure = ''">{{ failure }}</v-alert>
    <v-alert v-if="notice" :type="noticeType" variant="tonal" closable class="mb-4" @click:close="notice = ''">{{ notice }}</v-alert>
    <v-tabs v-model="tab" :disabled="!!busy" color="primary" class="mb-6">
      <v-tab value="settings">{{ t("integrations.settings") }}</v-tab>
      <v-tab value="inbox">{{ t("integrations.inbox") }}</v-tab>
      <v-tab value="outbox">{{ t("integrations.outbox") }}</v-tab>
    </v-tabs>
    <v-form v-if="tab === 'settings'" :disabled="!!busy || !!loadError" @submit.prevent="save">
      <v-card variant="outlined" rounded="lg" class="pa-5 mb-5">
        <v-switch v-model="enabled" :label="t('integrations.enableEmail')" color="primary" hide-details />
        <p class="text-body-small text-medium-emphasis mt-2">{{ t("integrations.credentialsHint") }}</p>
      </v-card>
      <v-row>
        <v-col cols="12" lg="6">
          <v-card variant="outlined" rounded="lg" class="pa-5 h-100">
            <h2 class="text-title-large mb-2">{{ t("integrations.outbound") }} · SMTP</h2>
            <v-switch v-model="config.outboundEnabled" :label="t('integrations.enableOutbound')" color="primary" hide-details class="mb-4" />
            <v-text-field v-model="config.fromName" :label="t('integrations.fromName')" maxlength="100" variant="outlined" />
            <v-text-field v-model="config.fromAddress" type="email" :label="t('integrations.fromAddress')" maxlength="254" variant="outlined" />
            <v-text-field v-model="config.smtpHost" :label="t('integrations.host')" placeholder="smtp.example.com" maxlength="253" variant="outlined" />
            <v-row><v-col cols="5"><v-text-field v-model.number="config.smtpPort" type="number" min="1" max="65535" :label="t('integrations.port')" variant="outlined" /></v-col><v-col cols="7"><v-select v-model="config.smtpSecurity" :items="['STARTTLS', 'SSL']" :label="t('integrations.security')" variant="outlined" /></v-col></v-row>
            <v-text-field v-model="config.smtpUsername" :label="t('integrations.username')" autocomplete="off" maxlength="254" variant="outlined" />
            <v-text-field v-model="smtpPassword" type="password" :label="t('integrations.password')" :placeholder="saved?.smtpPasswordSet ? t('integrations.passwordSaved') : ''" autocomplete="new-password" variant="outlined" :hint="t('integrations.passwordHint')" persistent-hint />
            <v-btn variant="tonal" class="mt-4" :disabled="!outboundReady || dirty || !!busy" :loading="busy === 'smtp'" @click="check('smtp')">{{ t("integrations.checkSmtp") }}</v-btn>
          </v-card>
        </v-col>
        <v-col cols="12" lg="6">
          <v-card variant="outlined" rounded="lg" class="pa-5 h-100">
            <h2 class="text-title-large mb-2">{{ t("integrations.inbound") }} · IMAP</h2>
            <v-switch v-model="config.inboundEnabled" :label="t('integrations.enableInbound')" color="primary" hide-details class="mb-4" />
            <v-text-field v-model="config.imapHost" :label="t('integrations.host')" placeholder="imap.example.com" maxlength="253" variant="outlined" />
            <v-row><v-col cols="5"><v-text-field v-model.number="config.imapPort" type="number" min="1" max="65535" :label="t('integrations.port')" variant="outlined" /></v-col><v-col cols="7"><v-select v-model="config.imapSecurity" :items="['SSL', 'STARTTLS']" :label="t('integrations.security')" variant="outlined" /></v-col></v-row>
            <v-text-field v-model="config.imapUsername" :label="t('integrations.username')" autocomplete="off" maxlength="254" variant="outlined" />
            <v-text-field v-model="imapPassword" type="password" :label="t('integrations.password')" :placeholder="saved?.imapPasswordSet ? t('integrations.passwordSaved') : ''" autocomplete="new-password" variant="outlined" :hint="t('integrations.passwordHint')" persistent-hint />
            <v-text-field v-model="config.imapFolder" :label="t('integrations.folder')" maxlength="255" variant="outlined" class="mt-4" />
            <p class="text-body-small text-medium-emphasis mb-4">{{ t("integrations.syncHint") }}</p>
            <v-btn variant="tonal" :disabled="!inboundReady || dirty || !!busy" :loading="busy === 'imap'" @click="check('imap')">{{ t("integrations.checkImap") }}</v-btn>
          </v-card>
        </v-col>
      </v-row>
      <p v-if="dirty" class="text-body-small text-medium-emphasis mt-4">{{ t("integrations.saveFirst") }}</p>
      <div class="d-flex justify-space-between ga-3 mt-5">
        <v-btn v-if="saved?.installed" variant="text" color="error" :disabled="!!busy" @click="confirmDisconnect = true">{{ t("integrations.disconnect") }}</v-btn>
        <v-spacer /><v-btn type="submit" color="primary" :loading="busy === 'save'" :disabled="!!busy || !!loadError">{{ t("integrations.save") }}</v-btn>
      </div>
    </v-form>
    <v-card v-else variant="outlined" rounded="lg">
      <div class="d-flex align-center justify-space-between flex-wrap ga-3 pa-4">
        <h2 class="text-title-medium">{{ t(tab === 'inbox' ? 'integrations.inbox' : 'integrations.outbox') }} ({{ listing.total }})</h2>
        <div class="d-flex ga-2">
          <v-btn variant="text" :disabled="!!busy" @click="run('messages', loadMessages)">{{ t("integrations.refresh") }}</v-btn>
          <v-btn v-if="tab === 'inbox'" color="primary" :disabled="!inboundReady || !!busy" :loading="busy === 'sync'" @click="sync">{{ t("integrations.sync") }}</v-btn>
          <v-btn v-else color="primary" :disabled="!outboundReady || !!busy" @click="compose">{{ t("integrations.compose") }}</v-btn>
        </div>
      </div>
      <v-progress-linear v-if="busy" indeterminate />
      <v-table><thead><tr><th>{{ t(tab === 'inbox' ? 'integrations.fromAddress' : 'integrations.to') }}</th><th>{{ t('integrations.subject') }}</th><th>{{ t('integrations.status') }}</th><th>{{ t('integrations.date') }}</th></tr></thead>
        <tbody><tr v-for="item in listing.items" :key="item.id">
          <td>{{ tab === 'inbox' ? item.sender : item.recipient }}</td>
          <td><button class="email-subject text-primary" type="button" :disabled="!!busy" @click="openMessage(item)">{{ item.subject || t('integrations.noSubject') }}</button></td>
          <td><v-chip size="small" :color="item.status === 'UNKNOWN' || item.status === 'SENDING' ? 'warning' : 'success'" variant="tonal">{{ t(`integrations.statuses.${item.status}`) }}</v-chip></td>
          <td class="text-no-wrap">{{ formatDate(item.occurredAt) }}</td>
        </tr><tr v-if="!listing.items.length && !busy"><td colspan="4" class="text-center pa-8 text-medium-emphasis">{{ t('integrations.empty') }}</td></tr></tbody>
      </v-table>
      <v-pagination v-if="listing.pages > 1" v-model="page" :length="listing.pages" :disabled="!!busy" class="my-3" />
    </v-card>
    <v-dialog v-model="reading" max-width="760"><v-card v-if="selected" class="pa-6" rounded="lg">
      <h2 class="text-title-large mb-3">{{ selected.message.subject || t('integrations.noSubject') }}</h2>
      <p class="text-body-small">{{ selected.message.sender }} → {{ selected.message.recipient }}</p>
      <p class="text-body-small text-medium-emphasis mb-4">{{ formatDate(selected.message.occurredAt) }}</p>
      <v-alert v-if="selected.message.status === 'UNKNOWN' || selected.message.status === 'SENDING'" type="warning" variant="tonal" class="mb-4">{{ t('integrations.deliveryUnknown') }}</v-alert>
      <div class="email-body">{{ selected.body || t('integrations.emptyBody') }}</div>
      <v-card-actions><v-spacer /><v-btn @click="reading = false">{{ t('integrations.close') }}</v-btn></v-card-actions>
    </v-card></v-dialog>
    <v-dialog v-model="composing" max-width="720" :persistent="busy === 'send'"><v-card class="pa-6" rounded="lg">
      <h2 class="text-title-large mb-5">{{ t('integrations.compose') }}</h2>
      <v-form :disabled="!!busy || deliveryUncertain" @submit.prevent="send">
        <v-text-field v-model="draft.to" type="email" :label="t('integrations.to')" maxlength="254" required variant="outlined" />
        <v-text-field v-model="draft.subject" :label="t('integrations.subject')" maxlength="255" required variant="outlined" />
        <v-textarea v-model="draft.body" :label="t('integrations.body')" maxlength="100000" required rows="8" variant="outlined" />
        <v-alert v-if="deliveryUncertain" type="warning" variant="tonal" class="mb-4">{{ t('integrations.deliveryUnknown') }}</v-alert>
        <v-alert v-if="failure" type="error" variant="tonal" class="mb-4">{{ failure }}</v-alert>
        <v-card-actions><v-spacer /><v-btn :disabled="!!busy" @click="composing = false">{{ t('integrations.close') }}</v-btn>
          <v-btn type="submit" color="primary" variant="flat" :loading="busy === 'send'" :disabled="!!busy || deliveryUncertain || !draft.to.trim() || !draft.subject.trim() || !draft.body.trim()">{{ t('integrations.send') }}</v-btn>
        </v-card-actions>
      </v-form>
    </v-card></v-dialog>
    <v-dialog v-model="confirmDisconnect" max-width="480"><v-card class="pa-6" rounded="lg">
      <h2 class="text-title-large mb-3">{{ t('integrations.disconnect') }}</h2><p>{{ t('integrations.disconnectHint') }}</p>
      <v-card-actions><v-spacer /><v-btn :disabled="!!busy" @click="confirmDisconnect = false">{{ t('integrations.close') }}</v-btn><v-btn color="error" :loading="busy === 'disconnect'" @click="disconnect">{{ t('integrations.disconnect') }}</v-btn></v-card-actions>
    </v-card></v-dialog>
  </v-container>
</template>

<style scoped>
.email-page { max-width: 1440px; }
.email-body { white-space: pre-wrap; overflow-wrap: anywhere; font-size: 14px; line-height: 1.7; }
.email-subject { text-align: start; overflow-wrap: anywhere; padding: 12px 0; }
</style>
