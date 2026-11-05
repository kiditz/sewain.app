import type {ThemeDocument} from '~/types/theme-library';
import type {SectionType,StorefrontThemeMode} from '~/types/storefront';
import {applyStorefrontPreset,presetColors} from './storefront-theme';
import {themeCopy,localizeThemeBlocks} from './theme-copy';
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

export function createThemeDocument(base:ThemeDocument,name:string,locale='en'):ThemeDocument {
  const preset=presets.find(entry=>entry.id===name.toLowerCase())??presets[0]!;
  const document:ThemeDocument=JSON.parse(JSON.stringify(base));
  document.theme.mode=preset.mode;
  applyStorefrontPreset(document.theme,preset.name);
  document.theme.typography={...document.theme.typography,headingFont:preset.headingFont,bodyFont:sans,baseFontSize:16};
  document.theme.layout={...document.theme.layout,borderRadius:preset.radius,contentMaxWidth:preset.width,sectionSpacing:preset.spacing};
  const requiredPages=[
    {kind:'PRODUCT',slug:'product',targetUrl:'/products/:slug',title:locale.startsWith('id')?'Produk':'Product'},
    {kind:'COLLECTIONS',slug:'collections',targetUrl:'/collections',title:locale.startsWith('id')?'Koleksi':'Collections'},
    {kind:'PRODUCT_GRID',slug:'all',targetUrl:'/collections/all',title:locale.startsWith('id')?'Semua produk':'All products'},
    {kind:'CART',slug:'cart',targetUrl:'/cart',title:locale.startsWith('id')?'Keranjang':'Cart'},
    {kind:'CHECKOUT',slug:'checkout',targetUrl:'/checkout',title:'Checkout'},
  ] as const;
  for(const page of requiredPages){
    if(document.pages.some(existing=>existing.kind===page.kind))continue;
    let slug:string=page.slug;
    for(let suffix=1;document.pages.some(existing=>existing.slug===slug);suffix++)slug=`${page.slug}-template-${suffix}`;
    document.pages.push({...page,slug,id:crypto.randomUUID(),status:'PUBLISHED',icon:'FileOutlined',sections:[],updatedAt:new Date().toISOString()});
  }
  const home=document.pages.find(page=>page.kind==='HOME');
  if(home){
    const navigation=home.sections.filter(section=>section.type==='NAVIGATION');
    const footer=home.sections.filter(section=>section.type==='FOOTER');
    const sections=preset.sections.map(entry=>{
      const section=createSection(entry.type);
      localizeSection(section,locale);
      const heroCopy:Record<string,[string,string]>={studio:['Designed for your ideas','Dirancang untuk idemu'],coast:['Good days, made simple','Hari menyenangkan, tanpa repot'],midnight:['Make a different impression','Saatnya tampil berbeda']};
      if(heroCopy[preset.id]&&['SLIDESHOW','SLIDESHOW_INSET','LAYERED_SLIDESHOW'].includes(section.type))for(const slide of section.blocks){
        const blocks=(slide.blocks??[]).flatMap(block=>block.type==='HEADER'?block.blocks??[]:[block]);
        const title=blocks.find(block=>block.type==='HEADING');if(title)title.settings.text=heroCopy[preset.id]![locale.startsWith('id')?1:0];
      }
      addThemeDemoImages(section,preset.id);
      if(entry.settings){
        const {layout,...settings}=entry.settings;
        section.settings={...section.settings,...settings,...(layout?{layout:{...section.settings.layout as object,...layout as object}}:{})};
      }
      const copyBlocks=section.blocks.flatMap(block=>block.type==='HEADER'?block.blocks??[]:[block]);
      for(const block of copyBlocks){
        const copy=block.type==='HEADING'?entry.heading:block.type==='TEXT'?entry.text:block.type==='EYEBROW'?entry.eyebrow:undefined;
        if(copy!==undefined){block.settings.text=themeCopy(copy,locale);block.enabled=true;}
      }
      return section;
    });
    home.sections=[...navigation,...sections,...footer];
  }
  for(const page of document.pages){
    if(['HOME','CUSTOM'].includes(page.kind))continue;
    const sections=page.kind==='PRODUCT'?[createSection('PRODUCT_INFORMATION'),createSection('PRODUCT_DESCRIPTION'),createSection(['atelier','terra'].includes(preset.id)?'FEATURED_COLLECTION_EDITORIAL':'FEATURED_COLLECTION_CAROUSEL')]
      :page.kind==='COLLECTIONS'?[createSection(['trail','coast','bloom','midnight','terra'].includes(preset.id)?'COLLECTION_BENTO':preset.id==='atelier'?'COLLECTION_EDITORIAL':'COLLECTION_LIST')]
      :page.kind==='PRODUCT_GRID'?[createSection('PRODUCT_GRID')]
      :page.kind==='CART'?[createSection('CART')]:page.kind==='CHECKOUT'?[createSection('CHECKOUT')]:[];
    for(const section of sections){
      localizeSection(section,locale);
      section.settings.layout={...section.settings.layout as object,width:'page'};
      section.settings.padding={...section.settings.padding as object,top:preset.spacing/2,bottom:preset.spacing/2};
      if(section.type==='PRODUCT_INFORMATION')section.settings.mediaPosition=['coast','atelier','terra'].includes(preset.id)?'right':'left';
      if(section.type==='PRODUCT_GRID'){section.settings.columns=preset.id==='mono'?4:3;section.settings.productsPerPage=preset.id==='mono'?16:12;}
    }
    const navigation=home?.sections.filter(section=>section.type==='NAVIGATION')??[];
    const footer=home?.sections.filter(section=>section.type==='FOOTER')??[];
    page.sections=[...structuredClone(navigation),...sections,...structuredClone(footer)];
  }
  return document;
}

