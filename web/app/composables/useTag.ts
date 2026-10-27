import type { Tag, TagCommand, TagSearchParams } from "~/types/tag";

export const useTag = () => {
  const api = useApi();
  return {
    find: (query: TagSearchParams = {}) => api<Tag[]>("/tags", { query }),
    findById: (id: string) => api<Tag>(`/tags/${id}`),
    create: (body: TagCommand) => api<Tag>("/tags", { method: "POST", body }),
    update: (id: string, body: TagCommand) => api<Tag>(`/tags/${id}`, { method: "PUT", body }),
    remove: (id: string) => api<void>(`/tags/${id}`, { method: "DELETE" }),
  };
};
