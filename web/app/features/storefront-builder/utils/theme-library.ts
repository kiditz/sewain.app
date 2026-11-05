import type {ThemeDocument} from '~/types/theme-library';
import type {SectionType,StorefrontThemeMode} from '~/types/storefront';
import {applyStorefrontPreset,presetColors} from './storefront-theme';
import {createSection} from '../composables/useStorefrontPages';
interface SectionPreset {
  type:SectionType;
  settings?:Record<string,unknown>;
  heading?:string;
  text?:string;
  eyebrow?:string;
}
interface ThemePreset {
  id:string;name:string;mode:StorefrontThemeMode;
  headingFont:string;radius:number;width:number;spacing:number;
  sections:SectionPreset[];
}
const serif='Georgia, serif';
const sans='Inter, Arial, sans-serif';
const presets:ThemePreset[]=[
  {id:'everyday',name:'Everyday',mode:'light',headingFont:serif,radius:16,width:1200,spacing:64,sections:[
    {type:'HERO'},{type:'FEATURED_PRODUCTS'},{type:'COLLECTION_LIST'},
  ]},
  {id:'studio',name:'Studio',mode:'light',headingFont:sans,radius:0,width:1280,spacing:64,sections:[
    {type:'SLIDESHOW'},{type:'FEATURED_PRODUCTS'},{type:'RICH_TEXT'},
  ]},
  {id:'sunrise',name:'Sunrise',mode:'light',headingFont:serif,radius:24,width:1200,spacing:64,sections:[
    {type:'IMAGE_BANNER'},{type:'COLLECTION_LIST'},{type:'FEATURED_PRODUCTS'},
  ]},
  {id:'trail',name:'Trail',mode:'light',headingFont:sans,radius:8,width:1320,spacing:72,sections:[
    {type:'HERO_BOTTOM',heading:'Ready for the outdoors',eyebrow:'EXPLORE MORE',text:'Gear up for your next escape. Rent what you need and make every adventure count.'},
    {type:'COLLECTION_BENTO',heading:'Find your next adventure'},
    {type:'FEATURED_COLLECTION_CAROUSEL',heading:'Gear for the journey',settings:{count:6}},
    {type:'ICONS_WITH_TEXT'},{type:'FAQ'},
  ]},
  {id:'coast',name:'Coast',mode:'light',headingFont:sans,radius:20,width:1280,spacing:80,sections:[
    {type:'SLIDESHOW_INSET'},
    {type:'COLLECTION_LINKS_SPOTLIGHT',heading:'A little room to explore'},
    {type:'FEATURED_COLLECTION_BENTO',heading:'Fresh finds',settings:{count:6}},
    {type:'IMAGE_WITH_TEXT',heading:'Good days, made simple',text:'Find the things you need for a slower, brighter kind of day.'},
    {type:'EMAIL_SIGNUP'},
  ]},
  {id:'atelier',name:'Atelier',mode:'light',headingFont:serif,radius:0,width:1360,spacing:96,sections:[
    {type:'EDITORIAL_JUMBO',heading:'Objects with a story'},
    {type:'COLLECTION_EDITORIAL',heading:'Thoughtfully curated',settings:{count:4}},
    {type:'FEATURED_COLLECTION_EDITORIAL',heading:'Considered essentials',settings:{count:4}},
    {type:'PULLQUOTE'},{type:'EMAIL_SIGNUP'},
  ]},
  {id:'bloom',name:'Bloom',mode:'light',headingFont:serif,radius:28,width:1160,spacing:72,sections:[
    {type:'HERO',heading:'Make every moment bloom',eyebrow:'SOMETHING SPECIAL',text:'Thoughtful details for the celebrations and everyday moments that matter.',settings:{layout:{width:'page',alignment:'center',direction:'vertical',position:'center',gap:24}}},
    {type:'FEATURED_COLLECTION_CAROUSEL',heading:'For your special moments',settings:{count:6}},
    {type:'MULTICOLUMN'},{type:'COLLECTION_BENTO',heading:'Explore the possibilities'},
    {type:'EMAIL_SIGNUP'},
  ]},
  {id:'mono',name:'Mono',mode:'light',headingFont:sans,radius:0,width:1440,spacing:48,sections:[
    {type:'HERO_MARQUEE',heading:'Only the essentials',eyebrow:'LESS, BUT BETTER',text:'Useful things. Clear choices. Ready when you are.'},
    {type:'FEATURED_PRODUCTS',heading:'The selection',settings:{columns:4,count:8,itemsGap:12}},
    {type:'DIVIDER'},{type:'COLLECTION_LINKS_TEXT',heading:'Browse collections'},
    {type:'RICH_TEXT',heading:'Keep it simple',text:'Rent what you need, for as long as you need it.'},
  ]},
  {id:'midnight',name:'Midnight',mode:'dark',headingFont:sans,radius:12,width:1320,spacing:80,sections:[
    {type:'LAYERED_SLIDESHOW'},
    {type:'FEATURED_COLLECTION_BENTO',heading:'Standout essentials',settings:{count:6}},
    {type:'MARQUEE'},{type:'COLLECTION_LINKS_SPOTLIGHT',heading:'Discover something different'},
    {type:'ICONS_WITH_TEXT'},{type:'EMAIL_SIGNUP'},
  ]},
  {id:'terra',name:'Terra',mode:'light',headingFont:serif,radius:6,width:1240,spacing:88,sections:[
    {type:'EDITORIAL',heading:'A more considered way',eyebrow:'ROOTED IN SIMPLICITY',text:'Beautiful, useful things to share. Make space for experiences, not possessions.'},
    {type:'COLLECTION_BENTO',heading:'Naturally useful'},
    {type:'FEATURED_COLLECTION_EDITORIAL',heading:'Made for everyday living',settings:{count:4}},
    {type:'IMAGE_WITH_TEXT',heading:'Use more. Own less.',text:'Choose what works for you today, and leave room for tomorrow.'},
    {type:'PULLQUOTE'},
  ]},
];
export const themeCatalog=presets.map(preset=>({...preset,colors:presetColors(preset.id,preset.mode)}));

