import { ImportBatch } from '../../domain/import/ImportBatch.js'
import { NotebookEntry } from '../../domain/import/NotebookEntry.js'
import { Payment } from '../../domain/payment/Payment.js'
import { ImportFormat } from '../../shared/importFormats.js'
import { ValidationError } from '../../domain/errors/ValidationError.js'

function buildImportBatch(fileName, rows, mapRecordToPayment, validatePayment) {
  const entries = rows.map((row) => {
    const paymentData = mapRecordToPayment(row)
    const validation = validatePayment(paymentData)
    const payment = validation.valid ? new Payment(paymentData) : null

    return new NotebookEntry({
      rowIndex: row.rowIndex,
      raw: row.raw,
      payment,
      validation,
    })
  })

  return new ImportBatch({
    fileName,
    entries,
  })
}

export async function parseImportUseCase(
  file,
  format,
  { readCsvFile, readJsonFile, mapRecordToPayment, validatePayment },
) {
  let rows

  if (format === ImportFormat.JSON) {
    rows = await readJsonFile(file)
  } else if (format === ImportFormat.CSV) {
    rows = await readCsvFile(file)
  } else {
    throw new ValidationError(`Unsupported import format: ${format}`)
  }

  return buildImportBatch(file.name, rows, mapRecordToPayment, validatePayment)
}
