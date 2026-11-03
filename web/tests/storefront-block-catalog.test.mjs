import test from 'node:test';
import assert from 'node:assert/strict';
import {build} from 'esbuild';
import {createRequire} from 'node:module';
import {readFileSync} from 'node:fs';
import {fileURLToPath} from 'node:url';
const root=fileURLToPath(new URL('../',import.meta.url));
const require=createRequire(new URL('../package.json',import.meta.url));
const result=await build({stdin:{contents:`export {blockRegistry,createDefaultBlock} from './app/features/storefront-builder/utils/block-registry';export {blockComponentRegistry} from './app/features/storefront-builder/utils/block-component-registry';export {blockCategories} from './app/features/storefront-builder/utils/block-catalog';export {canEmbedPage,copyrightBlockText} from './app/features/storefront-builder/utils/block-text';`,resolveDir:root,loader:'ts'},bundle:true,platform:'node',format:'cjs',write:false,external:['vue'],alias:{'~':root+'app'},plugins:[{name:'vue-stubs',setup(build){build.onLoad({filter:/\.vue$/},()=>({contents:'export default {}',loader:'js'}));}}]});
const compiled={exports:{}};new Function('require','module','exports',result.outputFiles[0].text)(require,compiled,compiled.exports);
const {blockRegistry,createDefaultBlock,blockComponentRegistry,blockCategories,canEmbedPage,copyrightBlockText}=compiled.exports;

test('all blocks are categorized once, rendered, translated, and accepted by the backend schema',()=>{
 const types=blockCategories.flatMap(group=>group.types);
 assert.equal(blockCategories.length,8);
 assert.equal(types.length,new Set(types).size);
 assert.deepEqual(new Set(types),new Set(Object.keys(blockRegistry)));
 const backend=readFileSync(new URL('../../backend/src/main/java/app/sewain/storefront/ThemeLibraryService.java',import.meta.url),'utf8');
 const backendTypes=[...backend.match(/BLOCK_TYPES = Set\.of\(([^;]+)\)/)[1].matchAll(/"([A-Z_]+)"/g)].map(match=>match[1]);
 assert.deepEqual(new Set(backendTypes),new Set(types));
 const messages=['en','id'].map(locale=>JSON.parse(readFileSync(new URL(`../i18n/locales/${locale}/${locale}.json`,import.meta.url),'utf8')));
 for(const type of types){assert.ok(blockComponentRegistry[type],type);assert.ok(backend.includes(`"${type}"`));for(const locale of messages)assert.ok(locale.blockCatalog.labels[type],type);}
});

test('nested defaults are independent and only use allowed children',()=>{
 function visit(block,ids){assert.ok(!ids.has(block.id));ids.add(block.id);for(const child of block.blocks||[]){assert.ok(blockRegistry[block.type].allowedChildren?.includes(child.type),`${block.type}: ${child.type}`);visit(child,ids);}}
 for(const type of Object.keys(blockRegistry))visit(createDefaultBlock(type),new Set());
 const before=createDefaultBlock('COMPARISON_SLIDER'),after=createDefaultBlock('COMPARISON_SLIDER');
 assert.equal(before.blocks.length,2);before.blocks[0].settings.src='/one.png';assert.equal(after.blocks[0].settings.src,'');
 assert.equal(createDefaultBlock('COLLECTION_CARD').blocks[0].settings.aspectRatio,'landscape');
 assert.equal(createDefaultBlock('CONTACT_FORM').settings.formKind,'CONTACT');
 assert.equal(createDefaultBlock('EMAIL_SIGNUP').settings.requireConsent,true);
});

test('embedded pages stop cycles and copyright escapes dynamic store names',()=>{
 assert.equal(canEmbedPage('about',[]),true);
 assert.equal(canEmbedPage('about',['about']),false);
 assert.equal(canEmbedPage('about',['one','two','three','four','five']),false);
 assert.equal(canEmbedPage('new-page',['about']),true);
 assert.equal(copyrightBlockText('© {year} {store}','<Store & Co>',2030),'© 2030 &lt;Store &amp; Co&gt;');
});
