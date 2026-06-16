package com.coffeeshop.backend.payments;

import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Calls the remote payments API. The remote service scopes idempotency on the
 * {@code Store-Id} + {@code Idempotency-Key} headers, so retries with the same key are safe.
 */
@Component
public class RemotePaymentClient {

    private static final String STORE_ID_HEADER = "Store-Id";
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final String PAYMENTS_PATH = "/api/v1/payments";
    private static final int TOO_MANY_REQUESTS = 429;

    private final RestClient restClient;

    public RemotePaymentClient(RestClient remotePaymentsRestClient) {
        this.restClient = remotePaymentsRestClient;
    }

    public RemotePaymentResult registerPayment(String storeId, String idempotencyKey, RemotePaymentRequest body) {
        ResponseEntity<Map<String, Object>> response = restClient.post()
                .uri(PAYMENTS_PATH)
                .header(STORE_ID_HEADER, storeId)
                .header(IDEMPOTENCY_KEY_HEADER, idempotencyKey)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    HttpStatusCode status = res.getStatusCode();
                    String message = "Remote payments API returned " + status + " for key " + idempotencyKey;
                    // 4xx (except 429) means the request itself is bad - retrying will not help,
                    // so surface a permanent error and let the worker fail the item immediately.
                    if (status.is4xxClientError() && status.value() != TOO_MANY_REQUESTS) {
                        throw new PermanentRemotePaymentException(message);
                    }
                    throw new RemotePaymentException(message);
                })
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

        boolean replayed = response.getStatusCode().value() == 200;
        Map<String, Object> payload = response.getBody();
        String paymentId = payload == null ? null : asString(payload.get("paymentId"));
        return new RemotePaymentResult(paymentId, replayed);
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    /**
     * Transient failure (5xx, 429, or network error). Safe to retry with the same idempotency key.
     */
    public static class RemotePaymentException extends RuntimeException {
        public RemotePaymentException(String message) {
            super(message);
        }
    }

    /**
     * Permanent failure (4xx other than 429). The payment will never succeed as-is, so the worker
     * should stop retrying and mark the item failed.
     */
    public static class PermanentRemotePaymentException extends RemotePaymentException {
        public PermanentRemotePaymentException(String message) {
            super(message);
        }
    }
}
