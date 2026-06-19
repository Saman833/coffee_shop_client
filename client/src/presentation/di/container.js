import { parseImportUseCase } from '../../application/useCases/parseImportUseCase.js'
import { submitPaymentBatchUseCase } from '../../application/useCases/submitPaymentBatchUseCase.js'
import { validatePayment } from '../../domain/payment/PaymentValidator.js'
import { getPaymentBatchStatus, submitPaymentBatch } from '../../services/batchApi.js'
import { readCsvFile } from '../../services/csvReader.js'
import { readJsonFile } from '../../services/jsonReader.js'
import { mapRecordToPayment } from '../../services/paymentRecordMapper.js'

export function parseImport(file, format) {
  return parseImportUseCase(file, format, {
    readCsvFile,
    readJsonFile,
    mapRecordToPayment,
    validatePayment,
  })
}

export function propagatePayments(storeId, payments, { onProgress, onBatchStatus } = {}) {
  return submitPaymentBatchUseCase(storeId, payments, {
    submitBatch: submitPaymentBatch,
    getBatchStatus: getPaymentBatchStatus,
    onProgress,
    onBatchStatus,
  })
}

export { buildPaymentRequest } from '../../services/paymentRequestBuilder.js'
export { clearProgressStore } from '../../services/progressStore.js'
