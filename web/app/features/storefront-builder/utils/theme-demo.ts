import type {Product} from '~/types/product';
import type {CollectionSummary} from '~/types/collection';
import type {ThemeDocument} from '~/types/theme-library';
import type {PageKind,StorefrontPage} from '~/types/storefront';
import {initialTheme} from './storefront-defaults';
import {createThemeDocument} from './theme-library';
import {createSection} from '../composables/useStorefrontPages';
import type {ThemeDemoCatalog} from './catalog-demo';
export {themeDemoCatalogKey} from './catalog-demo';
export function createThemeDemoCatalog(locale:string):ThemeDemoCatalog {
 const id=locale.startsWith('id');
 const products:Product[]=['camera','chair','tent','lamp','camera','chair'].map((asset,index)=>({
  id:`demo-${index}`,name:(id?['Kamera klasik','Kursi santai','Tenda petualang','Lampu meja']:['Classic camera','Lounge chair','Adventure tent','Table lamp'])[index%4]!,slug:asset,media:`/themes/demo/${asset}.svg`,description:id?'Pilihan berkualitas untuk menemani rencanamu.':'A considered essential for your next experience.',status:'ACTIVE',createdAt:'',updatedAt:'',variants:[{id:`variant-${index}`,name:id?'Standar':'Standard',sku:`DEMO-${index}`,mediaList:[`/themes/demo/${asset}.svg`],buyPrice:0,depositAmount:0,active:true,attributeValueIds:[],values:[],inventoryUnits:[],createdAt:'',updatedAt:'',rentalPlans:[{id:`plan-${index}`,durationUnit:'DAY',durationValue:1,rentalFee:75000+index*15000,active:true,createdAt:'',updatedAt:''}]}],
 }));
 const collections:CollectionSummary[]=['everyday','trail','coast','terra','bloom','atelier'].map((asset,index)=>({id:`collection-${index}`,name:(id?['Keseharian','Petualangan','Liburan','Rumah','Perayaan','Kreativitas']:['Everyday','Adventure','Getaways','At home','Celebrations','Creative'])[index]!,slug:asset,imageUrl:`/themes/demo/${asset}.svg`,parentId:null,description:null,type:'MANUAL',matchCondition:'ALL',productCount:6}));
 return {products,collections};
}
export function createThemeDemoDocument(preset:string,locale:string):ThemeDocument {
 const pages:StorefrontPage[]=(['HOME','PRODUCT','COLLECTIONS','PRODUCT_GRID','CART','CHECKOUT'] as PageKind[]).map(kind=>({id:kind,title:kind,slug:kind.toLowerCase(),kind,status:'PUBLISHED',targetUrl:'/',icon:'HomeOutlined',updatedAt:'',sections:kind==='HOME'?[createSection('NAVIGATION'),createSection('HERO'),createSection('FOOTER')]:[]}));
 const document=createThemeDocument({theme:structuredClone(initialTheme),pages},preset,locale);
 for(const page of document.pages)for(const section of page.sections)if(section.type==='NAVIGATION')for(const block of section.blocks)if(block.type==='LOGO'){block.type='HEADING';block.settings={text:'Sewain',style:{color:'',backgroundColor:'#00000000'},padding:{top:0,right:0,bottom:0,left:0},layout:{width:'match',maxWidth:'normal'}};}
 return document;
}
