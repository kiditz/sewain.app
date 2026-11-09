export interface StoreInformation {
  description: string | null;
  logoUrl: string | null;
  faviconUrl: string | null;
  contactEmail: string | null;
  contactPhone: string | null;
}

export interface BusinessInformation {
  name: string | null;
  address: string | null;
  email: string | null;
  phone: string | null;
  taxId: string | null;
}

export interface Merchant {
  id: string;
  name: string;
  slug: string;
  schema: string;
  storeInformation: StoreInformation;
  businessInformation: BusinessInformation;
  customDomain: string | null;
  published: boolean;
  provisioningStatus: "PROVISIONING" | "READY" | "FAILED";
  provisioningVersion: number;
}

export interface MerchantCommand {
  name: string;
  slug: string;
  storeInformation?: StoreInformation;
  businessInformation?: BusinessInformation;
  customDomain?: string | null;
  published?: boolean;
}