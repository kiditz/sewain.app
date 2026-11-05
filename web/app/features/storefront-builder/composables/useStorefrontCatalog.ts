import {getCurrentInstance,inject} from 'vue';
import {themeDemoCatalogKey} from '../utils/catalog-demo';
import { useStorefrontState as useState } from "./useStorefrontState";
import type { Product } from "~/types/product";
import type { ProductType } from "~/types/product-type";
import type { CollectionSummary } from "~/types/collection";
import type { PageResponse } from "~/types/pagination";
import type { ProductSearchQuery } from "~/composables/useProduct";
const inFlight = new WeakMap<object, Map<string, Promise<unknown>>>();

export function useStorefrontCatalog() {
  const demo=getCurrentInstance()?inject(themeDemoCatalogKey,null):null;
  const app = useNuxtApp();
  const api = useApi();
  const scope = `${useRequestURL().host}:${useMerchantStore().current?.slug ?? "guest"}`;
  const products = useState<Product[]>("storefront-products", () => []);
  const totalProducts = useState("storefront-products-total", () => 0);
  const collections = useState<CollectionSummary[]>("storefront-collections", () => []);
  const collectionsLoaded = useState("storefront-collections-loaded", () => false);
  const productTypes = useState<ProductType[]>("storefront-product-types", () => []);
  const typesLoaded = useState("storefront-product-types-loaded", () => false);
  const queryKey = (path: string, query: Record<string, unknown> = {}) =>
    `storefront-catalog:${demo?"demo:":""}${scope}:${path}:${JSON.stringify(Object.entries(query).filter(([, value]) => value != null).sort(([a], [b]) => a.localeCompare(b)))}`;
  function dedupe<T>(key: string, request: () => Promise<T>): Promise<T> {
    let pending = inFlight.get(app);
    if (!pending) { pending = new Map(); inFlight.set(app, pending); }
    const existing = pending.get(key);
    if (existing) return existing as Promise<T>;
    const task = Promise.resolve().then(request).finally(() => pending!.delete(key));
    pending.set(key, task);
    return task;
  }
  function pageQuery(query: ProductSearchQuery = {}) {
    return { status: "ACTIVE" as const, ...query,
      page: Math.max(0, Math.floor(Number(query.page) || 0)),
      size: Math.max(1, Math.min(100, Math.floor(Number(query.size) || 20))),
    };
  }
  function fetchProducts(query: ProductSearchQuery = {}) {
    const normalized = pageQuery(query);
    if(demo){
      const ids=query.id?.split(',');
      const items=demo.products.filter(product=>(!ids||ids.includes(product.id))&&(!query.keyword||product.name.toLowerCase().includes(query.keyword.toLowerCase())));
      return Promise.resolve({items:items.slice(normalized.page*normalized.size,(normalized.page+1)*normalized.size),totalItems:items.length});
    }
    return dedupe(queryKey("products", normalized), () => api<PageResponse<Product>>("/products", { query: normalized }));
  }
  function fetchCollectionProducts(id: string, query: ProductSearchQuery = {}) {
    if(demo)return fetchProducts(query);
    const normalized = pageQuery(query);
    return dedupe(queryKey(`collections/${id}/products`, normalized), () => api<PageResponse<Product>>(`/collections/${id}/products`, { query: normalized }));
  }
  async function loadProducts(query: ProductSearchQuery = {}) {
    const result = await fetchProducts(query);
    products.value = result.items;
    totalProducts.value = result.totalItems;
    return result;
  }
  async function loadCollections() {
    if(demo){collections.value=demo.collections;return demo.collections;}
    if (collectionsLoaded.value) return collections.value;
    return dedupe(queryKey("collections"), async () => {
      collections.value = await api<CollectionSummary[]>("/collections");
      collectionsLoaded.value = true;
      return collections.value;
    });
  }
  async function loadProductTypes() {
    if(demo)return [];
    if (typesLoaded.value) return productTypes.value;
    return dedupe(queryKey("product-types"), async () => {
      productTypes.value = await api<ProductType[]>("/products/types");
      typesLoaded.value = true;
      return productTypes.value;
    });
  }
  // Compatibility entry point: one requested page, never the entire catalog.
  async function loadCatalog(query: ProductSearchQuery = {}) {
    return Promise.all([loadProducts(query), loadCollections()]);
  }
  return { products, totalProducts, collections, productTypes, queryKey,
    fetchProducts, fetchCollectionProducts, loadProducts, loadCollections, loadProductTypes, loadCatalog };
}
