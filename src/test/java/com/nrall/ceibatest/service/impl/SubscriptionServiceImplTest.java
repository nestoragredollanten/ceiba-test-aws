package com.nrall.ceibatest.service.impl;

import com.nrall.ceibatest.domain.model.NotificationChannel;
import com.nrall.ceibatest.dto.SubscribeRequest;
import com.nrall.ceibatest.exception.InsufficientBalanceException;
import com.nrall.ceibatest.repository.CustomerAccountRepository;
import com.nrall.ceibatest.repository.SubscriptionRepository;
import com.nrall.ceibatest.repository.TransactionRepository;
import com.nrall.ceibatest.repository.inmemory.InMemoryCustomerAccountRepository;
import com.nrall.ceibatest.repository.inmemory.InMemorySubscriptionRepository;
import com.nrall.ceibatest.repository.inmemory.InMemoryTransactionRepository;
import com.nrall.ceibatest.service.FundCatalogService;
import com.nrall.ceibatest.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SubscriptionServiceImplTest {

    private SubscriptionServiceImpl service;

    @BeforeEach
    void setUp() {
        CustomerAccountRepository customerRepository = new InMemoryCustomerAccountRepository();
        SubscriptionRepository subscriptionRepository = new InMemorySubscriptionRepository();
        TransactionRepository transactionRepository = new InMemoryTransactionRepository();
        FundCatalogService fundCatalogService = new StaticFundCatalogService();
        NotificationService notificationService = (customerId, channel, destination, message) -> {
        };

        service = new SubscriptionServiceImpl(
                customerRepository,
                subscriptionRepository,
                transactionRepository,
                fundCatalogService,
                notificationService,
                new BigDecimal("500000")
        );
    }

    @Test
    void shouldSubscribeAndDecreaseBalance() {
        SubscribeRequest request = new SubscribeRequest(
                "CUST-001",
                1,
                new BigDecimal("75000"),
                NotificationChannel.EMAIL,
                "cliente@correo.com"
        );

        var response = service.subscribe(request);

        assertNotNull(response.subscriptionId());
        assertEquals(new BigDecimal("425000"), response.availableBalance());
        assertEquals(1, service.getTransactions("CUST-001").size());
    }

    @Test
    void shouldFailWhenBalanceIsInsufficient() {
        SubscribeRequest request = new SubscribeRequest(
                "CUST-001",
                4,
                new BigDecimal("600000"),
                NotificationChannel.SMS,
                "3000000000"
        );

        assertThrows(InsufficientBalanceException.class, () -> service.subscribe(request));
    }

    @Test
    void shouldCancelAndRestoreBalance() {
        SubscribeRequest request = new SubscribeRequest(
                "CUST-001",
                3,
                new BigDecimal("50000"),
                NotificationChannel.EMAIL,
                "cliente@correo.com"
        );

        var subscribed = service.subscribe(request);
        var cancelled = service.cancel(subscribed.subscriptionId(), "CUST-001");

        assertEquals(new BigDecimal("500000"), cancelled.availableBalance());
        assertEquals(2, service.getTransactions("CUST-001").size());
    }

    @Test
    void shouldCreateCustomerImplicitlyWhenCheckingBalance() {
        var balance = service.getBalance("CUST-NEW");

        assertEquals("CUST-NEW", balance.customerId());
        assertEquals(new BigDecimal("500000"), balance.availableBalance());
    }

    @Test
    void shouldCreateCustomerImplicitlyWhenCheckingTransactions() {
        var transactions = service.getTransactions("CUST-NEW");
        var balance = service.getBalance("CUST-NEW");

        assertEquals(0, transactions.size());
        assertEquals(new BigDecimal("500000"), balance.availableBalance());
    }
}

