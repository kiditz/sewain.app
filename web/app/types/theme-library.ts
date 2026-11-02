import type { StorefrontPage, StorefrontTheme } from './storefront';
export interface ThemeDocument { theme: StorefrontTheme; pages: StorefrontPage[] }
export interface InstalledTheme {
  id: string; name: string; source: string; version: string; schemaVersion: number;
  homeSlug: string;
  active: boolean; hasChanges: boolean; revision: number;
  colors: StorefrontTheme['colors'];
  history: { revision: number; createdAt: string }[];
}
