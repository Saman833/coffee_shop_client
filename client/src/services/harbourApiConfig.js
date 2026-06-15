export function getApiBaseUrl() {
  const configured = import.meta.env.VITE_API_BASE_URL?.trim()
  return configured || ''
}

export function getPaymentsUrl() {
  const base = getApiBaseUrl()
  return `${base}/api/v1/payments`
}
