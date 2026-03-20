package com.nrall.ceibatest.dto;

import com.nrall.ceibatest.domain.model.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SubscriptionResponse(
        String subscriptionId,
        String customerId,
        Integer fundId,
        String fundName,
        BigDecimal amount,
        SubscriptionStatus status,
        BigDecimal availableBalance,
        OffsetDateTime createdAt,
        OffsetDateTime cancelledAt
) {
}

