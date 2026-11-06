import type { Tag } from "./tag";
import type { ProductType } from "./product-type";

export type DurationUnit = "HOUR" | "DAY" | "WEEK" | "MONTH";
export type ProductStatus = "DRAFT" | "ACTIVE" | "UNLISTED" | "ALL";

export interface RentalPlanCommand {
  durationUnit: DurationUnit;
  durationValue: number;
  rentalFee: number;
  active: boolean;
}

export interface ProductVariantCommand {
  id?: string;
  name: string;
  sku: string;
  mediaList: string[];
  buyPrice: number | null;
  depositAmount: number;
  active: boolean;
  attributeValueIds: string[];
  rentalPlans: RentalPlanCommand[];
  inventoryQuantity: number;
  inventoryUnits?: InventoryUnitCommand[];
}

export interface ProductCommand {
  tagIds?: string[];
  productTypeId: string | null;
  name: string;
  slug: string;
  media: string;
  description: string;
  status: ProductStatus;
  variants: ProductVariantCommand[];
}

export interface AttributeValueOption {
  id: string;
  attributeName: string;
  value: string;
  slug: string;
}

export interface Product {
  tags?: Tag[];
  id: string;
  type?: ProductType | null;
  name: string;
  slug: string;
  media: string;
  description: string;
  status: ProductStatus;
  variants: ProductVariant[];
  createdAt: string;
  updatedAt: string;
}

/**
 * Hasil pemilihan di ProductVariantDialog: yang dipilih selalu varian,
 * product hanya disertakan sebagai konteks.
 */
export interface ProductVariantSelection {
  product: Product;
  variant: ProductVariant;
}

export interface ProductVariant {
  inventoryQuantity?: number;
  id: string;
  name: string;
  sku: string;
  mediaList: string[];
  buyPrice: number;
  depositAmount: number;
  active: boolean;
  attributeValueIds: string[];
  values: ProductAttributeValue[];
  rentalPlans: RentalPlan[];
  inventoryUnits: InventoryUnit[];
  createdAt: string;
  updatedAt: string;
}

export interface ProductAttributeValue {
  attributeId: string;
  attributeName: string;
  attributeSlug: string;
  attributeValueId: string;
  value: string;
  slug: string;
  position: number;
}

export interface RentalPlan {
  id: string;
  durationUnit: DurationUnit;
  durationValue: number;
  rentalFee: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}
export interface InventoryUnitCommand {
  id?: string;
  skuBarcode: string;
  serialNumber?: string | null;
  status?: "AVAILABLE" | "RENTED" | "MAINTENANCE" | "DAMAGED";
  conditionNotes?: string | null;
}

export interface InventoryUnit {
  id?: string;
  skuBarcode: string;
  serialNumber?: string | null;
  status?: "AVAILABLE" | "RENTED" | "MAINTENANCE" | "DAMAGED";
  conditionNotes?: string | null;
}
