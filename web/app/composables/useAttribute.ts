import type { Attribute, AttributeCommand } from "~/types/attribute";
import type { PageQuery, PageResponse } from "~/types/pagination";

export const useAttribute = () => {
  const api = useApi();
  const searchAttributes = (query?: PageQuery) => {
    return api<PageResponse<Attribute>>("/attributes/search", {
      query,
    });
  };
  const findAttributes = () => {
    return api<Attribute[]>("/attributes");
  };

  const findAttribute = (id: string) => {
    return api<Attribute>(`/attributes/${id}`);
  };

  const createAttribute = (data: AttributeCommand) => {
    return api<Attribute>("/attributes", {
      method: "POST",
      body: data,
    });
  };

  const updateAttribute = (id: string, data: AttributeCommand) => {
    return api<Attribute>(`/attributes/${id}`, {
      method: "PUT",
      body: data,
    });
  };

  const deleteAttribute = (id: string) => {
    return api<void>(`/attributes/${id}`, {
      method: "DELETE",
    });
  };

  return {
    findAttributes,
    searchAttributes,
    findAttribute,
    createAttribute,
    updateAttribute,
    deleteAttribute,
  };
};
