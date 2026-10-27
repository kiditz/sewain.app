import { useStorefrontState as useState } from "./useStorefrontState";
import type { Product } from "~/types/product";
import type { CollectionSummary } from "~/types/collection";
import type { PageResponse } from "~/types/pagination";

export function useStorefrontCatalog() {
  const products = useState<Product[]>("storefront-products", () => []);
  const collections = useState<CollectionSummary[]>("storefront-collections", () => []);
  const catalogLoaded = useState("storefront-catalog-loaded", () => false);
  const api = useApi();
  const productStore = useStorefrontProductStore();
  async function loadCatalog() {
    if (catalogLoaded.value) return;
    const loadAllActiveProducts = async () => {
      const pageSize = 100;
      const firstPage = await api<PageResponse<Product>>("/products", {
        query: { page: 0, size: pageSize, status: "ACTIVE" },
      });
      const pageCount = Math.ceil(firstPage.totalItems / pageSize);
      const remainingPages = await Promise.all(
        Array.from({ length: Math.max(0, pageCount - 1) }, (_, index) =>
          api<PageResponse<Product>>("/products", {
            query: { page: index + 1, size: pageSize, status: "ACTIVE" },
          }),
        ),
      );
      return [firstPage, ...remainingPages].flatMap((page) => page.items);
    };

    const results = await Promise.allSettled([
      loadAllActiveProducts(),
      api<CollectionSummary[]>("/collections"),
    ]);
    const productsResult = results[0];
    const collectionsResult = results[1];
    if (productsResult?.status === "fulfilled") {
      products.value = productsResult.value;
      if (products.value.length > 0) {
        productStore.setProduct(products.value[0]!);
      }
    }
    if (collectionsResult?.status === "fulfilled") {
      collections.value = collectionsResult.value;
    }
    catalogLoaded.value = true;
  }
  return { products, collections, loadCatalog };
}
