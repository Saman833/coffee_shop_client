export function ResultsSummary({ result }) {
  if (!result) {
    return null
  }

  return (
    <div className="panel results-panel">
      <div className="panel-header">
        <h2>Batch complete</h2>
        <p>
          Request <code>{result.requestId}</code> finished with status <strong>{result.status}</strong>
        </p>
        <p>
          {result.replayed ?? 0} idempotent replays · {result.succeeded ?? 0} newly created
        </p>
      </div>
      <div className="results-grid">
        <div className="result-card success">
          <span>{result.succeeded}</span>
          <label>Created</label>
        </div>
        <div className="result-card replay">
          <span>{result.replayed ?? 0}</span>
          <label>Replayed</label>
        </div>
        <div className="result-card failed">
          <span>{result.failed}</span>
          <label>Failed</label>
        </div>
      </div>
    </div>
  )
}
