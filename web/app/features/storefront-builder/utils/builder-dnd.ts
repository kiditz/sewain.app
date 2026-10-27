import type { InjectionKey } from "vue";

/**
 * Kontrak antara PageBuilder dan renderer preview untuk drag & drop langsung
 * di canvas. PageBuilder menyediakan (provide) implementasinya, renderer
 * (StorefrontRenderer / BlockRenderer) memakainya (inject) hanya di mode
 * preview. Di storefront publik tidak ada provider, jadi drag otomatis mati.
 *
 * Reorder dilakukan berdasarkan id, bukan index, karena renderer hanya
 * menampilkan item yang aktif (enabled) sehingga index-nya tidak sama
 * dengan index di data.
 */
export interface BuilderDnd {
  /** Urutan baru id section yang tampil di preview. */
  reorderSections(orderedIds: string[]): void;
  /** Urutan baru id block (dalam satu section) yang tampil di satu list. */
  reorderBlocks(orderedIds: string[]): void;
}

export const builderDndKey: InjectionKey<BuilderDnd> = Symbol(
  "storefront-builder-dnd",
);

/**
 * Menyusun ulang `orderedIds` ke slot yang ditempatinya di `list`, sementara
 * item lain (misalnya yang disembunyikan) tetap di posisinya semula.
 * Mengembalikan `null` jika id tidak cocok dengan isi list.
 */
export function applyOrder<T extends { id: string }>(
  list: T[],
  orderedIds: string[],
): T[] | null {
  const byId = new Map(list.map((item) => [item.id, item]));

  if (
    orderedIds.length === 0 ||
    new Set(orderedIds).size !== orderedIds.length ||
    orderedIds.some((id) => !byId.has(id))
  ) {
    return null;
  }

  const slots = list
    .map((item, index) => (orderedIds.includes(item.id) ? index : -1))
    .filter((index) => index >= 0);

  const next = [...list];

  slots.forEach((slot, position) => {
    next[slot] = byId.get(orderedIds[position]!)!;
  });

  return next;
}
