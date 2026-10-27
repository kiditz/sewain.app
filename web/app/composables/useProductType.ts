import type {
  ProductType,
  ProductTypeCommand,
  ProductTypeSearchParams,
} from "~/types/product-type";

export const useProductType = () => {
  const api = useApi();

  const find = (params: ProductTypeSearchParams = {}) =>
    api<ProductType[]>("/product-types", {
      query: params,
    });

  const findById = (id: string) => api<ProductType>(`/product-types/${id}`);

  const create = (command: ProductTypeCommand) =>
    api<ProductType>("/product-types", {
      method: "POST",
      body: command,
    });

  const remove = (id: string) =>
    api<void>(`/product-types/${id}`, {
      method: "DELETE",
    });

  return {
    find,
    findById,
    create,
    remove,
  };
};
