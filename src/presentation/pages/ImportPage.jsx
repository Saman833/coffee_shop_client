import { useEffect, useMemo, useState } from 'react'
import { BackendPaymentsPreview } from '../components/BackendPaymentsPreview.jsx'
import { ImportControls } from '../components/ImportControls.jsx'
import { ImportPreview } from '../components/ImportPreview.jsx'
import { PropagationProgress } from '../components/PropagationProgress.jsx'
import { RequestResponsePanel } from '../components/RequestResponsePanel.jsx'
import { formatJson } from '../../shared/jsonFormat.js'
import { describeIdempotencyStatus, generateIdempotencyKey } from '../../shared/idempotency.js'
import { SAMPLE_REQUEST_KEY, SAMPLE_REQUEST_TEXT } from '../../shared/sampleRequest.js'
import { ResultsSummary } from '../components/ResultsSummary.jsx'
import { useBackendPayments } from '../hooks/useBackendPayments.js'
import { useImport } from '../hooks/useImport.js'
import { usePaymentPropagation } from '../hooks/usePaymentPropagation.js'
import { buildPaymentRequest, clearProgressStore } from '../di/container.js'
import {
  getIdempotencyKeyFromRequestText,
  parsePaymentRequestText,
  updateRequestIdempotencyKey,
} from '../../services/paymentRequestParser.js'
import { registerPaymentRequest } from '../../services/harbourApi.js'
import * as progressStore from '../../services/progressStore.js'
import '../styles/import-page.css'

