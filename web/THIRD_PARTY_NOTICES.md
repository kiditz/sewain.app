# Background removal

The optional background-removal feature uses:

- [Transformers.js 3.8.1](https://github.com/huggingface/transformers.js/tree/3.8.1), licensed under [Apache License 2.0](https://github.com/huggingface/transformers.js/blob/3.8.1/LICENSE).
- [ORMBG ONNX model](https://huggingface.co/onnx-community/ormbg-ONNX/tree/c491647), licensed under [Apache License 2.0](https://huggingface.co/onnx-community/ormbg-ONNX/blob/main/LICENSE). The browser downloads the quantized model at the pinned revision on first use and can cache it for subsequent use.

The model runs locally in a browser worker. Photos are uploaded to the existing Sewain media endpoint only when the user chooses Upload.
