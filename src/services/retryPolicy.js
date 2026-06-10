const DEFAULT_MAX_ATTEMPTS = 4
const DEFAULT_BASE_DELAY_MS = 500

function isRetryableStatus(status) {
  return status === 408 || status === 429 || status >= 500
}

function isRetryableError(error) {
  return error instanceof TypeError || error.name === 'AbortError'
}

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

export async function withRetry(operation, options = {}) {
  const maxAttempts = options.maxAttempts ?? DEFAULT_MAX_ATTEMPTS
  const baseDelayMs = options.baseDelayMs ?? DEFAULT_BASE_DELAY_MS
  let lastError

  for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
    try {
      const result = await operation(attempt)
      if (result.retryable) {
        lastError = new Error(result.errorMessage || `Retryable failure on attempt ${attempt}`)
        if (attempt < maxAttempts) {
          await delay(baseDelayMs * 2 ** (attempt - 1))
          continue
        }
        throw lastError
      }
      return result.value
    } catch (error) {
      lastError = error
      if (!isRetryableError(error) || attempt >= maxAttempts) {
        throw error
      }
      await delay(baseDelayMs * 2 ** (attempt - 1))
    }
  }

  throw lastError
}

export function classifyResponse(status) {
  if (status === 200 || status === 201) {
    return { kind: 'success' }
  }
  if (status === 400) {
    return { kind: 'client-error', retryable: false }
  }
  if (isRetryableStatus(status)) {
    return { kind: 'server-error', retryable: true }
  }
  return { kind: 'unexpected', retryable: false }
}
