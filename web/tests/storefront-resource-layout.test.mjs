import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {stripTypeScriptTypes} from 'node:module';
const source=stripTypeScriptTypes(readFileSync(new URL('../app/features/storefront-builder/utils/resource-list-layout.ts',import.meta.url),'utf8'));
const {bentoPlacements,editorialPlacement,resourceListItemStyles,resourceCardLayoutForBlock}=await import(`data:text/javascript;base64,${Buffer.from(source).toString('base64')}`);
function assertNoOverlap(placements){
 const occupied=new Set();
 for(const {column,span,row,rows} of placements){
  assert.ok(column>=1&&column+span<=13);
  for(let y=row;y<row+rows;y++)for(let x=column;x<column+span;x++){
   const key=`${x}:${y}`;assert.ok(!occupied.has(key),`overlap at ${key}`);occupied.add(key);
  }
 }
}
test('Bento fills rows without overlap or a lone final group for supported counts',()=>{
 for(let count=1;count<=100;count++){
  const placements=bentoPlacements(count);assert.equal(placements.length,count);
  assertNoOverlap(placements.map(item=>item.desktop));
  for(const [key,width] of [['desktop',12],['mobile',2]]){
   const rows=new Map();
   for(const item of placements)rows.set(item[key].row,(rows.get(item[key].row)||0)+item[key].span);
   for(const total of rows.values())assert.equal(total,width,`${count} ${key}`);
  }
 }
 assert.deepEqual(bentoPlacements(13).slice(-2).map(item=>item.desktop.span),[6,6]);
 assert.deepEqual(bentoPlacements(Infinity),[]);assert.deepEqual(bentoPlacements(-1),[]);
});
test('Editorial preserves asymmetric space across repeated desktop and mobile compositions',()=>{
 for(const kind of ['collections','products']){
  const placements=Array.from({length:100},(_,i)=>editorialPlacement(i,kind));assertNoOverlap(placements.map(item=>item.desktop));
  assert.equal(placements[8].desktop.row-placements[0].desktop.row,kind==='collections'?44:40);
  assert.ok(placements.some(item=>item.mobile.width===100));
  assert.ok(placements.some(item=>item.mobile.align==='flex-end'));
 }
 assert.notDeepEqual(editorialPlacement(0,'collections'),editorialPlacement(0,'products'));
 assert.deepEqual(resourceListItemStyles('grid',2,'products'),[{},{}]);
 assert.deepEqual(resourceListItemStyles('carousel',2,'products'),[{},{}]);
 assert.match(resourceListItemStyles('editorial',1,'collections')[0]['--resource-desktop-column'],/span 4/);
});
test('Card geometry ownership follows the nearest list and leaves ordinary blocks editable',()=>{
 const blocks=[{id:'header',type:'HEADER',settings:{}},{id:'card',type:'COLLECTION_CARD',settings:{},blocks:[{id:'image',type:'IMAGE',settings:{}}]},{id:'recommended',type:'RECOMMENDED_PRODUCTS',settings:{displayType:'grid'},blocks:[{id:'product',type:'PRODUCT_CARD',settings:{}}]}];
 assert.equal(resourceCardLayoutForBlock(blocks,'image','bento'),'bento');
 assert.equal(resourceCardLayoutForBlock(blocks,'header','bento'),undefined);
 assert.equal(resourceCardLayoutForBlock(blocks,'product','editorial'),undefined);
 blocks[2].settings.displayType='editorial';
 assert.equal(resourceCardLayoutForBlock(blocks,'product','grid'),'editorial');
});
