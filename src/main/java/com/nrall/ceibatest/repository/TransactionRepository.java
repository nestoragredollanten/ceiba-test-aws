package com.nrall.ceibatest.repository;

import com.nrall.ceibatest.domain.model.Transaction;

import java.util.List;

public interface TransactionRepository {

    Transaction save(Transaction transaction);

    List<Transaction> findByCustomerId(String customerId);
}

