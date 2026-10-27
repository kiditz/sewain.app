import type { CategoryCommand, Category } from "~/types/category";
import type { PageQuery, PageResponse } from "~/types/pagination";

export const useCategory = () => {
  const api = useApi();
  const findCategories = () => {
    return api<Category[]>("/categories");
  };
  const searchCategories = (query?: PageQuery) => {
    return api<PageResponse<Category>>("/categories/search", {
      query,
    });
  };
  const reorderCategories = (categoryIds: string[]) => {
    return api<void>("/categories/reorder", {
      method: "PUT",
      body: {
        categoryIds,
      },
    });
  };

  const createCategory = (command: CategoryCommand) => {
    return api<Category>("/categories", {
      method: "POST",
      body: command,
    });
  };
  const updateCategory = (id: string, command: CategoryCommand) => {
    return api<Category>(`/categories/${id}`, {
      method: "PUT",
      body: command,
    });
  };
  const deleteCategory = (id: string) => {
    return api<Category>(`/categories/${id}`, {
      method: "DELETE",
    });
  };
  return {
    findCategories,
    searchCategories,
    createCategory,
    updateCategory,
    deleteCategory,
    reorderCategories,
  };
};
