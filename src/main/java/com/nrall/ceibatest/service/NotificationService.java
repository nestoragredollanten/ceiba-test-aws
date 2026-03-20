package com.nrall.ceibatest.service;

import com.nrall.ceibatest.domain.model.NotificationChannel;

public interface NotificationService {

    void notify(String customerId, NotificationChannel channel, String destination, String message);
}

