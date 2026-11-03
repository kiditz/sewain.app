import type {SectionDefinition} from './section-registry';
import type {SectionType,BlockType,SectionLayout} from '~/types/storefront';
import StructuredSection from '../components/sections/StructuredSection.vue';
import ImageCompareSection from '../components/sections/ImageCompareSection.vue';
import ProductHotspotSection from '../components/sections/ProductHotspotSection.vue';
import FeaturedProductSection from '../components/sections/FeaturedProductSection.vue';
import StorefrontFormSection from '../components/sections/StorefrontFormSection.vue';
import {contentSectionDefaults} from './content-section-layout';

export function createAdditionalSections(base: Record<string,SectionDefinition>) {
  function variant(type:SectionType,label:string,source:string,settings:Record<string,unknown>={}):SectionDefinition {
    const original=base[source]!;
    return {...original,type,label,description:label,content:{heading:label,text:'Discover products and collections for your next adventure.'},defaults:{...structuredClone(original.defaults),...settings},defaultBlocks:[...original.defaultBlocks],allowedBlocks:[...original.allowedBlocks]};
  }
  function structured(type:SectionType,label:string,appearance:string,blocks:BlockType[],layout:Partial<SectionLayout>={}):SectionDefinition {
    const defaults=contentSectionDefaults();
    return {type,label,description:label,component:StructuredSection,defaults:{...defaults,variant:appearance,layout:{...defaults.layout,gap:24,...layout}},allowedBlocks:['HEADER','EYEBROW','HEADING','TEXT','IMAGE','BUTTON','ICON_TEXT','COLUMN','DIVIDER','MARQUEE_TEXT','LOGO'],defaultBlocks:blocks,content:{heading:label,text:'Tell your story and help customers discover your store.'}};
  }
  const slideshow=(type:SectionType,label:string,appearance:string)=>variant(type,label,'SLIDESHOW',{variant:appearance,layout:{...base.SLIDESHOW!.defaults.layout as object,width:appearance==='full'?'full':'page'},padding:{top:24,right:24,bottom:24,left:24}});
  const collection=(type:SectionType,label:string,displayType:string)=>({...variant(type,label,'COLLECTION_LIST',{displayType,count:6,itemsGap:displayType==='editorial'?32:18}),...(['text','spotlight'].includes(displayType)?{defaultBlocks:['HEADER'] as BlockType[]}: {})});
  const featured=(type:SectionType,label:string,displayType:string)=>variant(type,label,'FEATURED_PRODUCTS',{displayType,itemsGap:displayType==='editorial'?32:18});
  const staticDefaults=()=>({...contentSectionDefaults(),layout:{width:'page'}});
  const form=(type:SectionType,label:string,formKind:string):SectionDefinition=>({...structured(type,label,'form',['HEADER']),component:StorefrontFormSection,content:{heading:formKind==='CONTACT'?'Get in touch':'Stay in the loop',text:formKind==='CONTACT'?'Send your questions and we will help you plan your rental.':'Sign up for product updates and news from our store.'},defaults:{...staticDefaults(),formKind,buttonLabel:'',successMessage:'',requireConsent:formKind==='EMAIL_SIGNUP'},allowedBlocks:['HEADER','TEXT','IMAGE']});
  const product=(type:SectionType,label:string,appearance:string):SectionDefinition=>({...structured(type,label,appearance,['HEADER','PRODUCT_TITLE','PRODUCT_PRICE','TEXT']),component:FeaturedProductSection,defaults:{...contentSectionDefaults(),layout:{...contentSectionDefaults().layout,direction:'horizontal',gap:32},variant:appearance,productId:'',buttonLabel:''},allowedBlocks:['HEADER','PRODUCT_TITLE','PRODUCT_PRICE','TEXT','EYEBROW']});
  return {
    HERO_BOTTOM: variant('HERO_BOTTOM','Hero: Bottom Aligned','HERO',{layout:{...base.HERO!.defaults.layout as object,position:'bottom'}}),
    HERO_MARQUEE: {...variant('HERO_MARQUEE','Hero: Text Marquee','HERO'),allowedBlocks:[...base.HERO!.allowedBlocks,'HEADER','MARQUEE_TEXT'] as BlockType[],defaultBlocks:['HEADER','MARQUEE_TEXT','BUTTON'] as BlockType[]},
    LARGE_LOGO: structured('LARGE_LOGO','Large Logo','large-logo',['LOGO'],{alignment:'center'}),
    SPLIT_SHOWCASE: structured('SPLIT_SHOWCASE','Split showcase','split',['IMAGE','COLUMN'],{direction:'horizontal',position:'center'}),
    LAYERED_SLIDESHOW: slideshow('LAYERED_SLIDESHOW','Layered Slideshow','layered'),
    SLIDESHOW_INSET: slideshow('SLIDESHOW_INSET','Slideshow: Inset','inset'),
    COLLECTION_LINKS_SPOTLIGHT: collection('COLLECTION_LINKS_SPOTLIGHT','Collection Links: Spotlight','spotlight'),
    COLLECTION_LINKS_TEXT: collection('COLLECTION_LINKS_TEXT','Collection Links: Text','text'),
    COLLECTION_BENTO: collection('COLLECTION_BENTO','Collection List: Bento','bento'),
    COLLECTION_CAROUSEL: collection('COLLECTION_CAROUSEL','Collection List: Carousel','carousel'),
    COLLECTION_EDITORIAL: collection('COLLECTION_EDITORIAL','Collection List: Editorial','editorial'),
    CONTACT_FORM: form('CONTACT_FORM','Contact Form','CONTACT'),
    EMAIL_SIGNUP: form('EMAIL_SIGNUP','Email Signup','EMAIL_SIGNUP'),
    DIVIDER: {...structured('DIVIDER','Divider','divider',['DIVIDER']),allowedBlocks:['DIVIDER'] as BlockType[]},
    CUSTOM_SECTION: structured('CUSTOM_SECTION','Custom Section','custom',[]),
    FEATURED_COLLECTION_CAROUSEL: featured('FEATURED_COLLECTION_CAROUSEL','Featured Collections: Carousel','carousel'),
    FEATURED_COLLECTION_EDITORIAL: featured('FEATURED_COLLECTION_EDITORIAL','Featured Collections: Editorial','editorial'),
    FEATURED_PRODUCT: product('FEATURED_PRODUCT','Featured Product','featured'),
    PRODUCT_HIGHLIGHT: product('PRODUCT_HIGHLIGHT','Product highlight','highlight'),
    PRODUCT_HOTSPOT: {...structured('PRODUCT_HOTSPOT','Product Hotspot','hotspot',['HEADER','IMAGE','HOTSPOT','HOTSPOT']),component:ProductHotspotSection,defaults:{...staticDefaults(),aspectRatio:1.6},allowedBlocks:['HEADER','IMAGE','HOTSPOT'] as BlockType[]},
    RECOMMENDED_PRODUCTS: {...featured('RECOMMENDED_PRODUCTS','Recommended Products','grid'),defaults:{...structuredClone(base.FEATURED_PRODUCTS!.defaults),recommendation:true}},
    EDITORIAL: structured('EDITORIAL','Editorial','split',['HEADER','IMAGE'],{direction:'horizontal',position:'center'}),
    EDITORIAL_JUMBO: structured('EDITORIAL_JUMBO','Editorial Jumbo Text','jumbo',['HEADER','IMAGE']),
    IMAGE_COMPARE: {...structured('IMAGE_COMPARE','Image Compare','compare',['HEADER','IMAGE','IMAGE']),component:ImageCompareSection,defaults:{...staticDefaults(),aspectRatio:1.6,beforeLabel:'',afterLabel:''},allowedBlocks:['HEADER','IMAGE'] as BlockType[],maxBlocks:3},
    IMAGE_WITH_TEXT: structured('IMAGE_WITH_TEXT','Image with text','split',['IMAGE','HEADER'],{direction:'horizontal',position:'center'}),
    FAQ: {...structured('FAQ','FAQ','faq',['HEADER','FAQ_ITEM','FAQ_ITEM','FAQ_ITEM']),allowedBlocks:['HEADER','FAQ_ITEM'] as BlockType[]},
    ICONS_WITH_TEXT: {...structured('ICONS_WITH_TEXT','Icons with Text','columns',['ICON_TEXT','ICON_TEXT','ICON_TEXT'],{direction:'horizontal'}),allowedBlocks:['HEADER','ICON_TEXT'] as BlockType[]},
    MARQUEE: {...structured('MARQUEE','Marquee','marquee',['MARQUEE_TEXT'],{width:'full'}),allowedBlocks:['MARQUEE_TEXT'] as BlockType[]},
    MULTICOLUMN: {...structured('MULTICOLUMN','Multicolumn','columns',['COLUMN','COLUMN','COLUMN'],{direction:'horizontal'}),allowedBlocks:['COLUMN'] as BlockType[]},
    PULLQUOTE: {...structured('PULLQUOTE','Pullquote','pullquote',['HEADER'],{alignment:'center'}),content:{heading:'“Great experiences begin with the right gear.”',text:'A happy customer'}},
    POLICIES_LINKS: {...variant('POLICIES_LINKS','Policies & Links','FOOTER'),content:{text:'© {year}'},defaultBlocks:['MENU','TEXT'] as BlockType[],allowedBlocks:['MENU','TEXT','LOGO','SOCIAL_LINK'] as BlockType[]},
  } satisfies Partial<Record<SectionType,SectionDefinition>>;
}
