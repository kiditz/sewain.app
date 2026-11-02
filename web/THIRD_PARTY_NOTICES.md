# Background removal

The optional background-removal feature uses:

- [Transformers.js 3.8.1](https://github.com/huggingface/transformers.js/tree/3.8.1), licensed under [Apache License 2.0](https://github.com/huggingface/transformers.js/blob/3.8.1/LICENSE).
- [ORMBG ONNX model](https://huggingface.co/onnx-community/ormbg-ONNX/tree/034e2d884afbab897e10e78fc5bb566b29533fd6), declared Apache-2.0 in its model card ([license text](https://www.apache.org/licenses/LICENSE-2.0)). The browser downloads the quantized model at the pinned revision on first use and can cache it for subsequent use.

The model runs locally in a browser worker. Photos are uploaded to the existing Sewain media endpoint only when the user chooses Upload.
