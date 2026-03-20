package com.nrall.ceibatest.repository.dynamodb;

import com.nrall.ceibatest.domain.model.CustomerAccount;
import com.nrall.ceibatest.repository.CustomerAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DynamoDbCustomerAccountRepository implements CustomerAccountRepository {

    private final DynamoDbClient dynamoDbClient;

    @Value("${aws.dynamodb.table.customer:customer_account_prod}")
    private String tableName;

    @Override
    public Optional<CustomerAccount> findByCustomerId(String customerId) {
        try {
            GetItemRequest request = GetItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of("customerId", AttributeValue.builder().s(customerId).build()))
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

    @Override
    public CustomerAccount createIfAbsent(String customerId, BigDecimal initialBalance) {
        Optional<CustomerAccount> existing = findByCustomerId(customerId);
        if (existing.isPresent()) {
            return existing.get();
        }

        CustomerAccount newAccount = new CustomerAccount(customerId, initialBalance);
        return save(newAccount);
    }

    @Override
    public CustomerAccount save(CustomerAccount account) {
        Map<String, AttributeValue> item = mapToDynamodb(account);

        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build();

        dynamoDbClient.putItem(request);
        return account;
    }

    private CustomerAccount mapFromDynamodb(Map<String, AttributeValue> item) {
        String customerId = item.get("customerId").s();
        BigDecimal balance = new BigDecimal(item.get("balance").n());
        return new CustomerAccount(customerId, balance);
    }

    private Map<String, AttributeValue> mapToDynamodb(CustomerAccount account) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("customerId", AttributeValue.builder().s(account.getCustomerId()).build());
        item.put("balance", AttributeValue.builder().n(account.getBalance().toPlainString()).build());
        return item;
    }
}

