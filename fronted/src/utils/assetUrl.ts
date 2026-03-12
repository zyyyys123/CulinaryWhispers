export function normalizeAssetUrl(url?: string | null): string | undefined {
  if (!url) return undefined
  const u = String(url).trim()
  if (!u) return undefined
  if (
    u.startsWith('http://') ||
    u.startsWith('https://') ||
    u.startsWith('data:') ||
    u.startsWith('blob:')
  ) {
    return u
  }
  if (u.startsWith('api/')) return `/${u}`
  if (u.startsWith('/api/')) return u
  if (u.startsWith('/uploads/')) return `/api${u}`
  if (u.startsWith('uploads/')) return `/api/${u}`
  return u
}
