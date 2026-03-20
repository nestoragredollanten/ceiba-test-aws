package com.nrall.ceibatest.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@AllArgsConstructor
public class Subscription {

    private final String id;
    private final String customerId;
    private final Integer fundId;
    private final String fundName;
    private final BigDecimal amount;
    private final OffsetDateTime createdAt;
    private SubscriptionStatus status;
    @Setter
    private OffsetDateTime cancelledAt;

    public Subscription(
            String id,
            String customerId,
            Integer fundId,
            String fundName,
            BigDecimal amount,
            OffsetDateTime createdAt,
            SubscriptionStatus status
    ) {
        this(id, customerId, fundId, fundName, amount, createdAt, status, null);
    }

    public void cancel(OffsetDateTime cancelledAt) {
        this.status = SubscriptionStatus.CANCELLED;
        this.cancelledAt = cancelledAt;
    }
}
