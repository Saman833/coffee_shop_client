import { getApiBaseUrl } from './harbourApiConfig.js'

function batchesUrl(path = '') {
  const base = getApiBaseUrl()
  return `${base}/api/v1/payment-batches${path}`
}

async function parseError(response) {
  const contentType = response.headers.get('content-type') ?? ''
  if (contentType.includes('application/json') || contentType.includes('problem+json')) {
    const body = await response.json()
    return body.detail || body.title || response.statusText
  }
  return response.statusText
}

export async function submitPaymentBatch(storeId, payments) {
  const response = await fetch(batchesUrl(), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      storeId,
      payments: payments.map((payment) => ({
        idempotencyKey: payment.idempotencyKey,
        coffeeType: payment.coffeeType,
        price: Number(payment.price),
        currency: payment.currency,
        loyaltyCardId: payment.loyaltyCardId,
      })),
    }),
  })

  if (!response.ok) {
    throw new Error(await parseError(response))
  }

  return response.json()
}

export async function getPaymentBatchStatus(requestId) {
  const response = await fetch(batchesUrl(`/${requestId}`))

  if (response.status === 404) {
    throw new Error(`Batch request ${requestId} was not found`)
  }

  if (!response.ok) {
    throw new Error(await parseError(response))
  }

  return response.json()
}
