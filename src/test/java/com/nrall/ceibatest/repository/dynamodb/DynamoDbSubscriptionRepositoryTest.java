package com.nrall.ceibatest.repository.dynamodb;

import com.nrall.ceibatest.domain.model.Subscription;
import com.nrall.ceibatest.domain.model.SubscriptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("DynamoDbSubscriptionRepository Tests")
class DynamoDbSubscriptionRepositoryTest {

    @Mock
    private DynamoDbClient dynamoDbClient;

    private DynamoDbSubscriptionRepository repository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        repository = new DynamoDbSubscriptionRepository(dynamoDbClient);
    }

    // ============= save Tests =============

    @Test
    @DisplayName("save should persist subscription to DynamoDB")
    void testSaveSuccessfully() {
        // Arrange
        Subscription subscription = createTestSubscription(SubscriptionStatus.ACTIVE, null);
        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenReturn(PutItemResponse.builder().build());

        // Act
        Subscription result = repository.save(subscription);

        // Assert
        assertNotNull(result);
        assertSame(subscription, result);
        verify(dynamoDbClient, times(1)).putItem(any(PutItemRequest.class));
    }

    @Test
    @DisplayName("save should handle active subscription without cancelledAt")
    void testSaveActiveSubscriptionWithoutCancelledAt() {
        // Arrange
        Subscription subscription = createTestSubscription(SubscriptionStatus.ACTIVE, null);
        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenReturn(PutItemResponse.builder().build());

        // Act
        repository.save(subscription);

        // Assert
        verify(dynamoDbClient).putItem(any(PutItemRequest.class));
    }

    @Test
    @DisplayName("save should handle cancelled subscription with cancelledAt")
    void testSaveCancelledSubscriptionWithCancelledAt() {
        // Arrange
        OffsetDateTime cancelledAt = OffsetDateTime.now();
        Subscription subscription = createTestSubscription(SubscriptionStatus.CANCELLED, cancelledAt);
        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenReturn(PutItemResponse.builder().build());

        // Act
        repository.save(subscription);

        // Assert
        verify(dynamoDbClient).putItem(argThat((PutItemRequest request) -> {
            Map<String, AttributeValue> item = request.item();
            return item.containsKey("subscriptionId") &&
                    item.get("status").s().equals("CANCELLED") &&
                    item.containsKey("cancelledAt");
        }));
    }

    @Test
    @DisplayName("save should map all subscription fields correctly")
    void testSaveMapsProperly() {
        // Arrange
        String subscriptionId = "SUB-UUID";
        String customerId = "CUST-001";
        Integer fundId = 1;
        String fundName = "FPV_BTG_PACTUAL_RECAUDADORA";
        BigDecimal amount = new BigDecimal("75000");
        OffsetDateTime createdAt = OffsetDateTime.now();

        Subscription subscription = new Subscription(
                subscriptionId, customerId, fundId, fundName,
                amount, createdAt, SubscriptionStatus.ACTIVE
        );

        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenReturn(PutItemResponse.builder().build());

        // Act
        repository.save(subscription);

        // Assert
        verify(dynamoDbClient).putItem(argThat((PutItemRequest request) -> {
            Map<String, AttributeValue> item = request.item();
            return item.get("subscriptionId").s().equals(subscriptionId) &&
                    item.get("customerId").s().equals(customerId) &&
                    item.get("fundId").n().equals("1") &&
                    item.get("fundName").s().equals(fundName) &&
                    item.get("amount").n().equals(amount.toPlainString()) &&
                    item.get("status").s().equals("ACTIVE");
        }));
    }

    // ============= findById Tests =============

    @Test
    @DisplayName("findById should return subscription when found")
    void testFindByIdWhenFound() {
        // Arrange
        String subscriptionId = "SUB-UUID";
        GetItemResponse mockResponse = GetItemResponse.builder()
                .item(createSubscriptionDynamoItem(subscriptionId, SubscriptionStatus.ACTIVE, null))
                .build();

        when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(mockResponse);

        // Act
        Optional<Subscription> result = repository.findById(subscriptionId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(subscriptionId, result.get().getId());
        assertEquals(SubscriptionStatus.ACTIVE, result.get().getStatus());
        verify(dynamoDbClient, times(1)).getItem(any(GetItemRequest.class));
    }

    @Test
    @DisplayName("findById should return empty Optional when not found")
    void testFindByIdWhenNotFound() {
        // Arrange
        String subscriptionId = "SUB-NOTFOUND";
        GetItemResponse mockResponse = GetItemResponse.builder().build();

        when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(mockResponse);

        // Act
        Optional<Subscription> result = repository.findById(subscriptionId);

        // Assert
        assertTrue(result.isEmpty());
        verify(dynamoDbClient, times(1)).getItem(any(GetItemRequest.class));
    }

    @Test
    @DisplayName("findById should return empty Optional on ResourceNotFoundException")
    void testFindByIdOnResourceNotFoundException() {
        // Arrange
        String subscriptionId = "SUB-UUID";
        when(dynamoDbClient.getItem(any(GetItemRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());

        // Act
        Optional<Subscription> result = repository.findById(subscriptionId);

        // Assert
        assertTrue(result.isEmpty());
        verify(dynamoDbClient, times(1)).getItem(any(GetItemRequest.class));
    }

    @Test
    @DisplayName("findById should map subscription with cancelledAt correctly")
    void testFindByIdMapsSubscriptionWithCancelledAt() {
        // Arrange
        String subscriptionId = "SUB-UUID";
        OffsetDateTime cancelledAt = OffsetDateTime.now();
        GetItemResponse mockResponse = GetItemResponse.builder()
                .item(createSubscriptionDynamoItem(subscriptionId, SubscriptionStatus.CANCELLED, cancelledAt))
                .build();

        when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(mockResponse);

        // Act
        Optional<Subscription> result = repository.findById(subscriptionId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(subscriptionId, result.get().getId());
        assertEquals(SubscriptionStatus.CANCELLED, result.get().getStatus());
        assertNotNull(result.get().getCancelledAt());
        verify(dynamoDbClient, times(1)).getItem(any(GetItemRequest.class));
    }

    // ============= Mapping Tests =============

    @Test
    @DisplayName("mapFromDynamodb should correctly map active subscription")
    void testMapFromDynamodbActiveSubscription() {
        // Arrange
        String subscriptionId = "SUB-UUID";
        String customerId = "CUST-001";
        Integer fundId = 1;
        String fundName = "FPV_BTG_PACTUAL_RECAUDADORA";
        BigDecimal amount = new BigDecimal("75000");
        OffsetDateTime createdAt = OffsetDateTime.now();

        GetItemResponse mockResponse = GetItemResponse.builder()
                .item(Map.of(
                        "subscriptionId", AttributeValue.builder().s(subscriptionId).build(),
                        "customerId", AttributeValue.builder().s(customerId).build(),
                        "fundId", AttributeValue.builder().n(fundId.toString()).build(),
                        "fundName", AttributeValue.builder().s(fundName).build(),
                        "amount", AttributeValue.builder().n(amount.toPlainString()).build(),
                        "createdAt", AttributeValue.builder().s(createdAt.toString()).build(),
                        "status", AttributeValue.builder().s(SubscriptionStatus.ACTIVE.toString()).build()
                ))
                .build();

        when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(mockResponse);

        // Act
        Optional<Subscription> result = repository.findById(subscriptionId);

        // Assert
        assertTrue(result.isPresent());
        Subscription subscription = result.get();
        assertEquals(subscriptionId, subscription.getId());
        assertEquals(customerId, subscription.getCustomerId());
        assertEquals(fundId, subscription.getFundId());
        assertEquals(fundName, subscription.getFundName());
        assertEquals(0, amount.compareTo(subscription.getAmount()));
        assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
    }

    @Test
    @DisplayName("mapToDynamodb should correctly map subscription with all fields")
    void testMapToDynamodbCorrectly() {
        // Arrange
        Subscription subscription = createTestSubscription(SubscriptionStatus.ACTIVE, null);
        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenReturn(PutItemResponse.builder().build());

        // Act
        repository.save(subscription);

        // Assert
        verify(dynamoDbClient).putItem(argThat((PutItemRequest request) -> {
            Map<String, AttributeValue> item = request.item();
            return item.containsKey("subscriptionId") &&
                    item.containsKey("customerId") &&
                    item.containsKey("fundId") &&
                    item.containsKey("fundName") &&
                    item.containsKey("amount") &&
                    item.containsKey("createdAt") &&
                    item.containsKey("status");
        }));
    }

    // ============= Helper Methods =============

    private Subscription createTestSubscription(SubscriptionStatus status, OffsetDateTime cancelledAt) {
        Subscription subscription = new Subscription(
                "SUB-UUID",
                "CUST-001",
                1,
                "FPV_BTG_PACTUAL_RECAUDADORA",
                new BigDecimal("75000"),
                OffsetDateTime.now(),
                status
        );
        if (cancelledAt != null) {
            subscription.setCancelledAt(cancelledAt);
        }
        return subscription;
    }

    private Map<String, AttributeValue> createSubscriptionDynamoItem(
            String subscriptionId, SubscriptionStatus status, OffsetDateTime cancelledAt) {
        Map<String, AttributeValue> item = new java.util.HashMap<>();
        item.put("subscriptionId", AttributeValue.builder().s(subscriptionId).build());
        item.put("customerId", AttributeValue.builder().s("CUST-001").build());
        item.put("fundId", AttributeValue.builder().n("1").build());
        item.put("fundName", AttributeValue.builder().s("FPV_BTG_PACTUAL_RECAUDADORA").build());
        item.put("amount", AttributeValue.builder().n("75000").build());
        item.put("createdAt", AttributeValue.builder().s(OffsetDateTime.now().toString()).build());
        item.put("status", AttributeValue.builder().s(status.toString()).build());
        
        if (cancelledAt != null) {
            item.put("cancelledAt", AttributeValue.builder().s(cancelledAt.toString()).build());
        }
        
        return item;
    }
}


