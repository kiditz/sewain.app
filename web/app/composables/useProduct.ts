import type { PageQuery, PageResponse } from "~/types/pagination";
import type { Product, ProductCommand, ProductStatus } from "~/types/product";

export interface ProductSearchQuery extends PageQuery {
  id?: string;
  status?: ProductStatus;
  keyword?: string;
  name?: string;
  "type.id"?: string;
  "tags.id"?: string;
  available?: boolean;
  minimumRentalPrice?: number;
  maximumRentalPrice?: number;
}

export const useProduct = () => {
  const api = useApi();
  
  const findProducts = (
    query?: ProductSearchQuery,
  ) => {
    return api<PageResponse<Product>>("/products", {
      query,
    });
  };

  const findProduct = (id: string) => {
    return api<Product>(`/products/${id}`);
  };

  const findProductBySlug = (slug: string) => {
    return api<Product>(`/products/slug/${slug}`);
  };

  const createProduct = (data: ProductCommand) => {
    return api<Product>("/products", {
      method: "POST",
      body: data,
    });
  };

  const updateProduct = (id: string, data: ProductCommand) => {
    return api<Product>(`/products/${id}`, {
      method: "PUT",
      body: data,
    });
  };

  const deleteProduct = (id: string) => {
    return api<void>(`/products/${id}`, {
      method: "DELETE",
    });
  };

  return {
    findProducts,
    findProduct,
    findProductBySlug,
    createProduct,
    updateProduct,
    deleteProduct,
  };
};
