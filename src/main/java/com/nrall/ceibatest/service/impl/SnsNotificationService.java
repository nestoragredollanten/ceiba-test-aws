package com.nrall.ceibatest.service.impl;

import com.nrall.ceibatest.domain.model.NotificationChannel;
import com.nrall.ceibatest.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.SnsException;

import java.util.HashMap;
import java.util.Map;

@Service
@Primary
@ConditionalOnProperty(name = "sns.topic.arn")
public class SnsNotificationService implements NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SnsNotificationService.class);

    private final SnsClient snsClient;
    private final String topicArn;

    public SnsNotificationService(SnsClient snsClient, @Value("${sns.topic.arn}") String topicArn) {
        this.snsClient = snsClient;
        this.topicArn = topicArn;
    }

    @Override
    public void notify(String customerId, NotificationChannel channel, String destination, String message) {
        try {
            PublishRequest request = buildRequest(customerId, channel, destination, message);
            snsClient.publish(request);
        } catch (SnsException e) {
            LOGGER.error("Error sending SNS notification. customerId={}, channel={}, destination={}, reason={}",
                    customerId, channel, destination, e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage());
        }
    }

    private PublishRequest buildRequest(String customerId, NotificationChannel channel, String destination, String message) {
        Map<String, MessageAttributeValue> attributes = new HashMap<>();
        attributes.put("customerId", stringAttribute(customerId));
        attributes.put("channel", stringAttribute(channel.name()));
        attributes.put("destination", stringAttribute(destination));

        if (channel == NotificationChannel.SMS) {
            return PublishRequest.builder()
                    .phoneNumber(destination)
                    .message(message)
                    .messageAttributes(attributes)
                    .build();
        }

        String emailMessage = "To: " + destination + "\n\n" + message;
        return PublishRequest.builder()
                .topicArn(topicArn)
                .subject("BTG Fondos - Confirmacion de suscripcion")
                .message(emailMessage)
                .messageAttributes(attributes)
                .build();
    }

    private MessageAttributeValue stringAttribute(String value) {
        return MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(value)
                .build();
    }
}

