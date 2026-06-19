import { RowStatus } from '../../domain/import/ImportStatus.js'

const STATUS_LABELS = {
  [RowStatus.PENDING]: 'Ready',
  [RowStatus.VALID]: 'Valid',
  [RowStatus.INVALID]: 'Invalid',
  [RowStatus.SENDING]: 'Sending…',
  [RowStatus.SUCCEEDED]: 'Sent',
  [RowStatus.FAILED]: 'Failed',
  [RowStatus.SKIPPED]: 'Skipped',
}

function getRowStatus(entry, rowStatuses) {
  if (rowStatuses[entry.rowIndex]) {
    return rowStatuses[entry.rowIndex].status
  }
  return entry.isValid ? RowStatus.VALID : RowStatus.INVALID
}

export function ImportPreview({
  batch,
  rowStatuses = {},
  selectedRowIndex,
  onRowSelect,
}) {
  if (!batch) {
    return null
  }

  return (
    <div className="panel">
      <div className="panel-header">
        <h2>Import: {batch.fileName}</h2>
        <p>
          {batch.validEntries.length} valid · {batch.invalidEntries.length} invalid ·{' '}
          {batch.entries.length} total rows. Click a valid row to inspect request/response JSON.
        </p>
      </div>

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Row</th>
              <th>Store</th>
              <th>Idempotency Key</th>
              <th>Coffee</th>
              <th>Price</th>
              <th>Currency</th>
              <th>Loyalty Card</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {batch.entries.map((entry) => {
              const status = getRowStatus(entry, rowStatuses)
              const isInvalid = status === RowStatus.INVALID
              const isSelected = selectedRowIndex === entry.rowIndex
              const canSelect = entry.isValid

              return (
                <tr
                  key={entry.rowIndex}
                  className={[
                    isInvalid ? 'row-invalid' : '',
                    isSelected ? 'row-selected' : '',
                    canSelect ? 'row-clickable' : '',
                  ]
                    .filter(Boolean)
                    .join(' ')}
                  onClick={() => {
                    if (canSelect) {
                      onRowSelect?.(entry.rowIndex)
                    }
                  }}
                >
                  <td>{entry.rowIndex}</td>
                  <td>{entry.raw.storeId}</td>
                  <td>{entry.raw.idempotencyKey}</td>
                  <td>{entry.raw.coffeeType}</td>
                  <td>{entry.raw.price}</td>
                  <td>{entry.raw.currency}</td>
                  <td>{entry.raw.loyaltyCardId}</td>
                  <td>
                    <span className={`status-badge status-${status}`}>
                      {STATUS_LABELS[status]}
                    </span>
                    {isInvalid && (
                      <small className="row-error">{entry.validation.errors.join('; ')}</small>
                    )}
                    {rowStatuses[entry.rowIndex]?.detail && status !== RowStatus.INVALID && (
                      <small className="row-detail">{rowStatuses[entry.rowIndex].detail}</small>
                    )}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </div>
  )
}
