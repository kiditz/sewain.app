import type {
  Customer,
  CustomerCommand,
  CustomerPageQuery,
  CustomerStatistics,
} from "~/types/customer";
import type { PageResponse } from "~/types/pagination";

export const useCustomer = () => {
  const api = useApi();

  const findCustomers = (query: CustomerPageQuery) =>
    api<PageResponse<Customer>>("/merchant/customers", { query });

  const findCustomer = (id: string) =>
    api<Customer>(`/merchant/customers/${id}`);

  const getCustomerStatistics = () =>
    api<CustomerStatistics>("/merchant/customers/summary");

  const createCustomer = (command: CustomerCommand) =>
    api<Customer>("/merchant/customers", { method: "POST", body: command });

  const setCustomerStatus = (id: string, enabled: boolean) =>
    api<Customer>(`/merchant/customers/${id}/status`, {
      method: "PATCH",
      body: { enabled },
    });

  return {
    findCustomers,
    findCustomer,
    getCustomerStatistics,
    createCustomer,
    setCustomerStatus,
  };
};
