export const ImportFormat = {
  CSV: 'csv',
  JSON: 'json',
}

export function getAcceptTypes(format) {
  if (format === ImportFormat.JSON) {
    return '.json,application/json'
  }
  return '.csv,text/csv'
}
