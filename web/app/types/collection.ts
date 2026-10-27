export type CollectionType = "MANUAL" | "AUTOMATED";

export type CollectionMatchCondition = "ALL" | "ANY";

export type CollectionRuleAttribute =
  | "TITLE"
  | "TYPE"
  | "TAG"
  | "VARIANT_TITLE"
  | "ATTRIBUTE_VALUE";

export type CollectionRuleOperator =
  | "EQUALS"
  | "NOT_EQUALS"
  | "CONTAINS"
  | "NOT_CONTAINS"
  | "STARTS_WITH"
  | "ENDS_WITH";

export interface CollectionRule {
  id?: string;
  attribute: CollectionRuleAttribute;
  operator: CollectionRuleOperator;
  /** Beberapa value dalam satu rule = salah satu cocok (OR). */
  values: string[];
}

/** Ringkasan untuk daftar / tree. */
export interface CollectionSummary {
  id: string;
  name: string;
  slug: string;
  imageUrl: string | null;
  parentId: string | null;
  description: string | null;
  type: CollectionType;
  matchCondition: CollectionMatchCondition;
  /** Jumlah produk manual. Collection AUTOMATED selalu 0. */
  productCount: number;
}

/** Detail untuk form edit. */
export interface Collection {
  id: string;
  name: string;
  slug: string;
  imageUrl: string | null;
  parentId: string | null;
  description: string | null;
  type: CollectionType;
  matchCondition: CollectionMatchCondition;
  rules: CollectionRule[];
  /** Produk manual, terurut. */
  productIds: string[];
}

export interface CollectionCommand {
  name: string;
  slug: string;
  imageUrl?: string | null;
  parentId?: string | null;
  description?: string;
  type: CollectionType;
  matchCondition: CollectionMatchCondition;
  /** Dikirim hanya untuk AUTOMATED. */
  rules?: CollectionRule[];
  /** Dikirim hanya untuk MANUAL; urutan = urutan tampil. */
  productIds?: string[];
}
