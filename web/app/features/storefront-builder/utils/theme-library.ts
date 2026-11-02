import type { ThemeDocument } from '~/types/theme-library';
import { applyStorefrontPreset, presetColors } from './storefront-theme';
import { createSection } from '../composables/useStorefrontPages';
export const themeCatalog = [
  { id: 'everyday', name: 'Everyday', colors: presetColors('everyday', 'light') },
  { id: 'studio', name: 'Studio', colors: presetColors('studio', 'light') },
  { id: 'sunrise', name: 'Sunrise', colors: presetColors('sunrise', 'light') },
];
export function createThemeDocument(base: ThemeDocument, name: string): ThemeDocument {
  const document: ThemeDocument = JSON.parse(JSON.stringify(base));
  applyStorefrontPreset(document.theme, name);
  document.theme.mode = 'light';
  document.theme.colors = presetColors(name, 'light');
  document.theme.typography.headingFont = name === 'Studio' ? 'Inter, Arial, sans-serif' : 'Georgia, serif';
  document.theme.layout.borderRadius = name === 'Studio' ? 0 : name === 'Sunrise' ? 24 : 16;
  const home = document.pages.find(page => page.kind === 'HOME');
  if (home) {
    const navigation = home.sections.filter(section => section.type === 'NAVIGATION');
    const footer = home.sections.filter(section => section.type === 'FOOTER');
    const sections = name === 'Studio' ? [createSection('SLIDESHOW'), createSection('FEATURED_PRODUCTS'), createSection('RICH_TEXT')]
      : name === 'Sunrise' ? [createSection('IMAGE_BANNER'), createSection('COLLECTION_LIST'), createSection('FEATURED_PRODUCTS')]
      : [createSection('HERO'), createSection('FEATURED_PRODUCTS'), createSection('COLLECTION_LIST')];
    home.sections = [...navigation, ...sections, ...footer];
  }
  return document;
}
