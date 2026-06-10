export class NotebookEntry {
  constructor({ rowIndex, raw, payment, validation }) {
    this.rowIndex = rowIndex
    this.raw = raw
    this.payment = payment
    this.validation = validation
  }

  get isValid() {
    return this.validation.valid
  }
}
