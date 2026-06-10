import { useCallback, useState } from 'react'
import { RowStatus } from '../../domain/import/ImportStatus.js'
import { propagatePayments } from '../di/container.js'

export function usePaymentPropagation() {
  const [rowStatuses, setRowStatuses] = useState({})
  const [exchanges, setExchanges] = useState({})
  const [result, setResult] = useState(null)
  const [propagating, setPropagating] = useState(false)
  const [error, setError] = useState(null)

  const reset = useCallback(() => {
    setRowStatuses({})
    setExchanges({})
    setResult(null)
    setError(null)
  }, [])

  const propagate = useCallback(async (payments, requestTexts = {}) => {
    setPropagating(true)
    setError(null)
    setResult(null)
    setRowStatuses({})
    setExchanges({})

    try {
      const propagationResult = await propagatePayments(payments, {
        requestTexts,
        onProgress: (rowIndex, status, detail) => {
          setRowStatuses((current) => ({
            ...current,
            [rowIndex]: { status, detail },
          }))
        },
        onExchange: (rowIndex, exchange) => {
          setExchanges((current) => ({
            ...current,
            [rowIndex]: exchange,
          }))
        },
      })
      setResult(propagationResult)
    } catch (err) {
      setError(err.message)
    } finally {
      setPropagating(false)
    }
  }, [])

  const completedCount = Object.values(rowStatuses).filter(
    (row) =>
      row.status === RowStatus.SUCCEEDED ||
      row.status === RowStatus.FAILED ||
      row.status === RowStatus.SKIPPED,
  ).length

  return {
    rowStatuses,
    exchanges,
    result,
    propagating,
    error,
    completedCount,
    propagate,
    reset,
  }
}
