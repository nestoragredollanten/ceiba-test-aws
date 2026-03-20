package com.nrall.ceibatest.dto;

import java.math.BigDecimal;

public record BalanceResponse(
        String customerId,
        BigDecimal availableBalance
) {
}

