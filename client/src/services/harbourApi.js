import { getPaymentsUrl } from './harbourApiConfig.js'
import { buildPaymentRequest } from './paymentRequestBuilder.js'
import { describeIdempotencyStatus } from '../shared/idempotency.js'
import { classifyResponse, withRetry } from './retryPolicy.js'

async function parseErrorBody(response) {
  try {
    return await response.json()
  } catch {
    return { detail: response.statusText }
  }
}

async function parseErrorDetail(response) {
  const body = await parseErrorBody(response)
  return body.detail || body.title || response.statusText
}

function buildSuccessResponse(request, status, body) {
  const idempotency = describeIdempotencyStatus(status)

  return {
    status,
    paymentId: body.paymentId,
    registeredAt: body.registeredAt,
    idempotentReplay: idempotency.idempotentReplay,
    exchange: {
      request,
      response: {
        status,
        idempotentReplay: idempotency.idempotentReplay,
        idempotencyLabel: idempotency.label,
        idempotencyMessage: idempotency.message,
        body,
      },
    },
  }
}

async function sendPaymentRequest(request) {
  const url = request.url || getPaymentsUrl()

  return withRetry(async () => {
    const response = await fetch(url, {
      method: request.method || 'POST',
      headers: request.headers,
      body: JSON.stringify(request.body),
    })

    const classification = classifyResponse(response.status)

    if (classification.kind === 'success') {
      const body = await response.json()
      return {
        retryable: false,
        value: buildSuccessResponse(request, response.status, body),
      }
    }

    const errorBody = await parseErrorBody(response)
    const detail = errorBody.detail || errorBody.title || response.statusText
    const exchange = {
      request,
      response: {
        status: response.status,
        body: errorBody,
      },
    }

    if (classification.retryable) {
      return {
        retryable: true,
        errorMessage: detail,
        exchange,
      }
    }

    const error = new Error(detail || `Payment registration failed with status ${response.status}`)
    error.exchange = exchange
    throw error
  })
}

export async function listPayments(storeId) {
  const url = `${getPaymentsUrl()}?storeId=${encodeURIComponent(storeId)}`
  const response = await fetch(url)

  if (!response.ok) {
    const detail = await parseErrorDetail(response)
    throw new Error(detail || `Failed to load payments (${response.status})`)
  }

  return response.json()
}

export async function registerPayment(payment) {
  return sendPaymentRequest(buildPaymentRequest(payment))
}

export async function registerPaymentRequest(request) {
  return sendPaymentRequest(request)
}
