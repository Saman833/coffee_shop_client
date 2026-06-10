import { getPaymentsUrl } from './harbourApiConfig.js'

export function buildPaymentRequest(payment) {
  return {
    method: 'POST',
    url: getPaymentsUrl(),
    headers: {
      'Content-Type': 'application/json',
      'Store-Id': payment.storeId,
      'Idempotency-Key': payment.idempotencyKey,
    },
    body: payment.toApiBody(),
  }
}
