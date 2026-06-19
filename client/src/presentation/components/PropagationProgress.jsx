export function PropagationProgress({ total, completed, propagating, batchStatus }) {
  if (!propagating && completed === 0 && !batchStatus) {
    return null
  }

  const percent = total > 0 ? Math.round((completed / total) * 100) : 0

  return (
    <div className="panel progress-panel">
      <div className="panel-header">
        <h2>Async batch propagation</h2>
        <p>
          {completed} of {total} rows processed ({percent}%)
        </p>
        {batchStatus?.requestId && (
          <p className="status-message">
            Request <code>{batchStatus.requestId}</code> · status {batchStatus.status}
          </p>
        )}
      </div>
      <div className="progress-bar">
        <div className="progress-fill" style={{ width: `${percent}%` }} />
      </div>
    </div>
  )
}
