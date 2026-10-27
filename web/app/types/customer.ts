export interface Customer {
  id: string;
  fullName: string;
  email: string;
  phone: string | null;
  notes: string | null;
  orderCount: number;
  totalSpent: number;
  lastOrderAt: string | null;
  createdAt: string;
  enabled: boolean;
}

export interface CustomerCommand {
  fullName: string;
  email: string;
  phone?: string;
  notes?: string;
}

export interface CustomerStatistics {
  total: number;
  active: number;
  newThisMonth: number;
  repeatCustomers: number;
}

export interface CustomerPageQuery {
  page: number;
  size: number;
  keyword?: string;
  enabled?: boolean;
  sortBy: string;
  direction: "asc" | "desc";
}
