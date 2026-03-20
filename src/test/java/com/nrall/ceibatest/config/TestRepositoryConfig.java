package com.nrall.ceibatest.config;

import com.nrall.ceibatest.repository.CustomerAccountRepository;
import com.nrall.ceibatest.repository.SubscriptionRepository;
import com.nrall.ceibatest.repository.TransactionRepository;
import com.nrall.ceibatest.repository.inmemory.InMemoryCustomerAccountRepository;
import com.nrall.ceibatest.repository.inmemory.InMemorySubscriptionRepository;
import com.nrall.ceibatest.repository.inmemory.InMemoryTransactionRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestRepositoryConfig {

    @Bean
    @Primary
    public CustomerAccountRepository customerAccountRepository() {
        return new InMemoryCustomerAccountRepository();
    }

    @Bean
    @Primary
    public SubscriptionRepository subscriptionRepository() {
        return new InMemorySubscriptionRepository();
    }

    @Bean
    @Primary
    public TransactionRepository transactionRepository() {
        return new InMemoryTransactionRepository();
    }
}

