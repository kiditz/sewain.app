export interface ProductType {
  id: string;
  name: string;
  slug: string;
}

export interface ProductTypeCommand {
  name: string;
}

export interface ProductTypeSearchParams {
  search?: string;
}
