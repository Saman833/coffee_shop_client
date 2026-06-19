package com.coffeeshop.backend.batch;

public enum ItemStatus {
    PENDING,
    PROCESSING,
    DONE,
    FAILED;

    public boolean isTerminal() {
        return this == DONE || this == FAILED;
    }
}
