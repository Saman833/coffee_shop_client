import { ValidationError } from '../domain/errors/ValidationError.js'

const REQUIRED_FIELDS = [
  'storeId',
  'idempotencyKey',
  'coffeeType',
  'price',
  'currency',
  'loyaltyCardId',
]

function extractPayments(parsed) {
  if (Array.isArray(parsed)) {
    return parsed
  }

  if (parsed && Array.isArray(parsed.payments)) {
    return parsed.payments
  }

  throw new ValidationError('JSON must be an array of payments or { "payments": [...] }')
}

export function parseJsonText(text) {
  let parsed

  try {
    parsed = JSON.parse(text)
  } catch {
    throw new ValidationError('Invalid JSON file')
  }

  const records = extractPayments(parsed)

  if (records.length === 0) {
    throw new ValidationError('JSON file contains no payments')
  }

  return records.map((record, index) => {
    if (!record || typeof record !== 'object' || Array.isArray(record)) {
      throw new ValidationError(`Payment at index ${index} must be an object`)
    }

    const missingFields = REQUIRED_FIELDS.filter((field) => record[field] === undefined)
    if (missingFields.length > 0) {
      throw new ValidationError(
        `Payment at index ${index} is missing fields: ${missingFields.join(', ')}`,
      )
    }

    return {
      rowIndex: index + 1,
      raw: record,
    }
  })
}

export async function readJsonFile(file) {
  const text = await file.text()
  return parseJsonText(text)
}
