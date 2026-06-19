import { useCallback, useEffect, useState } from 'react'
import { listPayments } from '../../services/harbourApi.js'

const DEFAULT_STORE_ID = 'store-london-01'

export function useBackendPayments() {
  const [storeId, setStoreId] = useState(DEFAULT_STORE_ID)
  const [payments, setPayments] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const refresh = useCallback(async (id) => {
    const targetId = (id ?? storeId).trim()
    if (!targetId) {
      setError('Store ID is required')
      setPayments([])
      return
    }

    setLoading(true)
    setError(null)

    try {
      const data = await listPayments(targetId)
      setPayments(data)
    } catch (err) {
      setPayments([])
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [storeId])

  useEffect(() => {
    let cancelled = false

    async function loadInitialPayments() {
      setLoading(true)
      setError(null)

      try {
        const data = await listPayments(DEFAULT_STORE_ID)
        if (!cancelled) {
          setPayments(data)
        }
      } catch (err) {
        if (!cancelled) {
          setPayments([])
          setError(err.message)
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    loadInitialPayments()

    return () => {
      cancelled = true
    }
  }, [])

  return {
    storeId,
    setStoreId,
    payments,
    loading,
    error,
    refresh,
  }
}
