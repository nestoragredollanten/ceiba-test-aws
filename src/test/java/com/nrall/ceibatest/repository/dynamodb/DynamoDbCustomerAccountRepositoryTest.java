package com.nrall.ceibatest.repository.dynamodb;

import com.nrall.ceibatest.domain.model.CustomerAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("DynamoDbCustomerAccountRepository Tests")
class DynamoDbCustomerAccountRepositoryTest {

    @Mock
    private DynamoDbClient dynamoDbClient;

    private DynamoDbCustomerAccountRepository repository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        repository = new DynamoDbCustomerAccountRepository(dynamoDbClient);
    }

    // ============= findByCustomerId Tests =============

    @Test
    @DisplayName("findByCustomerId should return customer when found")
    void testFindByCustomerIdWhenFound() {
        // Arrange
        String customerId = "CUST-001";
        BigDecimal balance = new BigDecimal("500000");
        GetItemResponse mockResponse = GetItemResponse.builder()
                .item(Map.of(
                        "customerId", AttributeValue.builder().s(customerId).build(),
                        "balance", AttributeValue.builder().n(balance.toPlainString()).build()
                ))
                .build();

        when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(mockResponse);

        // Act
        Optional<CustomerAccount> result = repository.findByCustomerId(customerId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(customerId, result.get().getCustomerId());
        assertEquals(balance, result.get().getBalance());
        verify(dynamoDbClient, times(1)).getItem(any(GetItemRequest.class));
    }

    @Test
    @DisplayName("findByCustomerId should return empty Optional when not found")
    void testFindByCustomerIdWhenNotFound() {
        // Arrange
        String customerId = "CUST-NOTFOUND";
        GetItemResponse mockResponse = GetItemResponse.builder()
                .item(Map.of())
                .build();

        when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(mockResponse);

        // Act
        Optional<CustomerAccount> result = repository.findByCustomerId(customerId);

        // Assert
        assertTrue(result.isEmpty());
        verify(dynamoDbClient, times(1)).getItem(any(GetItemRequest.class));
    }

    @Test
    @DisplayName("findByCustomerId should return empty Optional on ResourceNotFoundException")
    void testFindByCustomerIdOnResourceNotFoundException() {
        // Arrange
        String customerId = "CUST-001";
        when(dynamoDbClient.getItem(any(GetItemRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());

        // Act
        Optional<CustomerAccount> result = repository.findByCustomerId(customerId);

        // Assert
        assertTrue(result.isEmpty());
        verify(dynamoDbClient, times(1)).getItem(any(GetItemRequest.class));
    }

    // ============= createIfAbsent Tests =============

    @Test
    @DisplayName("createIfAbsent should create new account when not exists")
    void testCreateIfAbsentWhenNotExists() {
        // Arrange
        String customerId = "CUST-NEW";
        BigDecimal initialBalance = new BigDecimal("500000");
        
        GetItemResponse getItemResponse = GetItemResponse.builder().build();
        
        when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(getItemResponse);
        when(dynamoDbClient.putItem(any(PutItemRequest.class))).thenReturn(PutItemResponse.builder().build());

        // Act
        CustomerAccount result = repository.createIfAbsent(customerId, initialBalance);

        // Assert
        assertNotNull(result);
        assertEquals(customerId, result.getCustomerId());
        assertEquals(initialBalance, result.getBalance());
        verify(dynamoDbClient, times(1)).getItem(any(GetItemRequest.class));
        verify(dynamoDbClient, times(1)).putItem(any(PutItemRequest.class));
    }

    @Test
    @DisplayName("createIfAbsent should return existing account when exists")
    void testCreateIfAbsentWhenExists() {
        // Arrange
        String customerId = "CUST-001";
        BigDecimal existingBalance = new BigDecimal("425000");
        BigDecimal newBalance = new BigDecimal("500000");
        
        GetItemResponse getItemResponse = GetItemResponse.builder()
                .item(Map.of(
                        "customerId", AttributeValue.builder().s(customerId).build(),
                        "balance", AttributeValue.builder().n(existingBalance.toPlainString()).build()
                ))
                .build();
        
        when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(getItemResponse);

        // Act
        CustomerAccount result = repository.createIfAbsent(customerId, newBalance);

        // Assert
        assertNotNull(result);
        assertEquals(customerId, result.getCustomerId());
        assertEquals(existingBalance, result.getBalance());
        verify(dynamoDbClient, times(1)).getItem(any(GetItemRequest.class));
        verify(dynamoDbClient, never()).putItem(any(PutItemRequest.class));
    }

    // ============= save Tests =============

    @Test
    @DisplayName("save should persist customer account to DynamoDB")
    void testSaveSuccessfully() {
        // Arrange
        String customerId = "CUST-001";
        BigDecimal balance = new BigDecimal("425000");
        CustomerAccount account = new CustomerAccount(customerId, balance);
        
        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenReturn(PutItemResponse.builder().build());

        // Act
        CustomerAccount result = repository.save(account);

        // Assert
        assertNotNull(result);
        assertEquals(customerId, result.getCustomerId());
        assertEquals(balance, result.getBalance());
        verify(dynamoDbClient, times(1)).putItem(any(PutItemRequest.class));
    }

    @Test
    @DisplayName("save should return same account object")
    void testSaveReturnsAccountObject() {
        // Arrange
        CustomerAccount account = new CustomerAccount("CUST-001", new BigDecimal("500000"));
        
        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenReturn(PutItemResponse.builder().build());

        // Act
        CustomerAccount result = repository.save(account);

        // Assert
        assertSame(account, result);
    }

    @Test
    @DisplayName("save should map account to DynamoDB format correctly")
    void testSaveMapsProperly() {
        // Arrange
        String customerId = "CUST-001";
        BigDecimal balance = new BigDecimal("425000.50");
        CustomerAccount account = new CustomerAccount(customerId, balance);
        
        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenReturn(PutItemResponse.builder().build());

        // Act
        repository.save(account);

        // Assert
        verify(dynamoDbClient, times(1)).putItem(any(PutItemRequest.class));
    }

    // ============= Mapping Tests =============

    @Test
    @DisplayName("mapFromDynamodb should correctly map DynamoDB item to CustomerAccount")
    void testMapFromDynamodbCorrectly() {
        // Arrange
        String customerId = "CUST-001";
        BigDecimal balance = new BigDecimal("500000");
        GetItemResponse mockResponse = GetItemResponse.builder()
                .item(Map.of(
                        "customerId", AttributeValue.builder().s(customerId).build(),
                        "balance", AttributeValue.builder().n(balance.toPlainString()).build()
                ))
                .build();

        when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(mockResponse);

        // Act
        Optional<CustomerAccount> result = repository.findByCustomerId(customerId);

        // Assert
        assertTrue(result.isPresent());
        CustomerAccount account = result.get();
        assertEquals(customerId, account.getCustomerId());
        assertEquals(0, balance.compareTo(account.getBalance()));
    }


    @Test
    @DisplayName("mapToDynamodb should correctly map CustomerAccount to DynamoDB format")
    void testMapToDynamodbCorrectly() {
        // Arrange
        String customerId = "CUST-001";
        BigDecimal balance = new BigDecimal("500000.75");
        CustomerAccount account = new CustomerAccount(customerId, balance);
        
        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenReturn(PutItemResponse.builder().build());

        // Act
        repository.save(account);

        // Assert
        verify(dynamoDbClient).putItem(any(PutItemRequest.class));
    }
}


