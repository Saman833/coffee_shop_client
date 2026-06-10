import { parseImportUseCase } from '../../application/useCases/parseImportUseCase.js'
import { propagatePaymentsUseCase } from '../../application/useCases/propagatePaymentsUseCase.js'
import { validatePayment } from '../../domain/payment/PaymentValidator.js'
import { readCsvFile } from '../../services/csvReader.js'
import { readJsonFile } from '../../services/jsonReader.js'
import { mapRecordToPayment } from '../../services/paymentRecordMapper.js'
import { registerPaymentRequest } from '../../services/harbourApi.js'
import * as progressStore from '../../services/progressStore.js'

export function parseImport(file, format) {
  return parseImportUseCase(file, format, {
    readCsvFile,
    readJsonFile,
    mapRecordToPayment,
    validatePayment,
  })
}

export function propagatePayments(payments, { onProgress, onExchange, requestTexts } = {}) {
  return propagatePaymentsUseCase(payments, {
    registerPaymentRequest,
    progressStore,
    onProgress,
    onExchange,
    requestTexts,
  })
}

export { buildPaymentRequest } from '../../services/paymentRequestBuilder.js'

export { clearProgressStore } from '../../services/progressStore.js'
