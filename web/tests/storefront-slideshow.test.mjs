import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { stripTypeScriptTypes } from 'node:module';

const source = stripTypeScriptTypes(readFileSync(new URL('../app/features/storefront-builder/utils/slideshow.ts', import.meta.url), 'utf8'));
const { slideshowSettings, slideSettings, slideContentLayout, nextSlideId, slideContainingBlock } = await import(`data:text/javascript;base64,${Buffer.from(source).toString('base64')}`);
const slide = (id, enabled = true, blocks = []) => ({ id, type: 'SLIDE', enabled, settings: {}, blocks });

test('slide navigation follows reordered visible slides and wraps safely', () => {
  const first = slide('first');
  const second = slide('second');
  const hidden = slide('hidden', false);
  const visible = [second, hidden, first].filter(block => block.enabled);
  assert.equal(nextSlideId(visible, 'second'), 'first');
  assert.equal(nextSlideId(visible, 'first'), 'second');
  assert.equal(nextSlideId(visible, 'removed'), 'second');
  assert.equal(nextSlideId([first], 'first'), 'first');
  assert.equal(nextSlideId([], 'first'), null);
});

test('sidebar selection finds the owning slide through nested Header content', () => {
  const heading = { id: 'heading', type: 'HEADING', enabled: true, settings: {} };
  const second = slide('second', true, [{ id: 'header', type: 'HEADER', enabled: true, settings: {}, blocks: [heading] }]);
  const slides = [slide('first'), second];
  assert.equal(slideContainingBlock(slides, 'heading'), second);
  assert.equal(slideContainingBlock(slides, 'second'), second);
  assert.equal(slideContainingBlock(slides, 'missing'), undefined);
  assert.equal(slideContainingBlock(slides, null), undefined);
});

test('partial saved settings retain defaults and clamp autoplay interval', () => {
  const saved = { autoplay: true, interval: 1, layout: { width: 'page' }, padding: { left: 12 } };
  const normalized = slideshowSettings(saved);
  assert.equal(normalized.interval, 2);
  assert.equal(normalized.layout.height, 'medium');
  assert.equal(normalized.padding.left, 12);
  assert.equal(normalized.showArrows, true);
  assert.equal(slideshowSettings({ interval: 999 }).interval, 30);
  assert.equal(slideshowSettings({ interval: 'invalid' }).interval, 5);
  assert.equal(slideshowSettings({ autoplay: 'false', showArrows: false }).autoplay, false);
  const slide = slideSettings({ image: { src: '/photo.png' }, layout: { position: 'bottom' } });
  assert.equal(slide.image.alt, '');
  assert.equal(slide.layout.position, 'bottom');
  assert.equal(slide.padding.top, 48);
  assert.equal(slideContentLayout({ layout: { width: 'page', position: 'bottom' } }).width, 'full');
  normalized.padding.left = 99;
  assert.equal(saved.padding.left, 12);
});
