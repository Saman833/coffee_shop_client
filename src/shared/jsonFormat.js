export function formatJson(value) {
  if (value === null || value === undefined) {
    return ''
  }
  return JSON.stringify(value, null, 2)
}
