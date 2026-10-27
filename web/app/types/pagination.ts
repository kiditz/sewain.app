export interface PageQuery {
  page?: number;
  size?: number;
  sortBy?: string;
  direction?: "asc" | "desc";
}



export interface PageResponse<T> {
  items: T[];
  totalItems: number;
}