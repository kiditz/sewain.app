import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {fileURLToPath} from 'node:url';
import {createRequire} from 'node:module';
import {build} from 'esbuild';
import {parse,compileScript} from '@vue/compiler-sfc';
import {createSSRApp,h} from 'vue';
import {renderToString} from 'vue/server-renderer';
const root=fileURLToPath(new URL('../',import.meta.url)),require=createRequire(new URL('../package.json',import.meta.url));
async function compile(contents){const result=await build({stdin:{contents,loader:'ts',resolveDir:root},bundle:true,platform:'node',format:'cjs',write:false,external:['vue','sanitize-html'],alias:{'~':root+'app'}});const module={exports:{}};new Function('require','module','exports',result.outputFiles[0].text)(require,module,module.exports);return module.exports;}
const {sanitizeRichText,safeNavigationUrl}=await compile(`export * from './app/utils/content-security';`);
test('rich text sanitizer removes active markup and keeps supported Quill formatting',()=>{
 for(const input of ['<p>OK</p><img src=x onerror="x()">','<svg><g onload="x()"></g></svg>','<p style="background:url(javascript:x)">OK</p>','<p onclick="x()"><a href="jav&#x61;script:x">Link</a></p>','<math><mtext><table><mglyph><style><!--</style><img title="--><img src=x onerror=x()>">']){
  const output=sanitizeRichText(input);assert.doesNotMatch(output,/<(?:script|img|svg|math|iframe|style)\b|<[^>]+\son\w+\s*=|href="javascript:|\sstyle=/i);
 }
 const valid='<h2>Heading</h2><p class="ql-align-center"><strong>Bold</strong><em>Italic</em><u>Underline</u></p><ol><li data-list="ordered">Item</li></ol><blockquote>Quote</blockquote><pre class="ql-syntax">a&lt;b</pre>';
 const output=sanitizeRichText(valid);for(const fragment of ['<h2>Heading</h2>','ql-align-center','<strong>Bold</strong>','<em>Italic</em>','<u>Underline</u>','<ol>','data-list="ordered"','<blockquote>','ql-syntax','a&lt;b'])assert.ok(output.includes(fragment),fragment);
});
test('navigation policy rejects executable and obfuscated schemes without breaking internal links',()=>{
 for(const value of ['javascript:x','JaVaScRiPt:x','java\nscript:x','\u0000javascript:x','//evil.test','/\\evil.test','data:text/html,x','vbscript:x'])assert.equal(safeNavigationUrl(value),'');
 for(const value of ['/products/camera','#details','?page=2','https://shop.test/path'])assert.equal(safeNavigationUrl(value),value);
 assert.equal(safeNavigationUrl(null),'');
 assert.doesNotMatch(sanitizeRichText('<a href="//evil.test" target="_blank">Link</a>'),/href=|target=/);
});
test('actual BlockLabel SSR no longer returns attacker event attributes',async()=>{
 const filename='app/features/storefront-builder/components/blocks/BlockLabel.vue';const {descriptor}=parse(readFileSync(root+filename,'utf8'),{filename});const script=compileScript(descriptor,{id:'security-regression',inlineTemplate:true});const compiled=await compile(`import {computed} from 'vue';\n${script.content}`);
 const output=await renderToString(createSSRApp({render:()=>h(compiled.default,{value:'<p>Review</p><img src=x onerror="reviewMarker()">'})}));
 assert.ok(output.includes('<p>Review</p>'));assert.ok(!output.includes('onerror'));assert.ok(!output.includes('<img'));
});
