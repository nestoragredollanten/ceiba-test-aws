package com.nrall.ceibatest.repository.dynamodb;

import com.nrall.ceibatest.domain.model.Subscription;
import com.nrall.ceibatest.domain.model.SubscriptionStatus;
import com.nrall.ceibatest.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DynamoDbSubscriptionRepository implements SubscriptionRepository {

    private final DynamoDbClient dynamoDbClient;

    @Value("${aws.dynamodb.table.subscription:subscription_prod}")
    private String tableName;

    @Override
    public Subscription save(Subscription subscription) {
        Map<String, AttributeValue> item = mapToDynamodb(subscription);

        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build();

        dynamoDbClient.putItem(request);
        return subscription;
    }

    @Override
    public Optional<Subscription> findById(String id) {
        try {
            GetItemRequest request = GetItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of("subscriptionId", AttributeValue.builder().s(id).build()))
                    .build();

            GetItemResponse response = dynamoDbClient.getItem(request);

            if (response.hasItem() && !response.item().isEmpty()) {
                return Optional.of(mapFromDynamodb(response.item()));
            }
            return Optional.empty();
        } catch (ResourceNotFoundException e) {
            return Optional.empty();
        }
    }

    private Subscription mapFromDynamodb(Map<String, AttributeValue> item) {
        String id = item.get("subscriptionId").s();
        String customerId = item.get("customerId").s();
        Integer fundId = Integer.parseInt(item.get("fundId").n());
        String fundName = item.get("fundName").s();
        BigDecimal amount = new BigDecimal(item.get("amount").n());
        OffsetDateTime createdAt = OffsetDateTime.parse(item.get("createdAt").s());
        SubscriptionStatus status = SubscriptionStatus.valueOf(item.get("status").s());

        Subscription subscription = new Subscription(id, customerId, fundId, fundName, amount, createdAt, status);

        if (item.containsKey("cancelledAt") && item.get("cancelledAt").s() != null) {
            subscription.setCancelledAt(OffsetDateTime.parse(item.get("cancelledAt").s()));
        }

        return subscription;
    }

    private Map<String, AttributeValue> mapToDynamodb(Subscription subscription) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("subscriptionId", AttributeValue.builder().s(subscription.getId()).build());
        item.put("customerId", AttributeValue.builder().s(subscription.getCustomerId()).build());
        item.put("fundId", AttributeValue.builder().n(subscription.getFundId().toString()).build());
        item.put("fundName", AttributeValue.builder().s(subscription.getFundName()).build());
        item.put("amount", AttributeValue.builder().n(subscription.getAmount().toPlainString()).build());
        item.put("createdAt", AttributeValue.builder().s(subscription.getCreatedAt().toString()).build());
        item.put("status", AttributeValue.builder().s(subscription.getStatus().toString()).build());

        if (subscription.getCancelledAt() != null) {
            item.put("cancelledAt", AttributeValue.builder().s(subscription.getCancelledAt().toString()).build());
        }

        return item;
    }
}

