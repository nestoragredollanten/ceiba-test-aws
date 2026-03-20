package com.nrall.ceibatest.dto;

import com.nrall.ceibatest.domain.model.FundCategory;

import java.math.BigDecimal;

public record FundResponse(
        Integer id,
        String name,
        BigDecimal minimumAmount,
        FundCategory category
) {
}

