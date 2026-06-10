export function ResultsSummary({ result }) {
  if (!result) {
    return null
  }

  return (
    <div className="panel results-panel">
      <div className="panel-header">
        <h2>Propagation complete</h2>
        <p>
          {result.replayed ?? 0} idempotent replays (200) · {result.succeeded ?? 0} newly created
          (201)
        </p>
      </div>
      <div className="results-grid">
        <div className="result-card success">
          <span>{result.succeeded}</span>
          <label>Created (201)</label>
        </div>
        <div className="result-card replay">
          <span>{result.replayed ?? 0}</span>
          <label>Replayed (200)</label>
        </div>
        <div className="result-card failed">
          <span>{result.failed}</span>
          <label>Failed</label>
        </div>
      </div>
    </div>
  )
}
