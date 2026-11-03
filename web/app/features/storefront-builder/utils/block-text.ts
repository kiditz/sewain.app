export function escapedBlockText(value:unknown) {
  return String(value??'').replace(/[&<>"']/g,character=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'})[character]!);
}

export function copyrightBlockText(text:unknown,storeName:unknown,year:number) {
  return String(text||'© {year} {store}').replaceAll('{year}',String(year)).replaceAll('{store}',escapedBlockText(storeName));
}
export function canEmbedPage(id:string,ancestors:string[]) { return ancestors.length<5&&!ancestors.includes(id); }
