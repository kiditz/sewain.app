import test from 'node:test';
import assert from 'node:assert/strict';
import {build} from 'esbuild';
import {createRequire} from 'node:module';
import {readFileSync} from 'node:fs';
import {fileURLToPath} from 'node:url';
const root=fileURLToPath(new URL('../',import.meta.url));
const require=createRequire(new URL('../package.json',import.meta.url));
const result=await build({stdin:{contents:`export * from './app/features/storefront-builder/utils/theme-library';export * from './app/features/storefront-builder/utils/storefront-theme';export {initialTheme} from './app/features/storefront-builder/utils/storefront-defaults';export {createSection} from './app/features/storefront-builder/composables/useStorefrontPages';export {createThemeDemoDocument} from './app/features/storefront-builder/utils/theme-demo';`,resolveDir:root,loader:'ts'},bundle:true,define:{'import.meta.client':'false'},platform:'node',format:'cjs',write:false,external:['vue'],alias:{'~':root+'app'},plugins:[{name:'vue-stubs',setup(build){build.onLoad({filter:/\.vue$/},()=>({contents:'export default {}',loader:'js'}));}}]});
const compiled={exports:{}};
new Function('require','module','exports',result.outputFiles[0].text)(require,compiled,compiled.exports);
const {themeCatalog,storefrontPresets,initialTheme,createThemeDocument,createSection,setStorefrontThemeMode,presetColors,createThemeDemoDocument}=compiled.exports;
const base={theme:structuredClone(initialTheme),pages:[{id:'home',kind:'HOME',sections:['NAVIGATION','HERO','FOOTER'].map(createSection)},{id:'about',kind:'CUSTOM',sections:[createSection('RICH_TEXT')]},...['PRODUCT','COLLECTIONS','PRODUCT_GRID','CART','CHECKOUT'].map(kind=>({id:kind,kind,sections:[createSection('RICH_TEXT')]}))]};
test('ten unique themes have matching palettes and translated descriptions',()=>{
 assert.equal(themeCatalog.length,10);assert.equal(new Set(themeCatalog.map(theme=>theme.id)).size,10);
 assert.deepEqual(themeCatalog.map(theme=>theme.name),storefrontPresets);
 for(const locale of ['en','id']){
  const messages=JSON.parse(readFileSync(new URL(`../i18n/locales/${locale}/${locale}.json`,import.meta.url),'utf8'));
  for(const preset of themeCatalog){assert.ok(messages.themeLibrary.catalog[preset.id]);assert.deepEqual(preset.colors,presetColors(preset.id,preset.mode));
   const image=readFileSync(new URL(`../public/themes/previews/${preset.id}.png`,import.meta.url));assert.equal(image.readUInt32BE(16),768);assert.equal(image.readUInt32BE(20),576);
  }
 }
 assert.equal(new Set(themeCatalog.map(preset=>JSON.stringify(preset.colors))).size,10);
});
test('theme installation creates independent compositions while preserving navigation and custom pages',()=>{
 const before=JSON.stringify(base),compositions=new Set();
 for(const preset of themeCatalog){
  const first=createThemeDocument(base,preset.name),second=createThemeDocument(base,preset.name);
  assert.deepEqual(first.pages[1],base.pages[1]);
  assert.deepEqual(first.pages[0].sections[0],base.pages[0].sections[0]);
  assert.deepEqual(first.pages[0].sections.at(-1),base.pages[0].sections.at(-1));
  assert.equal(first.theme.mode,preset.mode);
  assert.deepEqual(first.theme.colors,preset.colors);
  for(const page of first.pages.filter(page=>!['HOME','CUSTOM'].includes(page.kind))){
   assert.equal(page.sections[0].type,'NAVIGATION');assert.equal(page.sections.at(-1).type,'FOOTER');
   assert.ok(page.sections.some(section=>section.type==={PRODUCT:'PRODUCT_INFORMATION',COLLECTIONS:preset.id==='atelier'?'COLLECTION_EDITORIAL':['trail','coast','bloom','midnight','terra'].includes(preset.id)?'COLLECTION_BENTO':'COLLECTION_LIST',PRODUCT_GRID:'PRODUCT_GRID',CART:'CART',CHECKOUT:'CHECKOUT'}[page.kind]));
  }
  const indonesian=createThemeDemoDocument(preset.id,'id');
  const english=createThemeDemoDocument(preset.id,'en');
  const allBlocks=blocks=>blocks.flatMap(block=>[block,...allBlocks(block.blocks??[])]);
  const texts=indonesian.pages.flatMap(page=>page.sections.flatMap(section=>allBlocks(section.blocks))).map(block=>block.settings.text).filter(value=>typeof value==='string');
  for(const [document,expected] of [[indonesian,['Barang sewaan','Subtotal sewa','Lanjut ke checkout','Jumlah','Sewa Sekarang']],[english,['Rental items','Rental subtotal','Proceed to checkout','Quantity','Rent now']]]){
   const cart=document.pages.find(page=>page.kind==='CART').sections.flatMap(section=>allBlocks(section.blocks));
   const product=document.pages.find(page=>page.kind==='PRODUCT').sections.flatMap(section=>allBlocks(section.blocks));
   assert.equal(cart.find(block=>block.type==='CART_ITEMS').settings.title,expected[0]);
   assert.equal(cart.find(block=>block.type==='CART_SUMMARY').settings.rentalLabel,expected[1]);
   assert.equal(cart.find(block=>block.type==='CART_CHECKOUT').settings.label,expected[2]);
   assert.equal(product.find(block=>block.type==='QUANTITY_SELECTOR').settings.label,expected[3]);
   assert.equal(product.find(block=>block.type==='ADD_TO_CART').settings.label,expected[4]);
   assert.equal(document.pages.find(page=>page.kind==='PRODUCT_GRID').targetUrl,'/collections/all');
   assert.equal(document.pages.find(page=>page.kind==='COLLECTIONS').targetUrl,'/collections');
  }
  assert.ok(!texts.includes('Sample product'));
  assert.ok(!texts.includes('Tell your story and help customers discover your store.'));
  for(const section of first.pages[0].sections.filter(section=>!['NAVIGATION','FOOTER'].includes(section.type))){
   const images=allBlocks(section.blocks).filter(block=>block.type==='IMAGE'&&block.settings.src).map(block=>block.settings.src);
   if(section.settings.image?.src)images.push(section.settings.image.src);
   for(const image of images)assert.doesNotThrow(()=>readFileSync(new URL(`../public${image}`,import.meta.url)));
  }
  const sections=first.pages[0].sections.slice(1,-1);
  compositions.add(sections.map(section=>section.type).join(','));
  assert.notEqual(sections[0].id,second.pages[0].sections[1].id);
  sections[0].settings.test='changed';assert.equal(second.pages[0].sections[1].settings.test,undefined);
  setStorefrontThemeMode(first.theme,preset.mode==='dark'?'light':'dark');
  assert.deepEqual(first.theme.colors,presetColors(preset.id,first.theme.mode));
 }
 const minimal=structuredClone(base);minimal.pages=minimal.pages.slice(0,2);minimal.pages[1].slug='cart';
 const completed=createThemeDocument(minimal,'Everyday','id');
 assert.equal(completed.pages.length,7);assert.deepEqual(completed.pages[1],minimal.pages[1]);
 assert.equal(completed.pages.find(page=>page.kind==='CART').slug,'cart-template-1');
 assert.equal(new Set(completed.pages.map(page=>page.id)).size,completed.pages.length);
 assert.equal(compositions.size,10);assert.equal(JSON.stringify(base),before);
});
