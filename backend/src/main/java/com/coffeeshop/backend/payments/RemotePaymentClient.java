package com.coffeeshop.backend.payments;

import java.util.Map;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Calls the remote payments API. The remote service scopes idempotency on the
 * {@code Store-Id} + {@code Idempotency-Key} headers, so retries with the same key are safe.
 */
@Component
public class RemotePaymentClient {

    private static final String STORE_ID_HEADER = "Store-Id";
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final String PAYMENTS_PATH = "/api/v1/payments";

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
                    throw new RemotePaymentException(
                            "Remote payments API returned " + res.getStatusCode() + " for key " + idempotencyKey);
                })
                .toEntity(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});

        boolean replayed = response.getStatusCode().value() == 200;
        Map<String, Object> payload = response.getBody();
        String paymentId = payload == null ? null : asString(payload.get("paymentId"));
        return new RemotePaymentResult(paymentId, replayed);
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    public static class RemotePaymentException extends RuntimeException {
        public RemotePaymentException(String message) {
            super(message);
        }

        public RemotePaymentException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Wraps low-level {@link RestClientResponseException} so callers can decide on retry policy.
     */
    public static RemotePaymentException wrap(RestClientResponseException ex) {
        return new RemotePaymentException(
                "Remote call failed with status " + ex.getStatusCode() + ": " + ex.getResponseBodyAsString(), ex);
    }
}
