/** Offset-bearing timestamps represent instants; new offset-less backend timestamps are UTC. */
export function parseApiDate(value: string | Date | number): Date {
  if (value instanceof Date) return value;
  if (typeof value === "number") return new Date(value);
  const normalized = value.trim().replace(/^(\d{4}-\d{2}-\d{2}) /, "$1T");
  if (/^\d{4}-\d{2}-\d{2}$/.test(normalized)) return new Date(`${normalized}T00:00:00Z`);
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(?::\d{2}(?:\.\d+)?)?$/.test(normalized)) return new Date(`${normalized}Z`);
  return new Date(normalized);
}
