import { useMemo, useRef } from 'react'
import Prism from 'prismjs'
import 'prismjs/components/prism-json.js'
import 'prismjs/themes/prism-tomorrow.css'

function highlightJson(code) {
  if (!Prism.languages.json) {
    return code
  }

  try {
    return Prism.highlight(code, Prism.languages.json, 'json')
  } catch {
    return code
  }
}

export function JsonCodeEditor({ value, onChange, readOnly = false, placeholder }) {
  const preRef = useRef(null)
  const safeValue = value ?? ''

  const highlighted = useMemo(() => highlightJson(safeValue), [safeValue])

  function syncScroll(event) {
    if (preRef.current) {
      preRef.current.scrollTop = event.target.scrollTop
      preRef.current.scrollLeft = event.target.scrollLeft
    }
  }

  if (readOnly) {
    return (
      <div className="json-code-editor read-only">
        <pre className="json-code-pre json-code-viewer">
          <code dangerouslySetInnerHTML={{ __html: `${highlighted}\n` }} />
        </pre>
      </div>
    )
  }

  return (
    <div className="json-code-editor">
      <textarea
        className="json-code-textarea editable"
        value={safeValue}
        onChange={(event) => onChange?.(event.target.value)}
        onScroll={syncScroll}
        placeholder={placeholder}
        spellCheck={false}
      />
    </div>
  )
}
