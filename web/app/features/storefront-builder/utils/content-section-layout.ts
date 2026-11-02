import type { SectionLayout, StorefrontPadding } from '~/types/storefront';
import { objectSettings } from './product-information-layout';

export function contentSectionDefaults(alignment: SectionLayout['alignment'] = 'left') {
  return {
    layout: { direction: 'vertical', alignment, position: 'top', gap: 16, width: 'page', height: 'auto', customHeight: 0, alignTextBaseline: false } as SectionLayout,
    padding: { top: null, right: 24, bottom: null, left: 24 } as StorefrontPadding,
  };
}

export function contentSectionSettings(settings: Record<string, unknown>, alignment: SectionLayout['alignment'] = 'left', spacing?: number) {
  const defaults = contentSectionDefaults(alignment);
  const { alignment: legacyAlignment, ...rest } = settings;
  const padding = { ...defaults.padding, ...objectSettings(settings.padding) } as StorefrontPadding;
  if (spacing != null) { padding.top ??= spacing; padding.bottom ??= spacing; }
  return {
    ...rest,
    layout: { ...defaults.layout, ...(['left', 'center', 'right'].includes(String(legacyAlignment)) ? { alignment: legacyAlignment } : {}), ...objectSettings(settings.layout) } as SectionLayout,
    padding,
  };
}
