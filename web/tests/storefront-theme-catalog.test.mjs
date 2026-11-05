import test from 'node:test';
import assert from 'node:assert/strict';
import {build} from 'esbuild';
import {createRequire} from 'node:module';
import {readFileSync} from 'node:fs';
import {fileURLToPath} from 'node:url';
const root=fileURLToPath(new URL('../',import.meta.url));
const require=createRequire(new URL('../package.json',import.meta.url));
const result=await build({stdin:{contents:`export * from './app/features/storefront-builder/utils/theme-library';export * from './app/features/storefront-builder/utils/storefront-theme';export {initialTheme} from './app/features/storefront-builder/utils/storefront-defaults';export {createSection} from './app/features/storefront-builder/composables/useStorefrontPages';`,resolveDir:root,loader:'ts'},bundle:true,define:{'import.meta.client':'false'},platform:'node',format:'cjs',write:false,external:['vue'],alias:{'~':root+'app'},plugins:[{name:'vue-stubs',setup(build){build.onLoad({filter:/\.vue$/},()=>({contents:'export default {}',loader:'js'}));}}]});
const compiled={exports:{}};
new Function('require','module','exports',result.outputFiles[0].text)(require,compiled,compiled.exports);
const {themeCatalog,storefrontPresets,initialTheme,createThemeDocument,createSection,setStorefrontThemeMode,presetColors}=compiled.exports;
const base={theme:structuredClone(initialTheme),pages:[{id:'home',kind:'HOME',sections:['NAVIGATION','HERO','FOOTER'].map(createSection)},{id:'about',kind:'CUSTOM',sections:[createSection('RICH_TEXT')]}]};
test('ten unique themes have matching palettes and translated descriptions',()=>{
 assert.equal(themeCatalog.length,10);assert.equal(new Set(themeCatalog.map(theme=>theme.id)).size,10);
 assert.deepEqual(themeCatalog.map(theme=>theme.name),storefrontPresets);
 for(const locale of ['en','id']){
  const messages=JSON.parse(readFileSync(new URL(`../i18n/locales/${locale}/${locale}.json`,import.meta.url),'utf8'));
  for(const preset of themeCatalog){assert.ok(messages.themeLibrary.catalog[preset.id]);assert.deepEqual(preset.colors,presetColors(preset.id,preset.mode));}
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
  const sections=first.pages[0].sections.slice(1,-1);
  compositions.add(sections.map(section=>section.type).join(','));
  assert.notEqual(sections[0].id,second.pages[0].sections[1].id);
  sections[0].settings.test='changed';assert.equal(second.pages[0].sections[1].settings.test,undefined);
  setStorefrontThemeMode(first.theme,preset.mode==='dark'?'light':'dark');
  assert.deepEqual(first.theme.colors,presetColors(preset.id,first.theme.mode));
 }
 assert.equal(compositions.size,10);assert.equal(JSON.stringify(base),before);
});
