package com.nrall.ceibatest.domain.model;

import java.math.BigDecimal;

public record Fund(Integer id, String name, BigDecimal minimumAmount, FundCategory category) {
}

