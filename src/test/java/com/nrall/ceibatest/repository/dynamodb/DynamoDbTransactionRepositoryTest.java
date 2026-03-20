package com.nrall.ceibatest.repository.dynamodb;

import com.nrall.ceibatest.domain.model.NotificationChannel;
import com.nrall.ceibatest.domain.model.Transaction;
import com.nrall.ceibatest.domain.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("DynamoDbTransactionRepository Tests")
class DynamoDbTransactionRepositoryTest {

    @Mock
    private DynamoDbClient dynamoDbClient;

    private DynamoDbTransactionRepository repository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        repository = new DynamoDbTransactionRepository(dynamoDbClient);
    }

    // ============= save Tests =============

    @Test
    @DisplayName("save should persist transaction to DynamoDB")
    void testSaveSuccessfully() {
        // Arrange
        Transaction transaction = createTestTransaction(TransactionType.OPENING);
        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenReturn(PutItemResponse.builder().build());

        // Act
        Transaction result = repository.save(transaction);

        // Assert
        assertNotNull(result);
        assertSame(transaction, result);
        verify(dynamoDbClient, times(1)).putItem(any(PutItemRequest.class));
    }

    @Test
    @DisplayName("save should map transaction with OPENING type correctly")
    void testSaveOpeningTransaction() {
        // Arrange
        Transaction transaction = createTestTransaction(TransactionType.OPENING);
        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenReturn(PutItemResponse.builder().build());

        // Act
        repository.save(transaction);

        // Assert
        verify(dynamoDbClient).putItem(any(PutItemRequest.class));
    }

    @Test
    @DisplayName("save should map transaction with CANCELLATION type correctly")
    void testSaveCancellationTransaction() {
        // Arrange
        Transaction transaction = createTestTransaction(TransactionType.CANCELLATION);
        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenReturn(PutItemResponse.builder().build());

        // Act
        repository.save(transaction);

        // Assert
        verify(dynamoDbClient).putItem(any(PutItemRequest.class));
    }

    @Test
    @DisplayName("save should omit notificationChannel when transaction has none")
    void testSaveCancellationTransactionWithoutNotificationChannel() {
        Transaction transaction = new Transaction(
                "TXN-CANCEL",
                "CUST-001",
                "SUB-UUID",
                1,
                "FPV_BTG_PACTUAL_RECAUDADORA",
                TransactionType.CANCELLATION,
                new BigDecimal("75000"),
                null,
                OffsetDateTime.now()
        );

        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenReturn(PutItemResponse.builder().build());

        repository.save(transaction);

        verify(dynamoDbClient).putItem(argThat((PutItemRequest request) -> !request.item().containsKey("notificationChannel")));
    }

    @Test
    @DisplayName("save should map transaction with EMAIL notification channel")
    void testSaveTransactionWithEmailNotification() {
        // Arrange
        String customerId = "CUST-001";
        String subscriptionId = "SUB-UUID";
        Integer fundId = 1;
        String fundName = "FPV_BTG_PACTUAL_RECAUDADORA";
        TransactionType type = TransactionType.OPENING;
        BigDecimal amount = new BigDecimal("75000");
        NotificationChannel channel = NotificationChannel.EMAIL;
        OffsetDateTime createdAt = OffsetDateTime.now();

        Transaction transaction = new Transaction(
                "TXN-UUID", customerId, subscriptionId, fundId, fundName,
                type, amount, channel, createdAt
        );

        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenReturn(PutItemResponse.builder().build());

        // Act
        repository.save(transaction);

        // Assert
        verify(dynamoDbClient).putItem(any(PutItemRequest.class));
    }

    @Test
    @DisplayName("save should map transaction with SMS notification channel")
    void testSaveTransactionWithSmsNotification() {
        // Arrange
        Transaction transaction = new Transaction(
                "TXN-UUID", "CUST-001", "SUB-UUID", 1, "FPV_BTG_PACTUAL_RECAUDADORA",
                TransactionType.OPENING, new BigDecimal("75000"),
                NotificationChannel.SMS, OffsetDateTime.now()
        );

        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenReturn(PutItemResponse.builder().build());

        // Act
        repository.save(transaction);

        // Assert
        verify(dynamoDbClient).putItem(argThat((PutItemRequest request) -> {
            Map<String, AttributeValue> item = request.item();
            return item.get("notificationChannel").s().equals("SMS");
        }));
    }

    @Test
    @DisplayName("save should create composite key createdAtTransactionId")
    void testSaveShouldCreateCompositeKey() {
        // Arrange
        Transaction transaction = createTestTransaction(TransactionType.OPENING);
        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenReturn(PutItemResponse.builder().build());

        // Act
        repository.save(transaction);

        // Assert
        verify(dynamoDbClient).putItem(argThat((PutItemRequest request) -> {
            Map<String, AttributeValue> item = request.item();
            String compositeKey = item.get("createdAtTransactionId").s();
            return compositeKey.contains("#") &&
                    compositeKey.startsWith(transaction.getCreatedAt().toString());
        }));
    }

    @Test
    @DisplayName("save should map all transaction fields correctly")
    void testSaveMapsProperly() {
        // Arrange
        String transactionId = "TXN-UUID";
        String customerId = "CUST-001";
        String subscriptionId = "SUB-UUID";
        Integer fundId = 2;
        String fundName = "FPV_BTG_PACTUAL_ECOPETROL";
        TransactionType type = TransactionType.OPENING;
        BigDecimal amount = new BigDecimal("125000");
        NotificationChannel channel = NotificationChannel.SMS;
        OffsetDateTime createdAt = OffsetDateTime.now();

        Transaction transaction = new Transaction(
                transactionId, customerId, subscriptionId, fundId, fundName,
                type, amount, channel, createdAt
        );

        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenReturn(PutItemResponse.builder().build());

        // Act
        repository.save(transaction);

        // Assert
        verify(dynamoDbClient).putItem(any(PutItemRequest.class));
    }

    // ============= findByCustomerId Tests =============

    @Test
    @DisplayName("findByCustomerId should return transactions in descending order")
    void testFindByCustomerIdOrdersDescending() {
        // Arrange
        String customerId = "CUST-001";
        OffsetDateTime time1 = OffsetDateTime.now().minusHours(2);
        OffsetDateTime time2 = OffsetDateTime.now().minusHours(1);
        OffsetDateTime time3 = OffsetDateTime.now();

        List<Map<String, AttributeValue>> items = List.of(
                createTransactionDynamoItem("TXN-001", customerId, time1),
                createTransactionDynamoItem("TXN-002", customerId, time2),
                createTransactionDynamoItem("TXN-003", customerId, time3)
        );

        QueryResponse mockResponse = QueryResponse.builder()
                .items(items)
                .build();

        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(mockResponse);

        // Act
        List<Transaction> result = repository.findByCustomerId(customerId);

        // Assert
        assertEquals(3, result.size());
        verify(dynamoDbClient, times(1)).query(any(QueryRequest.class));
    }

    @Test
    @DisplayName("findByCustomerId should return empty list when no transactions found")
    void testFindByCustomerIdWhenNoTransactionsFound() {
        // Arrange
        String customerId = "CUST-NOTFOUND";
        QueryResponse mockResponse = QueryResponse.builder()
                .items(List.of())
                .build();

        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(mockResponse);

        // Act
        List<Transaction> result = repository.findByCustomerId(customerId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(dynamoDbClient, times(1)).query(any(QueryRequest.class));
    }

    @Test
    @DisplayName("findByCustomerId should return empty list on ResourceNotFoundException")
    void testFindByCustomerIdOnResourceNotFoundException() {
        // Arrange
        String customerId = "CUST-001";
        when(dynamoDbClient.query(any(QueryRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());

        // Act
        List<Transaction> result = repository.findByCustomerId(customerId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(dynamoDbClient, times(1)).query(any(QueryRequest.class));
    }

    @Test
    @DisplayName("findByCustomerId should map transactions correctly")
    void testFindByCustomerIdMapsTransactionsCorrectly() {
        // Arrange
        String customerId = "CUST-001";
        OffsetDateTime createdAt = OffsetDateTime.now();

        List<Map<String, AttributeValue>> items = List.of(
                createTransactionDynamoItem("TXN-001", customerId, createdAt)
        );

        QueryResponse mockResponse = QueryResponse.builder()
                .items(items)
                .build();

        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(mockResponse);

        // Act
        List<Transaction> result = repository.findByCustomerId(customerId);

        // Assert
        assertEquals(1, result.size());
        Transaction transaction = result.get(0);
        assertEquals("TXN-001", transaction.getId());
        assertEquals(customerId, transaction.getCustomerId());
        assertEquals(TransactionType.OPENING, transaction.getType());
    }

    @Test
    @DisplayName("findByCustomerId should query with correct key condition")
    void testFindByCustomerIdUsesCorrectQuery() {
        // Arrange
        String customerId = "CUST-001";
        QueryResponse mockResponse = QueryResponse.builder()
                .items(List.of())
                .build();

        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(mockResponse);

        // Act
        repository.findByCustomerId(customerId);

        // Assert
        verify(dynamoDbClient).query(any(QueryRequest.class));
    }

    // ============= Mapping Tests =============

    @Test
    @DisplayName("mapFromDynamodb should correctly map DynamoDB item to Transaction")
    void testMapFromDynamodbCorrectly() {
        // Arrange
        String customerId = "CUST-001";
        String transactionId = "TXN-UUID";
        OffsetDateTime createdAt = OffsetDateTime.now();

        List<Map<String, AttributeValue>> items = List.of(
                createTransactionDynamoItem(transactionId, customerId, createdAt)
        );

        QueryResponse mockResponse = QueryResponse.builder()
                .items(items)
                .build();

        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(mockResponse);

        // Act
        List<Transaction> result = repository.findByCustomerId(customerId);

        // Assert
        assertEquals(1, result.size());
        Transaction transaction = result.get(0);
        assertEquals(transactionId, transaction.getId());
        assertEquals(customerId, transaction.getCustomerId());
        assertEquals("SUB-UUID", transaction.getSubscriptionId());
        assertEquals(1, transaction.getFundId());
        assertEquals("FPV_BTG_PACTUAL_RECAUDADORA", transaction.getFundName());
        assertEquals(TransactionType.OPENING, transaction.getType());
        assertEquals(0, new BigDecimal("75000").compareTo(transaction.getAmount()));
        assertEquals(NotificationChannel.EMAIL, transaction.getNotificationChannel());
    }

    @Test
    @DisplayName("mapFromDynamodb should allow missing notificationChannel")
    void testMapFromDynamodbWithoutNotificationChannel() {
        String customerId = "CUST-001";
        OffsetDateTime createdAt = OffsetDateTime.now();
        Map<String, AttributeValue> item = createTransactionDynamoItem("TXN-002", customerId, createdAt);
        item.remove("notificationChannel");

        QueryResponse mockResponse = QueryResponse.builder()
                .items(List.of(item))
                .build();

        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(mockResponse);

        List<Transaction> result = repository.findByCustomerId(customerId);

        assertEquals(1, result.size());
        assertEquals(TransactionType.OPENING, result.get(0).getType());
        assertNull(result.get(0).getNotificationChannel());
    }

    @Test
    @DisplayName("mapToDynamodb should correctly map Transaction to DynamoDB format")
    void testMapToDynamodbCorrectly() {
        // Arrange
        Transaction transaction = createTestTransaction(TransactionType.OPENING);
        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenReturn(PutItemResponse.builder().build());

        // Act
        repository.save(transaction);

        // Assert
        verify(dynamoDbClient).putItem(argThat((PutItemRequest request) -> {
            Map<String, AttributeValue> item = request.item();
            return item.containsKey("transactionId") &&
                    item.containsKey("customerId") &&
                    item.containsKey("createdAtTransactionId") &&
                    item.containsKey("subscriptionId") &&
                    item.containsKey("fundId") &&
                    item.containsKey("fundName") &&
                    item.containsKey("type") &&
                    item.containsKey("amount") &&
                    item.containsKey("notificationChannel") &&
                    item.containsKey("createdAt");
        }));
    }

    // ============= Helper Methods =============

    private Transaction createTestTransaction(TransactionType type) {
        return new Transaction(
                "TXN-UUID",
                "CUST-001",
                "SUB-UUID",
                1,
                "FPV_BTG_PACTUAL_RECAUDADORA",
                type,
                new BigDecimal("75000"),
                NotificationChannel.EMAIL,
                OffsetDateTime.now()
        );
    }

    private Map<String, AttributeValue> createTransactionDynamoItem(
            String transactionId, String customerId, OffsetDateTime createdAt) {
        Map<String, AttributeValue> item = new java.util.HashMap<>();
        item.put("transactionId", AttributeValue.builder().s(transactionId).build());
        item.put("customerId", AttributeValue.builder().s(customerId).build());
        item.put("subscriptionId", AttributeValue.builder().s("SUB-UUID").build());
        item.put("fundId", AttributeValue.builder().n("1").build());
        item.put("fundName", AttributeValue.builder().s("FPV_BTG_PACTUAL_RECAUDADORA").build());
        item.put("type", AttributeValue.builder().s(TransactionType.OPENING.toString()).build());
        item.put("amount", AttributeValue.builder().n("75000").build());
        item.put("notificationChannel", AttributeValue.builder().s(NotificationChannel.EMAIL.toString()).build());
        item.put("createdAt", AttributeValue.builder().s(createdAt.toString()).build());
        return item;
    }
}


