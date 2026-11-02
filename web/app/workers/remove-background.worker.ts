import { env, pipeline, RawImage } from "@huggingface/transformers";

env.allowLocalModels = false;
// Run inference off the UI thread without requiring cross-origin isolation.
env.backends.onnx.wasm!.numThreads = 1;
let segmenter: ReturnType<typeof pipeline<"background-removal">> | undefined;

self.addEventListener("message", async (event: MessageEvent<{ id: string; file: File }>) => {
  const { id, file } = event.data;
  try {
    const image = await RawImage.fromBlob(file);
    if (image.width * image.height > 16_000_000) throw new Error("IMAGE_TOO_LARGE");
    self.postMessage({ id, phase: "loading" });
    segmenter ??= pipeline<"background-removal">("background-removal", "onnx-community/ormbg-ONNX", {
      revision: "034e2d884afbab897e10e78fc5bb566b29533fd6",
      dtype: "q8",
      device: "wasm",
    });
    const removeBackground = await segmenter;
    self.postMessage({ id, phase: "processing" });
    const [result] = await removeBackground(image);
    if (!result) throw new Error("PROCESSING_FAILED");
    const blob: Blob = await result.toBlob("image/png");
    self.postMessage({ id, blob });
  } catch (error) {
    segmenter = undefined;
    self.postMessage({ id, error: error instanceof Error && error.message === "IMAGE_TOO_LARGE" ? "IMAGE_TOO_LARGE" : "PROCESSING_FAILED", detail: error instanceof Error ? error.message : String(error) });
  }
});
