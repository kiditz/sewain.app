export function useBackgroundRemoval() {
  const processing = ref(false);
  const phase = ref<"idle" | "loading" | "processing">("idle");
  let worker: Worker | undefined;
  let requestId = 0;
  let pending: { id: string; resolve: (blob: Blob) => void; reject: (error: Error) => void } | undefined;

  function cancel() {
    worker?.terminate();
    worker = undefined;
    pending?.reject(new DOMException("Cancelled", "AbortError"));
    pending = undefined;
    processing.value = false;
    phase.value = "idle";
  }

  function getWorker() {
    if (worker) return worker;
    worker = new Worker(new URL("../workers/remove-background.worker.ts", import.meta.url), { type: "module" });
    worker.onmessage = (event: MessageEvent<{ id: string; phase?: "loading" | "processing"; blob?: Blob; error?: string; detail?: string }>) => {
      if (!pending || event.data.id !== pending.id) return;
      if (event.data.phase) { phase.value = event.data.phase; return; }
      const task = pending;
      pending = undefined;
      if (event.data.blob) task.resolve(event.data.blob);
      else {
        if (import.meta.dev && event.data.detail) console.error("Background removal failed:", event.data.detail);
        worker?.terminate();
        worker = undefined;
        task.reject(new Error(event.data.error || "PROCESSING_FAILED", { cause: event.data.detail }));
      }
    };
    worker.onerror = (event) => {
      if (import.meta.dev) console.error("Background removal worker failed:", event.message);
      const task = pending;
      pending = undefined;
      worker?.terminate();
      worker = undefined;
      task?.reject(new Error("PROCESSING_FAILED"));
    };
    return worker;
  }

  async function removeBackground(file: File) {
    if (processing.value) throw new Error("PROCESSING_BUSY");
    if (!["image/png", "image/jpeg", "image/webp"].includes(file.type)) throw new Error("UNSUPPORTED_IMAGE");
    processing.value = true;
    phase.value = "loading";
    try {
      const engine = getWorker();
      const blob = await new Promise<Blob>((resolve, reject) => {
        const id = String(++requestId);
        pending = { id, resolve, reject };
        engine.postMessage({ id, file });
      });
      return new File([blob], `${file.name.replace(/\.[^.]+$/, "")}-transparent.png`, { type: "image/png" });
    } finally {
      processing.value = false;
      phase.value = "idle";
    }
  }

  onBeforeUnmount(cancel);
  return { processing, phase, removeBackground, cancel };
}
