export class ImportBatch {
  constructor({ fileName, entries }) {
    this.fileName = fileName
    this.entries = entries
  }

  get validEntries() {
    return this.entries.filter((entry) => entry.isValid)
  }

  get invalidEntries() {
    return this.entries.filter((entry) => !entry.isValid)
  }

  get validPayments() {
    return this.validEntries.map((entry) => entry.payment)
  }
}
