package com.nrall.ceibatest.controller;

import com.nrall.ceibatest.dto.BalanceResponse;
import com.nrall.ceibatest.dto.FundResponse;
import com.nrall.ceibatest.dto.SubscribeRequest;
import com.nrall.ceibatest.dto.SubscriptionResponse;
import com.nrall.ceibatest.dto.TransactionResponse;
import com.nrall.ceibatest.service.FundCatalogService;
import com.nrall.ceibatest.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final FundCatalogService fundCatalogService;

    public SubscriptionController(SubscriptionService subscriptionService, FundCatalogService fundCatalogService) {
        this.subscriptionService = subscriptionService;
        this.fundCatalogService = fundCatalogService;
    }

    @GetMapping("/funds")
    public List<FundResponse> getFunds() {
        return fundCatalogService.getAll().stream()
                .map(fund -> new FundResponse(fund.id(), fund.name(), fund.minimumAmount(), fund.category()))
                .toList();
    }

    @PostMapping("/subscriptions")
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionResponse subscribe(@Valid @RequestBody SubscribeRequest request) {
        return subscriptionService.subscribe(request);
    }

    @DeleteMapping("/subscriptions/{subscriptionId}")
    public SubscriptionResponse cancel(
            @PathVariable String subscriptionId,
            @RequestParam String customerId
    ) {
        return subscriptionService.cancel(subscriptionId, customerId);
    }

    @GetMapping("/customers/{customerId}/transactions")
    public List<TransactionResponse> getHistory(@PathVariable String customerId) {
        return subscriptionService.getTransactions(customerId);
    }

    @GetMapping("/customers/{customerId}/balance")
    public BalanceResponse getBalance(@PathVariable String customerId) {
        return subscriptionService.getBalance(customerId);
    }
}

