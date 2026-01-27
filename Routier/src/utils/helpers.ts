export function formatDate(date: Date | string): string {
  const d = typeof date === 'string' ? new Date(date) : date
  return d.toLocaleString()
}

export function isDefined<T>(value: T | null | undefined): value is T {
  return value !== null && value !== undefined
}
