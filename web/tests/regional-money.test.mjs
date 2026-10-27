import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { stripTypeScriptTypes } from 'node:module';

const source = stripTypeScriptTypes(
  readFileSync(new URL('../app/composables/useSettings.ts', import.meta.url), 'utf8'),
);
// Supply Nuxt's auto-imports without starting a Nuxt application.
const { useSettings } = await import(`data:text/javascript;base64,${Buffer.from(`
const useApi = () => () => {};
const useState = (_key, initialize) => ({ value: initialize() });
${source}
`).toString('base64')}`);

for (const locale of ['id', 'en']) {
  for (const currencyPosition of ['before', 'after']) {
    test(`money round-trips using ${locale} with currency ${currencyPosition}`, () => {
      const { regional, formatMoney, parseMoney } = useSettings();
      Object.assign(regional.value, { locale, currencyPosition, currency: 'USD' });
      for (const amount of [0, 1234, 1234.5, 1234.56, -1234.5]) {
        assert.equal(parseMoney(formatMoney(amount)), amount);
      }
    });
  }
}

test('parsing follows changes to regional locale', () => {
  const { regional, parseMoney } = useSettings();
  regional.value.locale = 'id';
  assert.equal(parseMoney('Rp 1.234,50'), 1234.5);
  regional.value.locale = 'en';
  assert.equal(parseMoney('$1,234.50'), 1234.5);
});

test('empty and invalid amounts are rejected', () => {
  const { parseMoney } = useSettings();
  for (const input of [null, undefined, '', ' ', 'Rp', 'abc', '--12', '1,2,3']) {
    assert.equal(parseMoney(input), null);
  }
});
