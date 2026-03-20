package com.nrall.ceibatest.service.impl;

import com.nrall.ceibatest.domain.model.NotificationChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SnsNotificationServiceTest {

    private static final String TOPIC_ARN = "arn:aws:sns:us-east-1:ACCOUNT_ID:btg-funds-notifications-prod";

    @Mock
    private SnsClient snsClient;

    private SnsNotificationService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new SnsNotificationService(snsClient, TOPIC_ARN);
    }

    @Test
    void shouldPublishSmsUsingPhoneNumber() {
        when(snsClient.publish(org.mockito.ArgumentMatchers.any(PublishRequest.class)))
                .thenReturn(PublishResponse.builder().messageId("m-1").build());

        service.notify("CUST-001", NotificationChannel.SMS, "+573001112233", "Suscripcion creada");

        ArgumentCaptor<PublishRequest> requestCaptor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient, times(1)).publish(requestCaptor.capture());

        PublishRequest request = requestCaptor.getValue();
        assertEquals("+573001112233", request.phoneNumber());
        assertNull(request.topicArn());
        assertEquals("Suscripcion creada", request.message());
        assertEquals("SMS", request.messageAttributes().get("channel").stringValue());
    }

    @Test
    void shouldPublishEmailUsingTopicArn() {
        when(snsClient.publish(org.mockito.ArgumentMatchers.any(PublishRequest.class)))
                .thenReturn(PublishResponse.builder().messageId("m-2").build());

        service.notify("CUST-002", NotificationChannel.EMAIL, "cliente@correo.com", "Suscripcion creada");

        ArgumentCaptor<PublishRequest> requestCaptor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient, times(1)).publish(requestCaptor.capture());

        PublishRequest request = requestCaptor.getValue();
        assertEquals(TOPIC_ARN, request.topicArn());
        assertNull(request.phoneNumber());
        assertEquals("BTG Fondos - Confirmacion de suscripcion", request.subject());
        assertEquals("EMAIL", request.messageAttributes().get("channel").stringValue());
    }
}

