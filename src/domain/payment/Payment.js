export class Payment {
  constructor({ storeId, idempotencyKey, coffeeType, price, currency, loyaltyCardId, rowIndex }) {
    this.storeId = storeId
    this.idempotencyKey = idempotencyKey
    this.coffeeType = coffeeType
    this.price = price
    this.currency = currency
    this.loyaltyCardId = loyaltyCardId
    this.rowIndex = rowIndex
  }

  toApiBody() {
    return {
      coffeeType: this.coffeeType,
      price: Number(this.price),
      currency: this.currency,
      loyaltyCardId: this.loyaltyCardId,
    }
  }
}
