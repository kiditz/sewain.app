import type {
  Collection,
  CollectionCommand,
  CollectionSummary,
} from "~/types/collection";
import type { PageResponse } from "~/types/pagination";
import type { Product } from "~/types/product";

export const useCollection = () => {
  const api = useApi();

  const findCollections = () => {
    return api<CollectionSummary[]>("/collections");
  };

  const findCollection = (id: string) => {
    return api<Collection>(`/collections/id/${id}`);
  };

  const findCollectionBySlug = (slug: string) => {
    return api<Collection>(`/collections/${slug}`);
  };

  const createCollection = (command: CollectionCommand) => {
    return api<Collection>("/collections", {
      method: "POST",
      body: command,
    });
  };

  const updateCollection = (id: string, command: CollectionCommand) => {
    return api<Collection>(`/collections/${id}`, {
      method: "PUT",
      body: command,
    });
  };

  const deleteCollection = (id: string) => {
    return api<void>(`/collections/${id}`, {
      method: "DELETE",
    });
  };

  /**
   * Produk di dalam collection:
   * MANUAL sesuai urutan pilihan, AUTOMATED hasil evaluasi rules.
   */
  const findCollectionProducts = (
    id: string,
    query?: { page?: number; size?: number; keyword?: string },
  ) => {
    return api<PageResponse<Product>>(`/collections/${id}/products`, {
      query,
    });
  };

  return {
    findCollections,
    findCollection,
    findCollectionBySlug,
    createCollection,
    updateCollection,
    deleteCollection,
    findCollectionProducts,
  };
};
