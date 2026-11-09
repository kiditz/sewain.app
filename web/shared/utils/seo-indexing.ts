/** Private workflows and theme previews must not appear in search results. */
export function shouldNoindex(path: string, queryKeys: Iterable<string>): boolean {
  const privateRoots = ["/merchant", "/profile", "/setting", "/cart", "/checkout", "/login", "/register"];
  const normalized = path.toLowerCase();
  if (privateRoots.some(root => normalized === root || normalized.startsWith(`${root}/`))) return true;
  for (const key of queryKeys) {
    if (["preview", "themeid"].includes(key.toLowerCase())) return true;
  }
  return false;
}
