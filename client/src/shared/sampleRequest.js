import { formatJson } from './jsonFormat.js'

export const SAMPLE_REQUEST = {
  method: 'POST',
  url: '/api/v1/payments',
  headers: {
    'Content-Type': 'application/json',
    'Store-Id': 'store-london-01',
    'Idempotency-Key': 'order-001',
  },
  body: {
    coffeeType: 'LATTE',
    price: 3.5,
    currency: 'EUR',
    loyaltyCardId: 'card-999',
  },
}

export const SAMPLE_REQUEST_TEXT = formatJson(SAMPLE_REQUEST)

export const SAMPLE_REQUEST_KEY = 'sample'
