package com.nrall.ceibatest.dto;

import com.nrall.ceibatest.domain.model.NotificationChannel;
import com.nrall.ceibatest.domain.model.TransactionType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TransactionResponse(
        String transactionId,
        String subscriptionId,
        Integer fundId,
        String fundName,
        TransactionType type,
        BigDecimal amount,
        NotificationChannel notificationChannel,
        OffsetDateTime createdAt
) {
}

