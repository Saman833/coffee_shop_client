import { RowStatus } from '../../domain/import/ImportStatus.js'
import { buildPaymentRequest } from '../../services/paymentRequestBuilder.js'
import { parsePaymentRequestText } from '../../services/paymentRequestParser.js'

export async function propagatePaymentsUseCase(
  payments,
  { registerPaymentRequest, progressStore, onProgress, onExchange, requestTexts = {} },
) {
  const results = {
    succeeded: 0,
    failed: 0,
    skipped: 0,
    replayed: 0,
    rows: [],
  }

  for (const payment of payments) {
    onProgress?.(payment.rowIndex, RowStatus.SENDING)

    let request
    try {
      const editedText = requestTexts[payment.rowIndex]
      request = editedText ? parsePaymentRequestText(editedText) : buildPaymentRequest(payment)
    } catch (error) {
      results.failed += 1
      const exchange = {
        request: buildPaymentRequest(payment),
        response: {
          status: null,
          body: { error: error.message },
        },
      }
      results.rows.push({
        rowIndex: payment.rowIndex,
        status: RowStatus.FAILED,
        message: error.message,
        exchange,
      })
      onExchange?.(payment.rowIndex, exchange)
      onProgress?.(payment.rowIndex, RowStatus.FAILED, error.message)
      continue
    }

    const storeId = request.headers['Store-Id']
    const idempotencyKey = request.headers['Idempotency-Key']

    try {
      const response = await registerPaymentRequest(request)
      progressStore.markPaymentSent(storeId, idempotencyKey, response)

      if (response.idempotentReplay) {
        results.replayed += 1
      } else {
        results.succeeded += 1
      }

      const detail = response.idempotentReplay
        ? 'Idempotent replay (200)'
        : 'Created (201)'

      results.rows.push({
        rowIndex: payment.rowIndex,
        status: RowStatus.SUCCEEDED,
        paymentId: response.paymentId,
        httpStatus: response.status,
        idempotentReplay: response.idempotentReplay,
        exchange: response.exchange,
      })
      onExchange?.(payment.rowIndex, response.exchange)
      onProgress?.(payment.rowIndex, RowStatus.SUCCEEDED, detail)
    } catch (error) {
      results.failed += 1
      results.rows.push({
        rowIndex: payment.rowIndex,
        status: RowStatus.FAILED,
        message: error.message,
        exchange: error.exchange ?? {
          request,
          response: {
            status: null,
            body: { error: error.message },
          },
        },
      })
      onExchange?.(payment.rowIndex, error.exchange)
      onProgress?.(payment.rowIndex, RowStatus.FAILED, error.message)
    }
  }

  return results
}
