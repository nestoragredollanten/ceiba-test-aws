package com.nrall.ceibatest.repository;

import com.nrall.ceibatest.domain.model.Subscription;

import java.util.Optional;

public interface SubscriptionRepository {

    Subscription save(Subscription subscription);

    Optional<Subscription> findById(String id);
}

