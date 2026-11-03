<script setup lang="ts">
import ConfirmDeleteDialog from '~/components/ConfirmDeleteDialog.vue';
import type {PageResponse} from '~/types/pagination';
definePageMeta({layout:'merchant',middleware:['merchant']});
interface Submission{id:string;kind:string;name:string;email:string;message:string;consent:boolean;createdAt:string}
const api=useApi();const {t}=useI18n();const snackbar=useSnackbarStore();
const items=ref<Submission[]>([]),total=ref(0),page=ref(1),loading=ref(false),deleting=ref<Submission|null>(null);
let sequence=0;
async function load(){const current=++sequence;loading.value=true;try{const result=await api<PageResponse<Submission>>('/storefront/submissions',{query:{page:page.value-1,size:20}});if(current===sequence){items.value=result.items;total.value=result.totalItems;}}catch{snackbar.openSnackbar({message:t('storefrontForms.loadFailed'),type:'error'});}finally{if(current===sequence)loading.value=false;}}
async function remove(){if(!deleting.value)return;const id=deleting.value.id;deleting.value=null;try{await api(`/storefront/submissions/${id}`,{method:'DELETE'});await load();}catch{snackbar.openSnackbar({message:t('storefrontForms.failed'),type:'error'});}}
watch(page,load);await load();
const headers=computed(()=>[{title:t('storefrontForms.kind'),key:'kind'},{title:t('storefrontForms.name'),key:'name'},{title:t('storefrontForms.email'),key:'email'},{title:t('storefrontForms.message'),key:'message'},{title:t('storefrontForms.date'),key:'createdAt'},{title:'',key:'actions',sortable:false}]);
</script>
<template><v-container class="py-8"><div class="d-flex align-center ga-3 mb-6"><v-btn to="/merchant/storefront" icon="mdi-arrow-left" variant="text" :aria-label="t('common.back')" /><h1>{{ t('storefrontForms.inbox') }}</h1></div><v-data-table-server :headers="headers" :items="items" :items-length="total" :items-per-page="20" :page="page" :loading="loading" hide-default-footer><template #item.kind="{item}">{{ t(item.kind==='CONTACT'?'storefrontForms.contact':'storefrontForms.signup') }}</template><template #item.message="{item}"><p class="submission-message">{{ item.message }}</p></template><template #item.actions="{item}"><v-btn icon="mdi-delete-outline" variant="text" :aria-label="t('common.remove')" @click="deleting=item" /></template></v-data-table-server><v-pagination v-model="page" :length="Math.max(1,Math.ceil(total/20))" class="mt-4" /><ConfirmDeleteDialog :model-value="!!deleting" :title="t('storefrontForms.deleteTitle')" :message="t('storefrontForms.deleteHint')" @update:model-value="!$event&&(deleting=null)" @confirmed="remove" /></v-container></template>
<style scoped>.submission-message{white-space:pre-line;min-width:200px;max-width:600px;max-height:180px;overflow:auto;padding:8px 0}</style>
