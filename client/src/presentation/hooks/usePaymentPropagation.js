import { useCallback, useState } from 'react'
import { RowStatus } from '../../domain/import/ImportStatus.js'
import { propagatePayments } from '../di/container.js'

export function usePaymentPropagation() {
  const [rowStatuses, setRowStatuses] = useState({})
  const [batchStatus, setBatchStatus] = useState(null)
  const [result, setResult] = useState(null)
  const [propagating, setPropagating] = useState(false)
  const [error, setError] = useState(null)

  const reset = useCallback(() => {
    setRowStatuses({})
    setBatchStatus(null)
    setResult(null)
    setError(null)
  }, [])

  const propagate = useCallback(async (storeId, payments) => {
    setPropagating(true)
    setError(null)
    setResult(null)
    setRowStatuses({})
    setBatchStatus(null)

    try {
      const propagationResult = await propagatePayments(storeId, payments, {
        onProgress: (rowIndex, status, detail) => {
          setRowStatuses((current) => ({
            ...current,
            [rowIndex]: { status, detail },
          }))
        },
        onBatchStatus: (status) => {
          setBatchStatus(status)
        },
      })
      setResult(propagationResult)
    } catch (err) {
      setError(err.message)
    } finally {
      setPropagating(false)
    }
  }, [])

  const completedCount = batchStatus?.processed ?? Object.values(rowStatuses).filter(
    (row) =>
      row.status === RowStatus.SUCCEEDED ||
      row.status === RowStatus.FAILED ||
      row.status === RowStatus.SKIPPED,
  ).length

  return {
    rowStatuses,
    batchStatus,
    result,
    propagating,
    error,
    completedCount,
    propagate,
    reset,
  }
}
