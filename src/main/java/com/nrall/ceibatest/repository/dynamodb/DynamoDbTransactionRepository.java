package com.nrall.ceibatest.repository.dynamodb;

import com.nrall.ceibatest.domain.model.NotificationChannel;
import com.nrall.ceibatest.domain.model.Transaction;
import com.nrall.ceibatest.domain.model.TransactionType;
import com.nrall.ceibatest.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class DynamoDbTransactionRepository implements TransactionRepository {

    private final DynamoDbClient dynamoDbClient;

    @Value("${aws.dynamodb.table.transaction:transaction_prod}")
    private String tableName;

    @Override
    public Transaction save(Transaction transaction) {
        Map<String, AttributeValue> item = mapToDynamodb(transaction);

        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build();

        dynamoDbClient.putItem(request);
        return transaction;
    }

    @Override
    public List<Transaction> findByCustomerId(String customerId) {
        try {
            QueryRequest request = QueryRequest.builder()
                    .tableName(tableName)
                    .keyConditionExpression("customerId = :customerId")
                    .expressionAttributeValues(Map.of(
                            ":customerId", AttributeValue.builder().s(customerId).build()
                    ))
                    .scanIndexForward(false)  // Ordenar descendente por createdAtTransactionId
                    .build();

            QueryResponse response = dynamoDbClient.query(request);
            List<Transaction> transactions = new ArrayList<>();

            if (response.hasItems()) {
                response.items().stream()
                        .sorted(Comparator.comparing((Map<String, AttributeValue> item) -> item.get("createdAt").s(), String::compareTo).reversed())
                        .forEach(item -> transactions.add(mapFromDynamodb(item)));
            }

            return transactions;
        } catch (ResourceNotFoundException e) {
            return Collections.emptyList();
        }
    }

    private Transaction mapFromDynamodb(Map<String, AttributeValue> item) {
        String id = item.get("transactionId").s();
        String customerId = item.get("customerId").s();
        String subscriptionId = item.get("subscriptionId").s();
        Integer fundId = Integer.parseInt(item.get("fundId").n());
        String fundName = item.get("fundName").s();
        TransactionType type = TransactionType.valueOf(item.get("type").s());
        BigDecimal amount = new BigDecimal(item.get("amount").n());
        NotificationChannel notificationChannel = null;
        if (item.containsKey("notificationChannel") && item.get("notificationChannel").s() != null) {
            notificationChannel = NotificationChannel.valueOf(item.get("notificationChannel").s());
        }
        OffsetDateTime createdAt = OffsetDateTime.parse(item.get("createdAt").s());

        return new Transaction(id, customerId, subscriptionId, fundId, fundName, type, amount, notificationChannel, createdAt);
    }

    private Map<String, AttributeValue> mapToDynamodb(Transaction transaction) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("transactionId", AttributeValue.builder().s(transaction.getId()).build());
        item.put("customerId", AttributeValue.builder().s(transaction.getCustomerId()).build());
        item.put("createdAtTransactionId", AttributeValue.builder()
                .s(transaction.getCreatedAt().toString() + "#" + transaction.getId())
                .build());
        item.put("subscriptionId", AttributeValue.builder().s(transaction.getSubscriptionId()).build());
        item.put("fundId", AttributeValue.builder().n(transaction.getFundId().toString()).build());
        item.put("fundName", AttributeValue.builder().s(transaction.getFundName()).build());
        item.put("type", AttributeValue.builder().s(transaction.getType().toString()).build());
        item.put("amount", AttributeValue.builder().n(transaction.getAmount().toPlainString()).build());
        if (transaction.getNotificationChannel() != null) {
            item.put("notificationChannel", AttributeValue.builder().s(transaction.getNotificationChannel().toString()).build());
        }
        item.put("createdAt", AttributeValue.builder().s(transaction.getCreatedAt().toString()).build());

        return item;
    }
}

