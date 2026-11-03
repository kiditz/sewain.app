import test from 'node:test';
import assert from 'node:assert/strict';
import {build} from 'esbuild';
import {createRequire} from 'node:module';
import {readFileSync} from 'node:fs';
import {fileURLToPath} from 'node:url';
const root=fileURLToPath(new URL('../',import.meta.url));
const require=createRequire(new URL('../package.json',import.meta.url));
const result=await build({stdin:{contents:`export {sectionRegistry} from './app/features/storefront-builder/utils/section-registry'; export {blockRegistry} from './app/features/storefront-builder/utils/block-registry'; export {sectionCategories} from './app/features/storefront-builder/utils/section-catalog'; export {createSection} from './app/features/storefront-builder/composables/useStorefrontPages';`,resolveDir:root,loader:'ts'},bundle:true,define:{'import.meta.client':'false'},platform:'node',format:'cjs',write:false,external:['vue'],alias:{'~':root+'app'},plugins:[{name:'vue-stubs',setup(build){build.onLoad({filter:/\.vue$/},()=>({contents:'export default {}',loader:'js'}));}}]});
const compiled={exports:{}};
new Function('require','module','exports',result.outputFiles[0].text)(require,compiled,compiled.exports);
const {sectionRegistry,blockRegistry,sectionCategories,createSection}=compiled.exports;

test('every section is categorized once and all presets create serializable editable blocks',()=>{
 const all=sectionCategories.flatMap(group=>group.types);
 assert.equal(sectionCategories.length,8);
 assert.equal(new Set(all).size,all.length);
 assert.deepEqual(new Set(all),new Set(Object.keys(sectionRegistry)));
 for(const type of all){
  const definition=sectionRegistry[type],section=createSection(type);
  assert.equal(section.type,type);
  assert.ok(section.settings.layout,`${type}: missing layout`);
  assert.ok(section.settings.padding,`${type}: missing padding`);
  assert.doesNotThrow(()=>JSON.stringify(section));
  const ids=new Set([section.id]);
  const visit=blocks=>{for(const block of blocks){assert.ok(blockRegistry[block.type]);assert.ok(!ids.has(block.id));ids.add(block.id);if(block.blocks)visit(block.blocks);}};
  visit(section.blocks);
  for(const block of section.blocks)assert.ok(definition.allowedBlocks.includes(block.type),`${type}: unsupported default ${block.type}`);
 }
});

test('presets have distinct configurations and independent defaults',()=>{
 assert.equal(createSection('HERO_BOTTOM').settings.layout.position,'bottom');
 assert.equal(createSection('COLLECTION_BENTO').settings.displayType,'bento');
 assert.equal(createSection('SLIDESHOW_INSET').settings.variant,'inset');
 assert.equal(createSection('RECOMMENDED_PRODUCTS').settings.recommendation,true);
 assert.equal(createSection('IMAGE_COMPARE').blocks.filter(block=>block.type==='IMAGE').length,2);
 const first=createSection('FAQ'),second=createSection('FAQ');
 first.blocks.find(block=>block.type==='FAQ_ITEM').settings.answer='Changed';
 assert.notEqual(second.blocks.find(block=>block.type==='FAQ_ITEM').settings.answer,'Changed');
 assert.equal(createSection('EMAIL_SIGNUP').settings.requireConsent,true);
});

test('backend schema and both locales cover all catalog types',()=>{
 const backend=readFileSync(new URL('../../backend/src/main/java/app/sewain/storefront/ThemeLibraryService.java',import.meta.url),'utf8');
 for(const type of [...Object.keys(sectionRegistry),...Object.keys(blockRegistry)])assert.ok(backend.includes(`"${type}"`),`Missing backend type ${type}`);
 for(const locale of ['en','id']){
  const messages=JSON.parse(readFileSync(new URL(`../i18n/locales/${locale}/${locale}.json`,import.meta.url),'utf8'));
  for(const type of Object.keys(sectionRegistry))assert.ok(messages.sectionCatalog.sections[type],`${locale}: ${type}`);
  for(const category of sectionCategories)assert.ok(messages.sectionCatalog.categories[category.id]);
 }
});
