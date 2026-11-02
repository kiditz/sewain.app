import type { StorefrontPage } from '~/types/storefront';
/** Navigation and Footer are shared inside a theme; page content stays independent. */
export function synchronizeThemeGlobals(pages: StorefrontPage[], source?: StorefrontPage) {
  if (!source) return;
  const navigation = source.sections.filter(section => section.type === 'NAVIGATION');
  const footer = source.sections.filter(section => section.type === 'FOOTER');
  for (const page of pages) {
    if (page === source) continue;
    page.sections = [
      ...JSON.parse(JSON.stringify(navigation)),
      ...page.sections.filter(section => !['NAVIGATION', 'FOOTER'].includes(section.type)),
      ...JSON.parse(JSON.stringify(footer)),
    ];
  }
}
