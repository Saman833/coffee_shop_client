import { RowStatus } from '../../domain/import/ImportStatus.js'

const TERMINAL_STATUSES = new Set(['DONE', 'FAILED', 'FAILED_PARTIAL'])

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

export async function submitPaymentBatchUseCase(
  storeId,
  payments,
  { submitBatch, getBatchStatus, onProgress, onBatchStatus, pollIntervalMs = 1000 },
) {
  for (const payment of payments) {
    onProgress?.(payment.rowIndex, RowStatus.SENDING)
  }

  const submission = await submitBatch(storeId, payments)
  onBatchStatus?.(submission)

  let status = submission
  while (!TERMINAL_STATUSES.has(status.status)) {
    await sleep(pollIntervalMs)
    status = await getBatchStatus(submission.requestId)
    onBatchStatus?.(status)
  }

  const succeeded = status.succeeded ?? 0
  const replayed = status.replayed ?? 0
  const failed = status.failed ?? 0

  for (const payment of payments) {
    if (failed === 0) {
      onProgress?.(payment.rowIndex, RowStatus.SUCCEEDED, status.status)
    } else if (succeeded + replayed === 0) {
      onProgress?.(payment.rowIndex, RowStatus.FAILED, status.status)
    } else {
      onProgress?.(payment.rowIndex, RowStatus.SUCCEEDED, `${status.status} (batch aggregate)`)
    }
  }

  return {
    requestId: status.requestId,
    status: status.status,
    total: status.total,
    processed: status.processed,
    succeeded,
    replayed,
    failed,
    rows: payments.map((payment) => ({
      rowIndex: payment.rowIndex,
      status: failed === 0 ? RowStatus.SUCCEEDED : RowStatus.FAILED,
    })),
  }
}
