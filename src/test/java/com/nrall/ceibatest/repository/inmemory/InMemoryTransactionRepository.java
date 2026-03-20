package com.nrall.ceibatest.repository.inmemory;

import com.nrall.ceibatest.domain.model.Transaction;
import com.nrall.ceibatest.repository.TransactionRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryTransactionRepository implements TransactionRepository {

    private final Map<String, List<Transaction>> store = new ConcurrentHashMap<>();

    @Override
    public Transaction save(Transaction transaction) {
        store.computeIfAbsent(transaction.getCustomerId(), key -> new CopyOnWriteArrayList<>()).add(transaction);
        return transaction;
    }

    @Override
    public List<Transaction> findByCustomerId(String customerId) {
        return store.getOrDefault(customerId, List.of())
                .stream()
                .sorted(Comparator.comparing(Transaction::getCreatedAt).reversed())
                .toList();
    }
}

