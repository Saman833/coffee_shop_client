import { ErrorBoundary } from './presentation/components/ErrorBoundary.jsx'
import { ImportPage } from './presentation/pages/ImportPage.jsx'
import './App.css'

function App() {
  return (
    <ErrorBoundary>
      <ImportPage />
    </ErrorBoundary>
  )
}

export default App
