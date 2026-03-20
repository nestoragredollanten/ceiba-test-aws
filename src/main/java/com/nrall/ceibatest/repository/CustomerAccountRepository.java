package com.nrall.ceibatest.repository;

import com.nrall.ceibatest.domain.model.CustomerAccount;

import java.math.BigDecimal;
import java.util.Optional;

public interface CustomerAccountRepository {

    Optional<CustomerAccount> findByCustomerId(String customerId);

    CustomerAccount createIfAbsent(String customerId, BigDecimal initialBalance);

    CustomerAccount save(CustomerAccount account);
}

