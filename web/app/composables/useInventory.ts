import type { PageQuery, PageResponse } from "~/types/pagination";

export type InventoryUnitStatus =
  | "AVAILABLE"
  | "RENTED"
  | "MAINTENANCE"
  | "DAMAGED";

export interface Inventory {
  id: string;
  variantId: string | null;
  productId: string | null;
  productName: string | null;
  variantName: string | null;
  variantSku: string | null;
  skuBarcode: string;
  serialNumber: string | null;
  status: InventoryUnitStatus;
  conditionNotes: string | null;
  /**
   * true jika unit tidak punya varian dan belum pernah masuk rental allocation.
   */
  deletable: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface InventoryCommand {
  skuBarcode: string;
  serialNumber?: string | null;
  variantId: string | null;
  status: InventoryUnitStatus;
  conditionNotes: string;
}

export const useInventory = () => {
  const api = useApi();

  const findInventories = (query?: PageQuery & Record<string, unknown>) => {
    return api<PageResponse<Inventory>>("/inventory", {
      query,
    });
  };

  const findInventory = (id: string) => {
    return api<Inventory>(`/inventory/${id}`);
  };

  const createInventory = (data: InventoryCommand) => {
    return api<Inventory>("/inventory", {
      method: "POST",
      body: data,
    });
  };

  const updateInventory = (id: string, data: InventoryCommand) => {
    return api<Inventory>(`/inventory/${id}`, {
      method: "PUT",
      body: data,
    });
  };

  const deleteInventory = (id: string) => {
    return api<void>(`/inventory/${id}`, {
      method: "DELETE",
    });
  };

  return {
    findInventories,
    findInventory,
    createInventory,
    updateInventory,
    deleteInventory,
  };
};
