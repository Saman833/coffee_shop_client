import { isCoffeeType } from './CoffeeType.js'

const CURRENCY_PATTERN = /^[A-Z]{3}$/
const PRICE_PATTERN = /^\d{1,10}(\.\d{1,2})?$/

export function validatePayment(payment) {
  const errors = []

  if (!payment.storeId?.trim()) {
    errors.push('storeId is required')
  }

  if (!payment.idempotencyKey?.trim()) {
    errors.push('idempotencyKey is required for reliable propagation')
  }

  if (!payment.coffeeType?.trim()) {
    errors.push('coffeeType is required')
  } else if (!isCoffeeType(payment.coffeeType)) {
    errors.push(`coffeeType must be one of: ${payment.coffeeType} is invalid`)
  }

  if (payment.price === undefined || payment.price === null || payment.price === '') {
    errors.push('price is required')
  } else {
    const priceNum = Number(payment.price)
    if (Number.isNaN(priceNum) || priceNum <= 0) {
      errors.push('price must be greater than 0')
    } else if (!PRICE_PATTERN.test(String(payment.price))) {
      errors.push('price must have at most 2 decimal places')
    }
  }

  if (!payment.currency?.trim()) {
    errors.push('currency is required')
  } else if (!CURRENCY_PATTERN.test(payment.currency)) {
    errors.push('currency must be a 3-letter ISO-4217 code, e.g. EUR')
  }

  if (payment.loyaltyCardId === undefined || payment.loyaltyCardId === null) {
    errors.push('loyaltyCardId is required')
  }

  return {
    valid: errors.length === 0,
    errors,
  }
}