function addThemeDemoImages(section:ReturnType<typeof createSection>,id:string){
  const src=`/themes/demo/${id}.svg`;
  if(['HERO','HERO_BOTTOM','HERO_MARQUEE'].includes(section.type)){
    section.settings.medias=[{src,alt:''}];
    section.settings.appearance={backgroundColor:'',overlayColor:'#ffffff',overlayOpacity:35};
    section.settings.layout={...section.settings.layout as object,height:'custom',customHeight:560};
  }
  if(section.type==='IMAGE_BANNER'){
    section.settings.image={src,alt:''};
    section.settings.layout={...section.settings.layout as object,height:'custom',customHeight:520,direction:'vertical',alignment:'left',position:'center',gap:20};
  }
  const visit=(blocks:typeof section.blocks)=>{for(const block of blocks){
    if(block.type==='IMAGE')block.settings.src=src;
    if(block.type==='SLIDE'){
      block.settings.image={src:blocks.indexOf(block)%2?`/themes/demo/${id}-alt.svg`:src,alt:''};
      block.settings.appearance={...block.settings.appearance as object,overlayColor:id==='midnight'?'#13111d':'#ffffff',overlayOpacity:22};
    }
    if(block.blocks)visit(block.blocks);
  }};
  // Product/collection template images stay bound to the merchant's resources.
  visit(section.blocks.filter(block=>!['PRODUCT_CARD','COLLECTION_CARD'].includes(block.type)));
}

function localizeSection(section:ReturnType<typeof createSection>,locale:string){
  localizeThemeBlocks(section.blocks,locale);
  const id=locale.startsWith('id');
  const headings:Partial<Record<SectionType,[string,string]>>={
    FEATURED_PRODUCTS:['Popular rentals','Pilihan favorit'],FEATURED_COLLECTION_CAROUSEL:['Discover the selection','Temukan pilihan kami'],FEATURED_COLLECTION_BENTO:['Standout essentials','Pilihan istimewa'],FEATURED_COLLECTION_EDITORIAL:['Considered essentials','Pilihan yang bermakna'],COLLECTION_LIST:['Explore collections','Jelajahi koleksi'],COLLECTION_BENTO:['Explore collections','Jelajahi koleksi'],COLLECTION_EDITORIAL:['Discover collections','Temukan koleksi'],COLLECTION_LINKS_SPOTLIGHT:['Explore more','Jelajahi lebih jauh'],COLLECTION_LINKS_TEXT:['Browse collections','Jelajahi koleksi'],EMAIL_SIGNUP:['Stay in the loop','Jangan lewatkan kabar terbaru'],FAQ:['Your questions, answered','Jawaban untuk pertanyaanmu'],CART:['Your cart','Keranjangmu'],CHECKOUT:['Checkout','Checkout'],PRODUCT_DESCRIPTION:['The details','Detail produk'],PRODUCT_GRID:['All products','Semua produk'],PULLQUOTE:['Less to own. More to experience.','Lebih sedikit barang. Lebih banyak pengalaman.'],RICH_TEXT:['A little about us','Tentang kami'],
  };
  const heading=headings[section.type];
  if(heading){
    const blocks=section.blocks.flatMap(block=>block.type==='HEADER'?block.blocks??[]:[block]);
    const title=blocks.find(block=>block.type==='HEADING');
    if(title)title.settings.text=heading[id?1:0];
    if(['CART','CHECKOUT'].includes(section.type))for(const block of blocks){if(block.type==='TEXT')block.settings.text=id?'Periksa pilihanmu dan lengkapi pesanan sewa.':'Review your selection and complete your rental order.';if(block.type==='EYEBROW')block.settings.text=id?'PESANAN SEWA':'YOUR RENTAL';}
  }
}
