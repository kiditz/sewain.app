import type { Media } from "~/types/media";
import type { PageResponse } from "~/types/pagination";

export const useMedia = () => {
  const api = useApi();
  const uploadMedia = async (files: File[]) => {
    const formData = new FormData();
    files.forEach((file) => {
      formData.append("files", file);
    });
    return api<Media[]>("/media", {
      method: "POST",
      body: formData,
    });
  };

  const getMedia = async (query: Record<string, unknown>) => {
    return api<PageResponse<Media>>("/media", {
      query: query,
    });
  };

  const updateMediaFilename = async (id: string, filename: string) => {
    return api<Media>(`/media/${id}`, {
      method: "PATCH",
      body: { filename },
    });
  };

  const deleteMedia = async (id: string) => {
    return api<void>(`/media/${id}`, { method: "DELETE" });
  };

  return { uploadMedia, getMedia, updateMediaFilename, deleteMedia };
};
