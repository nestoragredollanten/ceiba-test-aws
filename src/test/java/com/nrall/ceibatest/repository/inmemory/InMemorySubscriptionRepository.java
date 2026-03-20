package com.nrall.ceibatest.repository.inmemory;

import com.nrall.ceibatest.domain.model.Subscription;
import com.nrall.ceibatest.repository.SubscriptionRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySubscriptionRepository implements SubscriptionRepository {

    private final Map<String, Subscription> store = new ConcurrentHashMap<>();

    @Override
    public Subscription save(Subscription subscription) {
        store.put(subscription.getId(), subscription);
        return subscription;
    }

    @Override
    public Optional<Subscription> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }
}

