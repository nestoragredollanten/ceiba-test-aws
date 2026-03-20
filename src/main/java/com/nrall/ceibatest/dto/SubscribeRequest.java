package com.nrall.ceibatest.dto;

import com.nrall.ceibatest.domain.model.NotificationChannel;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record SubscribeRequest(
        @NotBlank String customerId,
        @NotNull @Positive Integer fundId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotNull NotificationChannel notificationChannel,
        @NotBlank String destination
) {
}

