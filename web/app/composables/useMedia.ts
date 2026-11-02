import type { Media } from "~/types/media";
import type { PageResponse } from "~/types/pagination";

export const useMedia = () => {
  const api = useApi();
  const uploadMedia = async (files: File[], backgroundRemoved: number[] = []) => {
    const formData = new FormData();
    files.forEach((file) => {
      formData.append("files", file);
    });
    backgroundRemoved.forEach(index => formData.append("backgroundRemoved", String(index)));
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

  const getMediaFile = (id: string) => api<Blob, "blob">(`/media/${id}/file`, { responseType: "blob" });

  const replaceMediaFile = (id: string, file: File) => {
    const body = new FormData();
    body.append("file", file);
    return api<Media>(`/media/${id}/file`, { method: "PUT", body });
  };

  return { uploadMedia, getMedia, updateMediaFilename, deleteMedia, getMediaFile, replaceMediaFile };
};
