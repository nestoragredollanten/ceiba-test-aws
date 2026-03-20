package com.nrall.ceibatest.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@AllArgsConstructor
public class Transaction {

    private final String id;
    private final String customerId;
    private final String subscriptionId;
    private final Integer fundId;
    private final String fundName;
    private final TransactionType type;
    private final BigDecimal amount;
    private final NotificationChannel notificationChannel;
    private final OffsetDateTime createdAt;
}
