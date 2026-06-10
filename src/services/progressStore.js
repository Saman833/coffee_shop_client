const STORAGE_KEY = 'coffee-shop-sent-payments'

function readStore() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : {}
  } catch {
    return {}
  }
}

function writeStore(store) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(store))
}

export function makeProgressKey(storeId, idempotencyKey) {
  return `${storeId}::${idempotencyKey}`
}

export function getSentPayment(storeId, idempotencyKey) {
  const store = readStore()
  return store[makeProgressKey(storeId, idempotencyKey)] ?? null
}

export function markPaymentSent(storeId, idempotencyKey, result) {
  const store = readStore()
  store[makeProgressKey(storeId, idempotencyKey)] = {
    paymentId: result.paymentId,
    registeredAt: result.registeredAt,
    sentAt: new Date().toISOString(),
  }
  writeStore(store)
}

export function clearProgressStore() {
  localStorage.removeItem(STORAGE_KEY)
}
