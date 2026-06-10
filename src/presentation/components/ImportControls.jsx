export function ImportControls({ disabled, onFileSelected }) {
  return (
    <div className="import-controls">
      <label className={`upload-zone ${disabled ? 'disabled' : ''}`}>
        <input
          type="file"
          accept=".csv,text/csv"
          disabled={disabled}
          onChange={(event) => {
            const file = event.target.files?.[0]
            if (file) {
              onFileSelected(file)
            }
            event.target.value = ''
          }}
        />
        <span className="upload-icon">☕</span>
        <strong>Upload notebook CSV</strong>
        <p>Select your end-of-day payments file to validate and propagate</p>
      </label>
    </div>
  )
}
