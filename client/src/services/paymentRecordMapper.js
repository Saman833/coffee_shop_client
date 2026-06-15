function toTrimmedString(value) {
  if (value === undefined || value === null) {
    return ''
  }
  return String(value).trim()
}

export function mapRecordToPayment(row) {
  return {
    storeId: toTrimmedString(row.raw.storeId),
    idempotencyKey: toTrimmedString(row.raw.idempotencyKey),
    coffeeType: toTrimmedString(row.raw.coffeeType),
    price: toTrimmedString(row.raw.price),
    currency: toTrimmedString(row.raw.currency),
    loyaltyCardId: toTrimmedString(row.raw.loyaltyCardId),
    rowIndex: row.rowIndex,
  }
}
