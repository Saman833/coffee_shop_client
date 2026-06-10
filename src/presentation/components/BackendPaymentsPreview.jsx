export function BackendPaymentsPreview({ storeId, payments, loading, error, onStoreIdChange, onRefresh }) {
  return (
    <div className="panel">
      <div className="panel-header">
        <h2>Central System payments</h2>
        <p>Loaded from the Harbour Cloud backend for the selected store.</p>
      </div>

      <div className="sample-row backend-controls">
        <label htmlFor="store-id">Store ID</label>
        <input
          id="store-id"
          type="text"
          value={storeId}
          disabled={loading}
          onChange={(event) => onStoreIdChange(event.target.value)}
          placeholder="store-london-01"
        />
        <button type="button" className="secondary" disabled={loading} onClick={onRefresh}>
          {loading ? 'Loading…' : 'Refresh'}
        </button>
      </div>

      {error && <p className="error-message">{error}</p>}

      {!loading && !error && payments.length === 0 && (
        <p className="status-message">No payments found for this store yet.</p>
      )}

      {payments.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Payment ID</th>
                <th>Store</th>
                <th>Coffee</th>
                <th>Price</th>
                <th>Currency</th>
                <th>Loyalty Card</th>
                <th>Registered At</th>
              </tr>
            </thead>
            <tbody>
              {payments.map((payment) => (
                <tr key={payment.paymentId}>
                  <td>{payment.paymentId}</td>
                  <td>{payment.storeId}</td>
                  <td>{payment.coffeeType}</td>
                  <td>{payment.price}</td>
                  <td>{payment.currency}</td>
                  <td>{payment.loyaltyCardId}</td>
                  <td>{payment.registeredAt}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