export function createThemeDocument(base:ThemeDocument,name:string):ThemeDocument {
  const preset=presets.find(entry=>entry.id===name.toLowerCase())??presets[0]!;
  const document:ThemeDocument=JSON.parse(JSON.stringify(base));
  document.theme.mode=preset.mode;
  applyStorefrontPreset(document.theme,preset.name);
  document.theme.typography={...document.theme.typography,headingFont:preset.headingFont,bodyFont:sans,baseFontSize:16};
  document.theme.layout={...document.theme.layout,borderRadius:preset.radius,contentMaxWidth:preset.width,sectionSpacing:preset.spacing};
  const home=document.pages.find(page=>page.kind==='HOME');
  if(home){
    const navigation=home.sections.filter(section=>section.type==='NAVIGATION');
    const footer=home.sections.filter(section=>section.type==='FOOTER');
    const sections=preset.sections.map(entry=>{
      const section=createSection(entry.type);
      if(entry.settings){
        const {layout,...settings}=entry.settings;
        section.settings={...section.settings,...settings,...(layout?{layout:{...section.settings.layout as object,...layout as object}}:{})};
      }
      const header=section.blocks.find(block=>block.type==='HEADER');
      for(const block of header?.blocks??[]){
        const copy=block.type==='HEADING'?entry.heading:block.type==='TEXT'?entry.text:block.type==='EYEBROW'?entry.eyebrow:undefined;
        if(copy!==undefined){block.settings.text=copy;block.enabled=true;}
      }
      return section;
    });
    home.sections=[...navigation,...sections,...footer];
  }
  return document;
}
