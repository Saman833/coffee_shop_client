package com.coffeeshop.backend.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically drives {@link BatchOrchestrator} to drain pending batch items.
 * Disabled in tests via {@code coffeeshop.worker.enabled=false}.
 */
@Component
@ConditionalOnProperty(name = "coffeeshop.worker.enabled", havingValue = "true", matchIfMissing = true)
public class BatchWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchWorker.class);

    private final BatchOrchestrator orchestrator;

    public BatchWorker(BatchOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Scheduled(fixedDelayString = "${coffeeshop.worker.poll-delay-ms:1000}",
            initialDelayString = "${coffeeshop.worker.poll-delay-ms:1000}")
    public void poll() {
        try {
            int processed = orchestrator.processNextChunk();
            if (processed > 0) {
                LOGGER.debug("Batch worker processed {} items", processed);
            }
        } catch (Exception ex) {
            LOGGER.error("Batch worker tick failed", ex);
        }
    }
}
