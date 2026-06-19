package com.coffeeshop.backend.batch;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PaymentBatchController.class)
class PaymentBatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentBatchService service;

    @Test
    void submitReturnsAcceptedWithRequestId() throws Exception {
        UUID requestId = UUID.randomUUID();
        when(service.submit(any())).thenReturn(new BatchSubmissionResponse(requestId, BatchStatus.PENDING, 2));

        String body = """
                {
                  "storeId": "store-1",
                  "payments": [
                    {
                      "idempotencyKey": "k-1",
                      "coffeeType": "LATTE",
                      "price": 4.50,
                      "currency": "USD",
                      "loyaltyCardId": "loyalty-1"
                    },
                    {
                      "idempotencyKey": "k-2",
                      "coffeeType": "ESPRESSO",
                      "price": 3.00,
                      "currency": "USD",
                      "loyaltyCardId": "loyalty-1"
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/payment-batches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", containsString("/api/v1/payment-batches/" + requestId)))
                .andExpect(jsonPath("$.requestId").value(requestId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void submitWithMissingFieldsReturnsProblemDetail() throws Exception {
        String body = """
                {
                  "storeId": "",
                  "payments": []
                }
                """;

        mockMvc.perform(post("/api/v1/payment-batches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid request payload"))
                .andExpect(jsonPath("$.fieldErrors.storeId").exists())
                .andExpect(jsonPath("$.fieldErrors.payments").exists());
    }

    @Test
    void submitRejectsInvalidCurrencyAndPrice() throws Exception {
        String body = """
                {
                  "storeId": "store-1",
                  "payments": [
                    {
                      "idempotencyKey": "k-1",
                      "coffeeType": "LATTE",
                      "price": 0,
                      "currency": "usd",
                      "loyaltyCardId": "loyalty-1"
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/payment-batches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request payload"));
    }

    @Test
    void getStatusReturnsPayloadWhenFound() throws Exception {
        UUID requestId = UUID.randomUUID();
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        when(service.getStatus(requestId)).thenReturn(Optional.of(new BatchStatusResponse(
                requestId, BatchStatus.PROCESSING, 5, 3, 2, 1, 0, null, now, now)));

        mockMvc.perform(get("/api/v1/payment-batches/" + requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(requestId.toString()))
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.processed").value(3))
                .andExpect(jsonPath("$.succeeded").value(2))
                .andExpect(jsonPath("$.replayed").value(1));
    }

    @Test
    void getStatusReturnsNotFoundWhenMissing() throws Exception {
        UUID requestId = UUID.randomUUID();
        when(service.getStatus(requestId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/payment-batches/" + requestId))
                .andExpect(status().isNotFound());
    }
}
