# Third-party notices

## Dashboard UI foundation

The merchant dashboard originally used **Mantis Free Vuetify Vue.js Admin Template** by **CodedThemes**, adapted for Nuxt and Sewain's merchant workflows. Its template-specific stylesheet tree and customizer store have been replaced by the Sewain style layer and dashboard navigation state. The existing visual direction is retained. This notice remains for historical provenance and inherited/derived work; the refactor is not presented as a clean-room redesign.

- [Upstream project](https://github.com/codedthemes/mantis-free-vuetify-vuejs-admin-template)
- [Upstream MIT license](https://github.com/codedthemes/mantis-free-vuetify-vuejs-admin-template/blob/master/LICENSE)
- [Bundled license notice](licenses/MANTIS-MIT.txt)

This attribution records the original template foundation. Sewain's rental domain, tenant architecture, feature workflows, integrations and tests are described separately in the root README.

## Other bundled components

- The WhatsApp gateway includes the [WhatsApp REST API wrapper](https://github.com/chrishubert/whatsapp-api); its [license notice](../whatsapp-api/LICENSE.md) is retained with the bundled source.
- Payment brand artwork and icon sources are documented in [payment SVG sources](public/images/payments/SOURCES.md). Brand artwork belongs to its respective owners.

## Background removal

The optional background-removal feature uses:

- [Transformers.js 3.8.1](https://github.com/huggingface/transformers.js/tree/3.8.1), licensed under [Apache License 2.0](https://github.com/huggingface/transformers.js/blob/3.8.1/LICENSE).
- [ORMBG ONNX model](https://huggingface.co/onnx-community/ormbg-ONNX/tree/034e2d884afbab897e10e78fc5bb566b29533fd6), declared Apache-2.0 in its model card ([license text](https://www.apache.org/licenses/LICENSE-2.0)). The browser downloads the quantized model at the pinned revision on first use and can cache it for subsequent use.

The model runs locally in a browser worker. Photos are uploaded to the existing Sewain media endpoint only when the user chooses Upload.
