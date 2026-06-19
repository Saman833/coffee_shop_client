import { ValidationError } from '../domain/errors/ValidationError.js'
import { formatJson } from '../shared/jsonFormat.js'

export function parsePaymentRequestText(text) {
  let parsed

  try {
    parsed = JSON.parse(text)
  } catch {
    throw new ValidationError('Request JSON is invalid')
  }

  if (!parsed?.headers?.['Store-Id']?.trim()) {
    throw new ValidationError('Request must include headers.Store-Id')
  }

  if (!parsed?.headers?.['Idempotency-Key']?.trim()) {
    throw new ValidationError('Request must include headers.Idempotency-Key')
  }

  if (!parsed?.body || typeof parsed.body !== 'object') {
    throw new ValidationError('Request must include a body object')
  }

  return parsed
}

export function getIdempotencyKeyFromRequestText(text) {
  try {
    const parsed = JSON.parse(text)
    return parsed?.headers?.['Idempotency-Key']?.trim() ?? ''
  } catch {
    return ''
  }
}

export function updateRequestIdempotencyKey(text, idempotencyKey) {
  const parsed = parsePaymentRequestText(text)
  parsed.headers['Idempotency-Key'] = idempotencyKey
  return formatJson(parsed)
}
