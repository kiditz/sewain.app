import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { stripTypeScriptTypes } from 'node:module';
const source = file => stripTypeScriptTypes(readFileSync(new URL(`../app/features/storefront-builder/utils/${file}`, import.meta.url), 'utf8'));
const moduleUrl = source => `data:text/javascript;base64,${Buffer.from(source).toString('base64')}`;
const {getLayoutStyle,getLayoutWidthStyle,getPaddingStyle}=await import(moduleUrl(source('block-settings.ts')));
const dependencies=moduleUrl(source('product-information-layout.ts'));
const {contentSectionSettings}=await import(moduleUrl(source('content-section-layout.ts').replace("'./product-information-layout'",JSON.stringify(dependencies))));
const theme={layout:{contentMaxWidth:1200,sectionSpacing:64}};

test('full clears inherited caps; page is centered and cannot exceed its parent',()=>{
 const full=getLayoutWidthStyle({width:'full'},theme);
 const page=getLayoutWidthStyle({width:'page'},theme);
 assert.equal(full.width,'100%');
 assert.equal(full.maxWidth,'100%');
 assert.equal(full.marginInline,0);
 assert.equal(page.width,'100%');
 assert.equal(page.maxWidth,'min(100%, 1200px)');
 assert.equal(page.marginInline,'auto');
 assert.equal(page.boxSizing,'border-box');
 assert.equal(full.boxSizing,'border-box');
});

test('partial layouts preserve width and padding stays inside that width',()=>{
 const style={...getLayoutStyle({width:'full'},theme),...getPaddingStyle({left:48,right:48},theme)};
 assert.equal(style.width,'100%');
 assert.equal(style.maxWidth,'100%');
 assert.equal(style.height,'auto');
 assert.equal(style.flexDirection,'column');
 assert.equal(style.paddingLeft,'48px');
 assert.equal(style.boxSizing,'border-box');
 assert.equal(getLayoutStyle({width:'page',direction:'horizontal'},theme).maxWidth,'min(100%, 1200px)');
});

test('legacy content sections gain editable width without losing alignment or zero padding',()=>{
 const settings=contentSectionSettings({alignment:'right',layout:{width:'full'},padding:{top:0,left:0}},'left',72);
 assert.equal(settings.layout.width,'full');
 assert.equal(settings.layout.alignment,'right');
 assert.equal(settings.padding.top,0);
 assert.equal(settings.padding.left,0);
 assert.equal(settings.padding.bottom,72);
 assert.equal(settings.padding.right,24);
 assert.equal(contentSectionSettings({},'center').layout.width,'page');
});
