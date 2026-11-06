import type {Product,ProductVariant} from '~/types/product';
import type {PageResponse} from '~/types/pagination';
export type PublicProduct=Omit<Product,'variants'>&{variants:(Omit<ProductVariant,'buyPrice'|'inventoryUnits'>&{inventoryQuantity:number})[]};
/** Adapt the public projection to shared UI types without requesting private inventory. */
export function storefrontProduct(product:PublicProduct):Product {
  return {...product,variants:(product.variants??[]).map(variant=>({...variant,buyPrice:0,inventoryUnits:[]}))};
}
export function storefrontProducts(page:PageResponse<PublicProduct>):PageResponse<Product>{return {...page,items:page.items.map(storefrontProduct)};}
