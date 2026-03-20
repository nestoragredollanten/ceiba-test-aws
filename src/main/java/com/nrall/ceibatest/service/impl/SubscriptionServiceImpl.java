package com.nrall.ceibatest.service.impl;

import com.nrall.ceibatest.domain.model.CustomerAccount;
import com.nrall.ceibatest.domain.model.Fund;
import com.nrall.ceibatest.domain.model.Subscription;
import com.nrall.ceibatest.domain.model.SubscriptionStatus;
import com.nrall.ceibatest.domain.model.Transaction;
import com.nrall.ceibatest.domain.model.TransactionType;
import com.nrall.ceibatest.dto.BalanceResponse;
import com.nrall.ceibatest.dto.SubscribeRequest;
import com.nrall.ceibatest.dto.SubscriptionResponse;
import com.nrall.ceibatest.dto.TransactionResponse;
import com.nrall.ceibatest.exception.BusinessException;
import com.nrall.ceibatest.exception.InsufficientBalanceException;
import com.nrall.ceibatest.exception.NotFoundException;
import com.nrall.ceibatest.repository.CustomerAccountRepository;
import com.nrall.ceibatest.repository.SubscriptionRepository;
import com.nrall.ceibatest.repository.TransactionRepository;
import com.nrall.ceibatest.service.FundCatalogService;
import com.nrall.ceibatest.service.NotificationService;
import com.nrall.ceibatest.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    private final CustomerAccountRepository customerAccountRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final TransactionRepository transactionRepository;
    private final FundCatalogService fundCatalogService;
    private final NotificationService notificationService;
    private final BigDecimal initialBalance;

    public SubscriptionServiceImpl(
            CustomerAccountRepository customerAccountRepository,
            SubscriptionRepository subscriptionRepository,
            TransactionRepository transactionRepository,
            FundCatalogService fundCatalogService,
            NotificationService notificationService,
            @Value("${app.initial-balance}") BigDecimal initialBalance
    ) {
        this.customerAccountRepository = customerAccountRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.transactionRepository = transactionRepository;
        this.fundCatalogService = fundCatalogService;
        this.notificationService = notificationService;
        this.initialBalance = initialBalance;
    }

    @Override
    public SubscriptionResponse subscribe(SubscribeRequest request) {
        Fund fund = fundCatalogService.findById(request.fundId())
                .orElseThrow(() -> new NotFoundException("Fondo no encontrado: " + request.fundId()));

        if (request.amount().compareTo(fund.minimumAmount()) < 0) {
            throw new BusinessException("El monto minimo para el fondo " + fund.name() + " es " + fund.minimumAmount());
        }

        CustomerAccount account = customerAccountRepository.createIfAbsent(request.customerId(), initialBalance);

        if (account.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientBalanceException(
                    "No tiene saldo disponible para vincularse al fondo " + fund.name()
            );
        }

        account.debit(request.amount());
        customerAccountRepository.save(account);

        OffsetDateTime now = OffsetDateTime.now();
        String subscriptionId = UUID.randomUUID().toString();
        String transactionId = UUID.randomUUID().toString();

        Subscription subscription = new Subscription(
                subscriptionId,
                request.customerId(),
                fund.id(),
                fund.name(),
                request.amount(),
                now,
                SubscriptionStatus.ACTIVE
        );

        Transaction transaction = new Transaction(
                transactionId,
                request.customerId(),
                subscriptionId,
                fund.id(),
                fund.name(),
                TransactionType.OPENING,
                request.amount(),
                request.notificationChannel(),
                now
        );

        subscriptionRepository.save(subscription);
        transactionRepository.save(transaction);
        notificationService.notify(
                request.customerId(),
                request.notificationChannel(),
                request.destination(),
                "Suscripcion creada para el fondo " + fund.name() + " por COP " + request.amount()
        );

        return mapToResponse(subscription, account.getBalance());
    }

    @Override
    public SubscriptionResponse cancel(String subscriptionId, String customerId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new NotFoundException("Suscripcion no encontrada: " + subscriptionId));

        if (!subscription.getCustomerId().equals(customerId)) {
            throw new BusinessException("La suscripcion no pertenece al cliente " + customerId);
        }

        if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new BusinessException("La suscripcion ya se encuentra cancelada");
        }

        CustomerAccount account = customerAccountRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new NotFoundException("Cuenta del cliente no encontrada: " + customerId));
        account.credit(subscription.getAmount());
        customerAccountRepository.save(account);

        OffsetDateTime now = OffsetDateTime.now();
        subscription.cancel(now);
        subscriptionRepository.save(subscription);

        Transaction transaction = new Transaction(
                UUID.randomUUID().toString(),
                customerId,
                subscriptionId,
                subscription.getFundId(),
                subscription.getFundName(),
                TransactionType.CANCELLATION,
                subscription.getAmount(),
                null,
                now
        );
        transactionRepository.save(transaction);

        return mapToResponse(subscription, account.getBalance());
    }

    @Override
    public List<TransactionResponse> getTransactions(String customerId) {
        customerAccountRepository.createIfAbsent(customerId, initialBalance);
        return transactionRepository.findByCustomerId(customerId)
                .stream()
                .map(transaction -> new TransactionResponse(
                        transaction.getId(),
                        transaction.getSubscriptionId(),
                        transaction.getFundId(),
                        transaction.getFundName(),
                        transaction.getType(),
                        transaction.getAmount(),
                        transaction.getNotificationChannel(),
                        transaction.getCreatedAt()
                ))
                .toList();
    }

    @Override
    public BalanceResponse getBalance(String customerId) {
        CustomerAccount account = customerAccountRepository.createIfAbsent(customerId, initialBalance);
        return new BalanceResponse(customerId, account.getBalance());
    }

    private SubscriptionResponse mapToResponse(Subscription subscription, BigDecimal balance) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getCustomerId(),
                subscription.getFundId(),
                subscription.getFundName(),
                subscription.getAmount(),
                subscription.getStatus(),
                balance,
                subscription.getCreatedAt(),
                subscription.getCancelledAt()
        );
    }
}

