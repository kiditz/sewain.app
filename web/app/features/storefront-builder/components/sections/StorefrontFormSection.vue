<script setup lang="ts">
import type {StorefrontBlock} from '~/types/storefront';
import BlockRenderer from '~/components/storefront/BlockRenderer.vue';
import {useStorefront} from '../../composables/useStorefront';
import {contentSectionSettings} from '../../utils/content-section-layout';
import {getLayoutWidthStyle,getPaddingStyle} from '../../utils/block-settings';
const props=defineProps<{settings:Record<string,unknown>;blocks:StorefrontBlock[];preview?:boolean;selectedBlockId?:string|null}>();
const {theme}=useStorefront();const {t,te}=useI18n();const api=useApi();const snackbar=useSnackbarStore();
const normalized=computed(()=>contentSectionSettings(props.settings));
const style=computed(()=>({...getLayoutWidthStyle(normalized.value.layout,theme.value),...getPaddingStyle(normalized.value.padding,theme.value,true)}));
const signup=computed(()=>props.settings.formKind==='EMAIL_SIGNUP');
const name=ref(''),email=ref(''),message=ref(''),website=ref(''),consent=ref(false),busy=ref(false);
let requestId:string|undefined;
watch([name,email,message,consent],()=>{requestId=undefined;});
async function submit(){
 if(props.preview || busy.value)return;
 if(!email.value.trim() || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value.trim()) || (!signup.value && (!name.value.trim() || !message.value.trim()))){snackbar.openSnackbar({message:t('storefrontForms.required'),type:'error'});return;}
 if((signup.value || props.settings.requireConsent)&&!consent.value){snackbar.openSnackbar({message:t('storefrontForms.consentRequired'),type:'error'});return;}
 busy.value=true;
 try{
   requestId??=crypto.randomUUID();
   await api('/storefront/submissions',{method:'POST',body:{requestId,kind:signup.value?'EMAIL_SIGNUP':'CONTACT',name:name.value.trim(),email:email.value.trim(),message:message.value.trim(),consent:consent.value,website:website.value}});
   snackbar.openSnackbar({message:String(props.settings.successMessage || t(signup.value?'storefrontForms.subscribed':'storefrontForms.received')),type:'success'});
   name.value='';email.value='';message.value='';consent.value=false;requestId=undefined;
 }catch(error){const code=(error as {data?:{message?:string}}).data?.message;snackbar.openSnackbar({message:code&&te(code)?t(code):t('storefrontForms.failed'),type:'error'});}
 finally{busy.value=false;}
}
</script>
<template><section :style="style"><BlockRenderer :blocks="blocks" :preview="preview" :selected-block-id="selectedBlockId" /><p v-if="preview" class="form-preview-hint">{{ t('storefrontForms.preview') }}</p><v-form class="storefront-form" @submit.prevent="submit"><fieldset :disabled="preview || busy"><v-text-field v-if="!signup" v-model="name" :label="t('storefrontForms.name')" autocomplete="name" maxlength="120" required variant="outlined" /><v-text-field v-model="email" :label="t('storefrontForms.email')" type="email" autocomplete="email" maxlength="254" required variant="outlined" /><v-textarea v-if="!signup" v-model="message" :label="t('storefrontForms.message')" maxlength="5000" rows="5" required variant="outlined" /><input v-model="website" name="website" tabindex="-1" autocomplete="off" aria-hidden="true" class="storefront-form__honey" /><v-checkbox v-if="signup || settings.requireConsent" v-model="consent" :label="t('storefrontForms.consent')" hide-details class="mb-4" /><v-btn type="submit" color="primary" :disabled="preview" :loading="busy">{{ settings.buttonLabel || t(signup?'storefrontForms.subscribe':'storefrontForms.send') }}</v-btn></fieldset></v-form></section></template>
<style scoped>.storefront-form{margin-top:24px;max-width:760px;margin-inline:auto}.storefront-form fieldset{border:0;min-width:0}.storefront-form__honey{position:absolute;left:-10000px;width:1px;height:1px}.form-preview-hint{margin-top:16px;opacity:.7;font-size:13px}</style>
