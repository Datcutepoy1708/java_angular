package com.store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled background job to scan and auto-cancel PENDING orders older than 24 hours.
 * Releases all reserved inventory back to available stock.
 *
 * Profile "!test" ensures this background scheduler does not interfere with integration tests.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class OrderTimeoutScheduler {

    private final OrderService orderService;

    // Runs every 15 minutes
    @Scheduled(cron = "0 */15 * * * *")
    public void autoCancelExpiredPendingOrders() {
        log.info("Running OrderTimeoutScheduler: scanning for expired PENDING orders (>24h)");
        try {
            int cancelled = orderService.processExpiredPendingOrders();
            if (cancelled > 0) {
                log.info("OrderTimeoutScheduler successfully cancelled {} expired orders and released inventory", cancelled);
            }
        } catch (Exception e) {
            log.error("Error occurred during OrderTimeoutScheduler execution: {}", e.getMessage(), e);
        }
    }
}
