// There is no user_id in this flow yet. Harbour Cloud scopes idempotency on
// Store-Id + Idempotency-Key, so a unique key per payment is enough for safe retries.
export function generateIdempotencyKey() {
  if (globalThis.crypto?.randomUUID) {
    return globalThis.crypto.randomUUID()
  }
  return `key-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

export function describeIdempotencyStatus(httpStatus) {
  if (httpStatus === 201) {
    return {
      idempotentReplay: false,
      label: '201 Created — new payment',
      message: 'A new payment was registered.',
    }
  }

  if (httpStatus === 200) {
    return {
      idempotentReplay: true,
      label: '200 OK — idempotent replay',
      message: 'Same Store-Id + Idempotency-Key was already processed. Original payment returned.',
    }
  }

  return {
    idempotentReplay: false,
    label: httpStatus ? `HTTP ${httpStatus}` : 'No status',
    message: '',
  }
}
