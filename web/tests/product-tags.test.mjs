import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { stripTypeScriptTypes } from 'node:module';

let source = stripTypeScriptTypes(readFileSync(new URL('../app/utils/product.schema.ts', import.meta.url), 'utf8'));
for (const dependency of ['zod', '@vee-validate/zod']) source = source.replace(`"${dependency}"`, JSON.stringify(import.meta.resolve(dependency)));
const { createProductSchema } = await import(`data:text/javascript;base64,${Buffer.from(source).toString('base64')}`);
const { productSchema, schema } = createProductSchema(key => key);
function product() {
  return { name: 'Tent', slug: 'tent', media: '', description: '', status: 'DRAFT', productTypeId: null,
    variants: [{ name: 'Default', sku: 'TENT', mediaList: [], buyPrice: null, depositAmount: 0, active: true,
      attributeValueIds: [], inventoryQuantity: 0, inventoryUnits: [], rentalPlans: [] }] };
}

test('selected tag IDs survive both product and vee-validate schema parsing', async () => {
  const tagIds = ['1703d19a-ced9-43a0-b5f8-c784dd149c50', '2c98f949-5f83-496f-b94d-e01bff4df3a2'];
  const input = { ...product(), tagIds };
  assert.deepEqual(productSchema.parse(input).tagIds, tagIds);
  const parsed = await schema.parse(input);
  assert.deepEqual(parsed.errors, []);
  assert.deepEqual(parsed.value.tagIds, tagIds);
});

test('tag selection can be cleared, defaults to empty, and rejects malformed IDs', () => {
  assert.deepEqual(productSchema.parse({ ...product(), tagIds: [] }).tagIds, []);
  assert.deepEqual(productSchema.parse(product()).tagIds, []);
  assert.equal(productSchema.safeParse({ ...product(), tagIds: ['not-a-uuid'] }).success, false);
});
