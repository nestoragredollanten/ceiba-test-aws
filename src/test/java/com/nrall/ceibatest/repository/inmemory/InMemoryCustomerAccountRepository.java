package com.nrall.ceibatest.repository.inmemory;

import com.nrall.ceibatest.domain.model.CustomerAccount;
import com.nrall.ceibatest.repository.CustomerAccountRepository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryCustomerAccountRepository implements CustomerAccountRepository {

    private final Map<String, CustomerAccount> store = new ConcurrentHashMap<>();

    @Override
    public Optional<CustomerAccount> findByCustomerId(String customerId) {
        return Optional.ofNullable(store.get(customerId));
    }

    @Override
    public CustomerAccount createIfAbsent(String customerId, BigDecimal initialBalance) {
        return store.computeIfAbsent(customerId, key -> new CustomerAccount(customerId, initialBalance));
    }

    @Override
    public CustomerAccount save(CustomerAccount account) {
        store.put(account.getCustomerId(), account);
        return account;
    }
}