export function ImportPage() {
  const {
    batch,
    error: importError,
    loading,
    selectedRowIndex,
    setSelectedRowIndex,
    importFile,
    reset: resetImport,
  } = useImport()
  const {
    storeId,
    setStoreId,
    payments: backendPayments,
    loading: backendLoading,
    error: backendError,
    refresh: refreshBackendPayments,
  } = useBackendPayments()
  const {
    rowStatuses,
    exchanges,
    result,
    propagating,
    error: propagationError,
    completedCount,
    propagate,
    reset: resetPropagation,
  } = usePaymentPropagation()

  const [requestTexts, setRequestTexts] = useState({
    [SAMPLE_REQUEST_KEY]: SAMPLE_REQUEST_TEXT,
  })
  const [activeKey, setActiveKey] = useState(SAMPLE_REQUEST_KEY)
  const [responseTexts, setResponseTexts] = useState({})
  const [sendingSingle, setSendingSingle] = useState(false)
  const [singleError, setSingleError] = useState(null)
  const [idempotencyStatuses, setIdempotencyStatuses] = useState({})

  const validPayments = batch?.validPayments ?? []
  const canPropagate = batch && validPayments.length > 0 && !propagating && !loading

  const selectedExchange = selectedRowIndex ? exchanges[selectedRowIndex] : null

  const defaultRequestText = useMemo(() => {
    if (activeKey === SAMPLE_REQUEST_KEY) {
      return SAMPLE_REQUEST_TEXT
    }
    if (selectedExchange?.request && activeKey === selectedRowIndex) {
      return formatJson(selectedExchange.request)
    }
    const entry = batch?.entries.find((item) => item.rowIndex === activeKey)
    if (entry?.payment) {
      return formatJson(buildPaymentRequest(entry.payment))
    }
    return SAMPLE_REQUEST_TEXT
  }, [activeKey, batch, selectedExchange, selectedRowIndex])

  useEffect(() => {
    if (!batch) {
      return
    }

    const texts = { [SAMPLE_REQUEST_KEY]: SAMPLE_REQUEST_TEXT }
    batch.validEntries.forEach((entry) => {
      texts[entry.rowIndex] = formatJson(buildPaymentRequest(entry.payment))
    })
    setRequestTexts(texts)
    setResponseTexts({})

    const firstValid = batch.validEntries[0]
    if (firstValid) {
      setActiveKey(firstValid.rowIndex)
    }
  }, [batch])

  useEffect(() => {
    if (activeKey === SAMPLE_REQUEST_KEY) {
      return
    }

    setRequestTexts((current) => {
      if (current[activeKey]) {
        return current
      }
      return {
        ...current,
        [activeKey]: defaultRequestText,
      }
    })
  }, [activeKey, defaultRequestText])

  useEffect(() => {
    if (Object.keys(exchanges).length === 0) {
      return
    }

    setResponseTexts((current) => {
      const next = { ...current }
      Object.entries(exchanges).forEach(([rowIndex, exchange]) => {
        next[rowIndex] = formatJson(exchange.response)
      })
      return next
    })
  }, [exchanges])

  const requestText = requestTexts[activeKey] ?? defaultRequestText
  const responseText = responseTexts[activeKey] ?? ''
  const idempotencyKey = getIdempotencyKeyFromRequestText(requestText)
  const idempotencyStatus = idempotencyStatuses[activeKey]
  const activeLabel =
    activeKey === SAMPLE_REQUEST_KEY
      ? 'sample request'
      : batch
        ? `row ${activeKey}`
        : 'sample request'

  function handleRequestChange(value) {
    setRequestTexts((current) => ({
      ...current,
      [activeKey]: value,
    }))
  }

  function handleRowSelect(rowIndex) {
    setSelectedRowIndex(rowIndex)
    setActiveKey(rowIndex)
  }

  function handleReset() {
    resetImport()
    resetPropagation()
    setRequestTexts({ [SAMPLE_REQUEST_KEY]: SAMPLE_REQUEST_TEXT })
    setResponseTexts({})
    setActiveKey(SAMPLE_REQUEST_KEY)
    setSingleError(null)
    setIdempotencyStatuses({})
  }

  function handleRegenerateKey() {
    try {
      const updated = updateRequestIdempotencyKey(requestText, generateIdempotencyKey())
      handleRequestChange(updated)
      setSingleError(null)
    } catch (error) {
      setSingleError(error.message)
    }
  }

  async function handleSendRequest() {
    setSendingSingle(true)
    setSingleError(null)

    try {
      const request = parsePaymentRequestText(requestText)
      const result = await registerPaymentRequest(request)
      progressStore.markPaymentSent(
        request.headers['Store-Id'],
        request.headers['Idempotency-Key'],
        result,
      )
      setResponseTexts((current) => ({
        ...current,
        [activeKey]: formatJson(result.exchange.response),
      }))
      setIdempotencyStatuses((current) => ({
        ...current,
        [activeKey]: describeIdempotencyStatus(result.status),
      }))
      refreshBackendPayments(storeId)
      return result
    } catch (error) {
      setSingleError(error.message)
      if (error.exchange?.response) {
        setResponseTexts((current) => ({
          ...current,
          [activeKey]: formatJson(error.exchange.response),
        }))
      }
      throw error
    } finally {
      setSendingSingle(false)
    }
  }

  async function handlePropagate() {
    await propagate(validPayments, requestTexts)
    refreshBackendPayments(storeId)
  }

  return (
    <div className="import-page">
      <header className="page-header">
        <div>
          <p className="eyebrow">Coffee Place · End of Day</p>
          <h1>Payment Propagation</h1>
          <p className="subtitle">
            Edit the sample request JSON, send a single payment, or upload a CSV notebook and
            propagate in bulk.
          </p>
        </div>
      </header>

      <BackendPaymentsPreview
        storeId={storeId}
        payments={backendPayments}
        loading={backendLoading}
        error={backendError}
        onStoreIdChange={setStoreId}
        onRefresh={() => refreshBackendPayments(storeId)}
      />

      <RequestResponsePanel
        activeLabel={activeLabel}
        requestText={requestText}
        responseText={responseText}
        idempotencyKey={idempotencyKey}
        idempotencyStatus={idempotencyStatus}
        onRequestChange={handleRequestChange}
        onSendRequest={handleSendRequest}
        onRegenerateKey={handleRegenerateKey}
        sending={sendingSingle}
      />

      {singleError && <p className="error-message">{singleError}</p>}

      <ImportControls disabled={loading || propagating} onFileSelected={importFile} />

      {loading && <p className="status-message">Parsing CSV…</p>}
      {importError && <p className="error-message">{importError}</p>}
      {propagationError && <p className="error-message">{propagationError}</p>}

      <ImportPreview
        batch={batch}
        rowStatuses={rowStatuses}
        selectedRowIndex={selectedRowIndex}
        onRowSelect={handleRowSelect}
      />

      {batch && (
        <div className="actions">
          <button
            type="button"
            className="primary"
            disabled={!canPropagate}
            onClick={handlePropagate}
          >
            {propagating ? 'Propagating…' : `Propagate ${validPayments.length} payments`}
          </button>
          <button
            type="button"
            className="secondary"
            disabled={propagating}
            onClick={handleReset}
          >
            Clear import
          </button>
          <button
            type="button"
            className="secondary"
            disabled={propagating}
            onClick={() => {
              clearProgressStore()
              resetPropagation()
            }}
          >
            Reset sent history
          </button>
        </div>
      )}

      <PropagationProgress
        total={validPayments.length}
        completed={completedCount}
        propagating={propagating}
      />

      <ResultsSummary result={result} />
    </div>
  )
}

