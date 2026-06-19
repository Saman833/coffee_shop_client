import { useCallback, useState } from 'react'
import { ImportFormat } from '../../shared/importFormats.js'
import { parseImport } from '../di/container.js'

export function useImport() {
  const [batch, setBatch] = useState(null)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)
  const [selectedRowIndex, setSelectedRowIndex] = useState(null)

  const importFile = useCallback(async (file) => {
    if (!file) {
      return
    }

    setLoading(true)
    setError(null)
    setSelectedRowIndex(null)

    try {
      const result = await parseImport(file, ImportFormat.CSV)
      setBatch(result)

      const firstValid = result.validEntries[0]
      if (firstValid) {
        setSelectedRowIndex(firstValid.rowIndex)
      }
    } catch (err) {
      setBatch(null)
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [])

  const reset = useCallback(() => {
    setBatch(null)
    setError(null)
    setSelectedRowIndex(null)
  }, [])

  return {
    batch,
    error,
    loading,
    selectedRowIndex,
    setSelectedRowIndex,
    importFile,
    reset,
  }
}
