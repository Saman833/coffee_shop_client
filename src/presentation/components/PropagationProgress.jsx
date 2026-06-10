export function PropagationProgress({ total, completed, propagating }) {
  if (!propagating && completed === 0) {
    return null
  }

  const percent = total > 0 ? Math.round((completed / total) * 100) : 0

  return (
    <div className="panel progress-panel">
      <div className="panel-header">
        <h2>Propagating to Central System</h2>
        <p>
          {completed} of {total} rows processed ({percent}%)
        </p>
      </div>
      <div className="progress-bar">
        <div className="progress-fill" style={{ width: `${percent}%` }} />
      </div>
    </div>
  )
}
