package com.rulesengine.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rulesengine.mapper.FieldMapper;
import com.rulesengine.mapper.FieldMappingUtils;
import com.rulesengine.exception.FieldMappingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests demonstrating FieldMapper usage with realistic data scenarios
 * that would be encountered when processing GraphQL and REST API responses.
 */
@SpringBootTest
@ActiveProfiles("test")
class FieldMappingIntegrationTest {

    private FieldMapper fieldMapper;
    private FieldMappingUtils fieldMappingUtils;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        fieldMapper = new FieldMapper(objectMapper);
        fieldMappingUtils = new FieldMappingUtils(fieldMapper);
    }

    @Test
    void testGraphQLUserResponseMapping() throws Exception {
        // Given - Simulated GraphQL response for user data
        String graphqlResponse = """
            {
              "data": {
                "user": {
                  "id": "123",
                  "profile": {
                    "firstName": "John",
                    "lastName": "Doe",
                    "email": "john.doe@example.com",
                    "address": {
                      "street": "123 Main St",
                      "city": "Springfield",
                      "zipCode": "12345"
                    }
                  },
                  "orders": [
                    {
                      "id": "order-1",
                      "amount": 99.99,
                      "status": "completed",
                      "items": [
                        {"name": "Product A", "quantity": 2},
                        {"name": "Product B", "quantity": 1}
                      ]
                    },
                    {
                      "id": "order-2",
                      "amount": 149.50,
                      "status": "pending",
                      "items": [
                        {"name": "Product C", "quantity": 3}
                      ]
                    }
                  ]
                }
              }
            }
            """;
        
        Object response = objectMapper.readValue(graphqlResponse, Object.class);
        
        // When & Then - Test various mapping expressions
        assertEquals("123", fieldMapper.extractValue(response, "data.user.id"));
        assertEquals("John", fieldMapper.extractValue(response, "data.user.profile.firstName"));
        assertEquals("john.doe@example.com", fieldMapper.extractValue(response, "data.user.profile.email"));
        assertEquals("Springfield", fieldMapper.extractValue(response, "data.user.profile.address.city"));
        
        // Array indexing
        assertEquals("order-1", fieldMapper.extractValue(response, "data.user.orders[0].id"));
        assertEquals(99.99, fieldMapper.extractValue(response, "data.user.orders[0].amount"));
        assertEquals("Product A", fieldMapper.extractValue(response, "data.user.orders[0].items[0].name"));
        
        // Array filtering
        assertEquals("order-2", fieldMapper.extractValue(response, "data.user.orders[status=pending].id"));
        assertEquals(149.50, fieldMapper.extractValue(response, "data.user.orders[status=pending].amount"));
    }

    @Test
    void testRESTAPIResponseMapping() throws Exception {
        // Given - Simulated REST API response for customer data
        String restResponse = """
            {
              "customer": {
                "customerId": "CUST-456",
                "personalInfo": {
                  "fullName": "Jane Smith",
                  "dateOfBirth": "1990-05-15",
                  "phoneNumber": "+1-555-0123"
                },
                "accounts": [
                  {
                    "accountNumber": "ACC-001",
                    "accountType": "checking",
                    "balance": 2500.75,
                    "isActive": true
                  },
                  {
                    "accountNumber": "ACC-002",
                    "accountType": "savings",
                    "balance": 15000.00,
                    "isActive": true
                  }
                ],
                "preferences": {
                  "notifications": {
                    "email": true,
                    "sms": false,
                    "push": true
                  }
                }
              }
            }
            """;
        
        Object response = objectMapper.readValue(restResponse, Object.class);
        
        // When & Then - Test field extraction and type conversion
        String customerId = fieldMappingUtils.extractAndConvert(response, "customer.customerId", String.class);
        assertEquals("CUST-456", customerId);
        
        Double balance = fieldMappingUtils.extractAndConvert(response, "customer.accounts[0].balance", Double.class);
        assertEquals(2500.75, balance);
        
        Boolean isActive = fieldMappingUtils.extractAndConvert(response, "customer.accounts[0].isActive", Boolean.class);
        assertTrue(isActive);
        
        // Array filtering with type conversion
        Double savingsBalance = fieldMappingUtils.extractAndConvert(
            response, "customer.accounts[accountType=savings].balance", Double.class);
        assertEquals(15000.00, savingsBalance);
    }

    @Test
    void testMultipleFieldExtraction() throws Exception {
        // Given - Complex nested response
        String complexResponse = """
            {
              "transaction": {
                "id": "TXN-789",
                "timestamp": "2023-12-25T10:30:00",
                "amount": "250.00",
                "currency": "USD",
                "merchant": {
                  "name": "Coffee Shop",
                  "category": "food_and_drink",
                  "location": {
                    "latitude": "40.7128",
                    "longitude": "-74.0060"
                  }
                },
                "paymentMethod": {
                  "type": "credit_card",
                  "lastFourDigits": "1234"
                }
              }
            }
            """;
        
        Object response = objectMapper.readValue(complexResponse, Object.class);
        
        // When - Extract multiple fields at once
        Map<String, String> fieldMappings = Map.of(
            "transactionId", "transaction.id",
            "merchantName", "transaction.merchant.name",
            "paymentType", "transaction.paymentMethod.type",
            "amount", "transaction.amount",
            "latitude", "transaction.merchant.location.latitude"
        );
        
        Map<String, Object> extractedFields = fieldMappingUtils.extractMultipleFields(response, fieldMappings);
        
        // Then
        assertEquals("TXN-789", extractedFields.get("transactionId"));
        assertEquals("Coffee Shop", extractedFields.get("merchantName"));
        assertEquals("credit_card", extractedFields.get("paymentType"));
        assertEquals("250.00", extractedFields.get("amount"));
        assertEquals("40.7128", extractedFields.get("latitude"));
    }

    @Test
    void testErrorHandlingAndRecovery() throws Exception {
        // Given - Response with missing fields
        String incompleteResponse = """
            {
              "user": {
                "id": "123",
                "name": "John Doe"
              }
            }
            """;
        
        Object response = objectMapper.readValue(incompleteResponse, Object.class);
        
        // When & Then - Test graceful handling of missing fields
        Object nameObj = fieldMappingUtils.extractWithDefault(response, "user.name", "Unknown");
        assertEquals("John Doe", nameObj);
        
        Object emailObj = fieldMappingUtils.extractWithDefault(response, "user.email", "no-email@example.com");
        assertEquals("no-email@example.com", emailObj);
        
        // Test validation
        assertTrue(fieldMappingUtils.validateMapperExpression(response, "user.name"));
        assertFalse(fieldMappingUtils.validateMapperExpression(response, "user.email"));
        
        // Test detailed validation
        FieldMappingUtils.ValidationResult validResult = 
            fieldMappingUtils.validateMapperExpressionDetailed(response, "user.name");
        assertTrue(validResult.isValid());
        assertEquals("John Doe", validResult.getExtractedValue());
        
        FieldMappingUtils.ValidationResult invalidResult = 
            fieldMappingUtils.validateMapperExpressionDetailed(response, "user.email");
        assertFalse(invalidResult.isValid());
        assertNotNull(invalidResult.getErrorMessage());
        assertNotNull(invalidResult.getSuggestion());
    }

    @Test
    void testTypeConversionsWithRealWorldData() throws Exception {
        // Given - Response with various data types as strings (common in APIs)
        String apiResponse = """
            {
              "metrics": {
                "totalUsers": "1250",
                "averageAge": "32.5",
                "isActive": "true",
                "lastUpdated": "2023-12-25T10:30:00",
                "conversionRate": "0.0325"
              }
            }
            """;
        
        Object response = objectMapper.readValue(apiResponse, Object.class);
        
        // When & Then - Test automatic type conversions
        Integer totalUsers = fieldMappingUtils.extractAndConvert(response, "metrics.totalUsers", Integer.class);
        assertEquals(1250, totalUsers);
        
        Double averageAge = fieldMappingUtils.extractAndConvert(response, "metrics.averageAge", Double.class);
        assertEquals(32.5, averageAge);
        
        Boolean isActive = fieldMappingUtils.extractAndConvert(response, "metrics.isActive", Boolean.class);
        assertTrue(isActive);
        
        // Test with defaults for conversion failures
        Integer invalidNumber = fieldMappingUtils.extractAndConvertWithDefault(
            response, "metrics.invalidField", Integer.class, 0);
        assertEquals(0, invalidNumber);
    }

    @Test
    void testArrayOperationsWithComplexData() throws Exception {
        // Given - Response with complex array structures
        String arrayResponse = """
            {
              "products": [
                {
                  "id": "PROD-1",
                  "name": "Laptop",
                  "price": 999.99,
                  "categories": ["electronics", "computers"],
                  "reviews": [
                    {"rating": 5, "comment": "Excellent"},
                    {"rating": 4, "comment": "Good value"}
                  ]
                },
                {
                  "id": "PROD-2",
                  "name": "Mouse",
                  "price": 29.99,
                  "categories": ["electronics", "accessories"],
                  "reviews": [
                    {"rating": 3, "comment": "Average"}
                  ]
                }
              ]
            }
            """;
        
        Object response = objectMapper.readValue(arrayResponse, Object.class);
        
        // When & Then - Test various array operations
        
        // Array indexing
        assertEquals("Laptop", fieldMapper.extractValue(response, "products[0].name"));
        assertEquals(999.99, fieldMapper.extractValue(response, "products[0].price"));
        
        // Nested array access
        assertEquals("electronics", fieldMapper.extractValue(response, "products[0].categories[0]"));
        assertEquals(5, fieldMapper.extractValue(response, "products[0].reviews[0].rating"));
        
        // Array filtering
        assertEquals("Mouse", fieldMapper.extractValue(response, "products[name=Mouse].name"));
        assertEquals(29.99, fieldMapper.extractValue(response, "products[name=Mouse].price"));
        
        // Array iteration (returns the array for further processing)
        Object allProducts = fieldMapper.extractValue(response, "products[*]");
        assertTrue(allProducts instanceof List);
        assertEquals(2, ((List<?>) allProducts).size());
    }

    @Test
    void testFieldMappingExceptionDetails() throws Exception {
        // Given - Response for testing error scenarios
        String testResponse = """
            {
              "data": {
                "items": ["a", "b"]
              }
            }
            """;
        
        Object response = objectMapper.readValue(testResponse, Object.class);
        
        // When & Then - Test specific error scenarios
        
        // Array index out of bounds
        FieldMappingException arrayException = assertThrows(FieldMappingException.class,
            () -> fieldMapper.extractValue(response, "data.items[5]"));
        assertEquals("data.items[5]", arrayException.getMapperExpression());
        assertTrue(arrayException.getMessage().contains("Array index 5 out of bounds"));
        
        // Property not found
        FieldMappingException propertyException = assertThrows(FieldMappingException.class,
            () -> fieldMapper.extractValue(response, "data.nonexistent"));
        assertEquals("data.nonexistent", propertyException.getMapperExpression());
        // The suggestion will be about key existence since JsonNode is treated as a Map
        assertNotNull(propertyException.getSuggestion());
        
        // Invalid array filter - this should fail because strings don't have a 'name' property
        FieldMappingException filterException = assertThrows(FieldMappingException.class,
            () -> fieldMapper.extractValue(response, "data.items[name=notfound]"));
        // The error message should contain information about the failure
        assertNotNull(filterException.getMessage());
    }
}