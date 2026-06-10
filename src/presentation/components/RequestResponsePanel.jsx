import { formatJson } from '../../shared/jsonFormat.js'
import { JsonCodeEditor } from './JsonCodeEditor.jsx'

const EMPTY_RESPONSE = formatJson({
  status: null,
  body: { message: 'Send a request to see the API response here.' },
})

export function RequestResponsePanel({
  activeLabel,
  requestText,
  responseText,
  idempotencyKey,
  idempotencyStatus,
  onRequestChange,
  onSendRequest,
  onRegenerateKey,
  sending,
}) {
  return (
    <div className="panel json-panel">
      <div className="panel-header">
        <h2>Request / Response</h2>
        <p>
          Every request uses <code>Idempotency-Key</code> so retries are safe. Send the same key
          twice and the backend returns <code>200 OK</code> with the original payment.
        </p>
      </div>

      <div className="idempotency-bar">
        <div>
          <span className="idempotency-label">Idempotency-Key</span>
          <code className="idempotency-value">{idempotencyKey || '—'}</code>
        </div>
        <div className="idempotency-actions">
          <button type="button" className="secondary" disabled={sending} onClick={onRegenerateKey}>
            New key
          </button>
          <button type="button" className="primary" disabled={sending} onClick={onSendRequest}>
            {sending ? 'Sending…' : 'Send request'}
          </button>
        </div>
      </div>

      {idempotencyStatus && (
        <p className={`idempotency-status ${idempotencyStatus.idempotentReplay ? 'replay' : 'created'}`}>
          {idempotencyStatus.label}
          {idempotencyStatus.message ? ` — ${idempotencyStatus.message}` : ''}
        </p>
      )}

      <p className="hint">
        {activeLabel ? `Editing: ${activeLabel}.` : ''} Change the JSON, resend with the same key to
        test idempotency, or click New key for a fresh payment.
      </p>

      <div className="json-split">
        <div className="json-pane">
          <h3>Request</h3>
          <JsonCodeEditor
            value={requestText}
            onChange={onRequestChange}
            placeholder="Request JSON"
          />
        </div>
        <div className="json-pane">
          <h3>Response</h3>
          <JsonCodeEditor
            value={responseText || EMPTY_RESPONSE}
            readOnly
            placeholder="Response JSON"
          />
        </div>
      </div>
    </div>
  )
}
