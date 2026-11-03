import type {CSSProperties,ComputedRef,InjectionKey} from 'vue';
import type {StorefrontBlock} from '~/types/storefront';

export const resourceCardLayoutKey:InjectionKey<ComputedRef<string>>=Symbol('resource-card-layout');
export type ResourceKind='collections'|'products';
export interface GridPlacement { column:number; span:number; row:number; rows:number }
export interface ResourcePlacement { desktop:GridPlacement; mobile:{column?:number;span?:number;row?:number;width?:number;align?:'flex-start'|'flex-end'|'center';ratio?:string} }

// Each desktop row fills twelve tracks. Small final groups have their own
// composition so they never leave an accidental hole at the end of the list.
const bentoSpans:Record<number,number[]>={
  1:[12],2:[6,6],3:[3,6,3],4:[4,8,8,4],5:[3,6,3,6,6],
  6:[3,6,3,6,3,3],7:[3,6,3,9,3,6,6],8:[3,6,3,6,3,3,3,9],
  9:[3,6,3,6,3,3,3,3,6],10:[3,6,3,6,3,3,9,3,3,9],
  11:[3,6,3,6,3,3,3,3,6,3,9],12:[3,6,3,6,3,3,3,3,6,4,4,4],
};
export function bentoPlacements(count:number):ResourcePlacement[]{
  const total=Number.isFinite(count)?Math.min(100,Math.max(0,Math.floor(count))):0;const result:ResourcePlacement[]=[];
  let remaining=total,row=1,mobileRow=1;
  while(remaining){
    // Avoid a single orphan card after a full group (13 -> 11 + 2).
    const size=remaining>12&&remaining%12===1?11:Math.min(12,remaining);
    let column=1,mobileColumn=1;
    for(const [index,span] of bentoSpans[size]!.entries()){
      const mobileSpan=index%3===2||(index===size-1&&index%3===0)?2:1;
      result.push({desktop:{column,span,row,rows:1},mobile:{column:mobileColumn,span:mobileSpan,row:mobileRow}});
      column+=span;if(column===13){column=1;row++;}
      mobileColumn+=mobileSpan;if(mobileColumn===3){mobileColumn=1;mobileRow++;}
    }
    remaining-=size;
  }
  return result;
}

const editorial:Record<ResourceKind,{positions:number[][];stride:number;mobile:{width:number;align:'flex-start'|'flex-end'|'center';ratio:string}[]}>= {
  collections:{positions:[[2,4,1,5],[7,5,3,5],[1,8,9,6],[3,8,16,6],[7,5,23,5],[2,4,25,5],[5,8,31,6],[2,8,38,6]],stride:44,mobile:[{width:66,align:'flex-start',ratio:'4 / 5'},{width:83,align:'flex-end',ratio:'1 / 1'},{width:83,align:'flex-start',ratio:'8 / 6'},{width:100,align:'center',ratio:'8 / 6'}]},
  products:{positions:[[1,7,1,6],[9,4,5,5],[2,5,8,5],[5,8,14,6],[1,7,21,6],[9,4,25,5],[2,5,28,5],[3,8,34,6]],stride:40,mobile:[{width:83,align:'flex-start',ratio:'7 / 6'},{width:83,align:'flex-end',ratio:'4 / 5'},{width:66,align:'flex-start',ratio:'1 / 1'},{width:100,align:'center',ratio:'8 / 6'}]},
};
export function editorialPlacement(index:number,kind:ResourceKind):ResourcePlacement{
  const pattern=editorial[kind];const [column,span,row,rows]=pattern.positions[index%8]!;
  return {desktop:{column:column!,span:span!,row:row!+Math.floor(index/8)*pattern.stride,rows:rows!},mobile:{...pattern.mobile[index%4]!}};
}
export function resourceListItemStyles(layout:string,count:number,kind:ResourceKind):CSSProperties[]{
  count=Number.isFinite(count)?Math.min(100,Math.max(0,Math.floor(count))):0;
  const bento=layout==='bento'?bentoPlacements(count):[];
  return Array.from({length:count},(_,index)=>{
    const placement=layout==='bento'?bento[index]:layout==='editorial'?editorialPlacement(index,kind):undefined;
    if(!placement)return {};
    return {
      '--resource-desktop-column':`${placement.desktop.column} / span ${placement.desktop.span}`,
      '--resource-desktop-row':`${placement.desktop.row} / span ${placement.desktop.rows}`,
      '--resource-mobile-column':`${placement.mobile.column??1} / span ${placement.mobile.span??1}`,
      '--resource-mobile-row':String(placement.mobile.row??1),
      '--resource-mobile-width':`${placement.mobile.width??100}%`,
      '--resource-mobile-align':placement.mobile.align??'stretch',
      '--resource-mobile-ratio':placement.mobile.ratio??'auto',
    };
  });
}

/** The nearest resource list owns the card's media dimensions. */
export function resourceCardLayoutForBlock(blocks:StorefrontBlock[],id:string,layout:string,insideCard=false):string|undefined {
  for(const block of blocks){
    const mode=block.type==='RECOMMENDED_PRODUCTS'?String(block.settings.displayType??'grid'):layout;
    const card=insideCard||['COLLECTION_CARD','PRODUCT_CARD'].includes(block.type);
    if(block.id===id)return card&&['bento','editorial'].includes(mode)?mode:undefined;
    const nested=resourceCardLayoutForBlock(block.blocks??[],id,mode,card);
    if(nested)return nested;
  }
}
