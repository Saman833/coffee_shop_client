package com.coffeeshop.backend.batch;

public enum BatchStatus {
    PENDING,
    PROCESSING,
    DONE,
    FAILED_PARTIAL,
    FAILED;

    public boolean isTerminal() {
        return this == DONE || this == FAILED || this == FAILED_PARTIAL;
    }
}
