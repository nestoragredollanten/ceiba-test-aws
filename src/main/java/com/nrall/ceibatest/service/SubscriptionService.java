package com.nrall.ceibatest.service;

import com.nrall.ceibatest.dto.BalanceResponse;
import com.nrall.ceibatest.dto.SubscribeRequest;
import com.nrall.ceibatest.dto.SubscriptionResponse;
import com.nrall.ceibatest.dto.TransactionResponse;

import java.util.List;

public interface SubscriptionService {

    SubscriptionResponse subscribe(SubscribeRequest request);

    SubscriptionResponse cancel(String subscriptionId, String customerId);

    List<TransactionResponse> getTransactions(String customerId);

    BalanceResponse getBalance(String customerId);
}

