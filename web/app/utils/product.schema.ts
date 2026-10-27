import { z } from "zod";
import { toTypedSchema } from "@vee-validate/zod";

type Translate = (key: string) => string;

export function createProductSchema(t: Translate) {
  const inventoryUnitSchema = z.object({
    id: z.string().optional(),
    skuBarcode: z.string().trim().min(1, "Barcode is required"),
    serialNumber: z.string().nullable().optional(),
    status: z
      .enum(["AVAILABLE", "RENTED", "MAINTENANCE", "DAMAGED"])
      .optional(),
    conditionNotes: z.string().nullable().optional(),
  });

  const rentalPlanSchema = z.object({
    durationUnit: z.enum(["HOUR", "DAY", "WEEK", "MONTH"]),
    durationValue: z.number().int().min(1),
    rentalFee: z.number().min(0),
    active: z.boolean(),
  });

  const productVariantSchema = z.object({
    id: z.string().optional(),
    name: z.string().trim().min(1, t("product.variant.name.required")),
    sku: z.string().trim().min(1, t("product.variant.sku.required")),
    mediaList: z.array(z.string()),
    buyPrice: z.number().min(0).nullable(),
    depositAmount: z.number().min(0),
    active: z.boolean(),
    attributeValueIds: z.array(z.string()),
    inventoryQuantity: z
      .number()
      .int()
      .min(0, t("product.inventory.quantity.invalid")),
    inventoryUnits: z.array(inventoryUnitSchema),
    rentalPlans: z.array(rentalPlanSchema),
  });

  const productSchema = z.object({
    productTypeId: z.string().nullable(),
    tagIds: z.array(z.string().uuid()).default([]),
    name: z.string().trim().min(1, t("product.validation.nameRequired")),
    slug: z.string().trim().min(1, t("product.validation.slugRequired")),
    media: z.string(),
    description: z.string(),
    status: z.enum(["DRAFT", "ACTIVE", "UNLISTED"]),
    variants: z.array(productVariantSchema).min(1, "product.variant.required"),
  });

  return {
    schema: toTypedSchema(productSchema),
    productSchema,
    productVariantSchema,
    inventoryUnitSchema,
    rentalPlanSchema,
  };
}

export type ProductFormValues = z.infer<
  ReturnType<typeof createProductSchema>["productSchema"]
>;

export type ProductVariantFormValues = z.infer<
  ReturnType<typeof createProductSchema>["productVariantSchema"]
>;

export type InventoryUnitFormValues = z.infer<
  ReturnType<typeof createProductSchema>["inventoryUnitSchema"]
>;

export type RentalPlanFormValues = z.infer<
  ReturnType<typeof createProductSchema>["rentalPlanSchema"]
>;
