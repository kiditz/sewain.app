export interface CartRentalPlan {
  plan: {
    id: string;
    durationUnit: string;
    durationValue: number;
    rentalFee: number;
  };
  quantity: number;
}

export interface CartItemCommand {
  productId: string;
  variantId: string;
  quantity: number;
  rentalStartAt: string;
  rentalEndAt: string;
  rentalPlans: CartRentalPlan[];
  rentalSubtotal: number;
  depositSubtotal: number;
  totalAmount: number;
}

export interface CartItemResponse extends CartItemCommand {
  id: string;
  productName: string;
  productSlug: string;
  variantName: string;
  mediaList: [];
  createdAt: string;
  updatedAt: string;
}

export interface CartResponse {
  id: string;
  customerId: string | null;
  guestToken: string | null;
  status: string;
  expiresAt: string | null;
  items: CartItemResponse[];
  createdAt: string;
  updatedAt: string;
}