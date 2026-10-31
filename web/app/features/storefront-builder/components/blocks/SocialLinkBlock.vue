<script setup lang="ts">
import { InstagramOutlined, FacebookOutlined, TwitterOutlined, YoutubeOutlined, LinkedinOutlined, WhatsAppOutlined, SendOutlined, GithubOutlined, GlobalOutlined } from "@ant-design/icons-vue";
import type { Component } from "vue";
import type { StorefrontBlock, StorefrontPadding } from "~/types/storefront";
import { useStorefront } from "../../composables/useStorefront";
import { getPaddingStyle } from "../../utils/block-settings";
import { socialPlatforms, socialLinkHref } from "../../utils/social-links";
const props = defineProps<{ block: StorefrontBlock; preview?: boolean }>();
const { theme } = useStorefront();
const icons: Record<string, Component> = { instagram: InstagramOutlined, facebook: FacebookOutlined, twitter: TwitterOutlined, youtube: YoutubeOutlined, linkedin: LinkedinOutlined, whatsapp: WhatsAppOutlined, telegram: SendOutlined, github: GithubOutlined, website: GlobalOutlined };
const network = computed(() => String(props.block.settings.network || "instagram"));
const icon = computed(() => icons[network.value] || GlobalOutlined);
const label = computed(() => String(props.block.settings.label || socialPlatforms.find(platform => platform.value === network.value)?.title || "Social link"));
const href = computed(() => socialLinkHref(props.block.settings.url));
const style = computed(() => ({
  ...getPaddingStyle(props.block.settings.padding as StorefrontPadding, theme.value),
  fontSize: `${Math.min(64, Math.max(12, Number(props.block.settings.size) || 24))}px`,
}));
</script>
<template>
  <a v-if="href || preview" :href="href || undefined" class="social-link" :style="style" :aria-label="label" :title="label" :target="block.settings.openInNewTab !== false ? '_blank' : undefined" :rel="block.settings.openInNewTab !== false ? 'noopener noreferrer' : undefined" @click="preview && $event.preventDefault()"><component :is="icon" aria-hidden="true" /></a>
</template>
<style scoped>
.social-link { display: inline-flex; align-items: center; justify-content: center; box-sizing: border-box; max-width: 100%; color: inherit; text-decoration: none; border-radius: 6px; }
.social-link:hover { opacity: .75; }
.social-link:focus-visible { outline: 2px solid currentColor; outline-offset: 2px; }
</style>
