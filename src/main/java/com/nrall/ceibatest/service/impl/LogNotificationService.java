package com.nrall.ceibatest.service.impl;

import com.nrall.ceibatest.domain.model.NotificationChannel;
import com.nrall.ceibatest.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LogNotificationService implements NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogNotificationService.class);

    @Override
    public void notify(String customerId, NotificationChannel channel, String destination, String message) {
        LOGGER.info("Notification sent. customerId={}, channel={}, destination={}, message={}",
                customerId, channel, destination, message);
    }
}

