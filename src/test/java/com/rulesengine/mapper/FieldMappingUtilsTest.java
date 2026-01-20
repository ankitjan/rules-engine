package com.rulesengine.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rulesengine.exception.FieldMappingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FieldMappingUtilsTest {

    private FieldMappingUtils fieldMappingUtils;
    private FieldMapper fieldMapper;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        fieldMapper = new FieldMapper(objectMapper);
        fieldMappingUtils = new FieldMappingUtils(fieldMapper);
    }

    @Test
    void extractMultipleFields_Success() {
        // Given
        Map<String, Object> response = new HashMap<>();
        response.put("name", "John");
        response.put("age", 30);
        response.put("email", "john@example.com");
        
        Map<String, String> fieldMappings = new HashMap<>();
        fieldMappings.put("userName", "name");
        fieldMappings.put("userAge", "age");
        fieldMappings.put("userEmail", "email");
        
        // When
        Map<String, Object> result = fieldMappingUtils.extractMultipleFields(response, fieldMappings);
        
        // Then
        assertEquals(3, result.size());
        assertEquals("John", result.get("userName"));
        assertEquals(30, result.get("userAge"));
        assertEquals("john@example.com", result.get("userEmail"));
    }

    @Test
    void extractMultipleFields_OneFieldFails_ThrowsExceptionWithContext() {
        // Given
        Map<String, Object> response = new HashMap<>();
        response.put("name", "John");
        
        Map<String, String> fieldMappings = new HashMap<>();
        fieldMappings.put("userName", "name");
        fieldMappings.put("userAge", "nonexistent");
        
        // When & Then
        FieldMappingException exception = assertThrows(FieldMappingException.class,
            () -> fieldMappingUtils.extractMultipleFields(response, fieldMappings));
        
        assertTrue(exception.getMessage().contains("Failed to extract field 'userAge'"));
    }

    @Test
    void extractWithDefault_Success() {
        // Given
        Map<String, Object> response = new HashMap<>();
        response.put("name", "John");
        
        // When
        Object result = fieldMappingUtils.extractWithDefault(response, "name", "default");
        
        // Then
        assertEquals("John", result);
    }

    @Test
    void extractWithDefault_FieldNotFound_ReturnsDefault() {
        // Given
        Map<String, Object> response = new HashMap<>();
        response.put("name", "John");
        
        // When
        Object result = fieldMappingUtils.extractWithDefault(response, "nonexistent", "default");
        
        // Then
        assertEquals("default", result);
    }

    @Test
    void extractAndConvert_Success() {
        // Given
        Map<String, Object> response = new HashMap<>();
        response.put("age", "30");
        
        // When
        Integer result = fieldMappingUtils.extractAndConvert(response, "age", Integer.class);
        
        // Then
        assertEquals(30, result);
    }

    @Test
    void extractAndConvert_NullValue_ReturnsNull() {
        // Given
        Map<String, Object> response = new HashMap<>();
        response.put("age", null);
        
        // When
        Integer result = fieldMappingUtils.extractAndConvert(response, "age", Integer.class);
        
        // Then
        assertNull(result);
    }

    @Test
    void extractAndConvertWithDefault_Success() {
        // Given
        Map<String, Object> response = new HashMap<>();
        response.put("age", "30");
        
        // When
        Integer result = fieldMappingUtils.extractAndConvertWithDefault(response, "age", Integer.class, 0);
        
        // Then
        assertEquals(30, result);
    }

    @Test
    void extractAndConvertWithDefault_FieldNotFound_ReturnsDefault() {
        // Given
        Map<String, Object> response = new HashMap<>();
        
        // When
        Integer result = fieldMappingUtils.extractAndConvertWithDefault(response, "nonexistent", Integer.class, 0);
        
        // Then
        assertEquals(0, result);
    }

    @Test
    void extractAndConvertWithDefault_ConversionFails_ReturnsDefault() {
        // Given
        Map<String, Object> response = new HashMap<>();
        response.put("age", "not_a_number");
        
        // When
        Integer result = fieldMappingUtils.extractAndConvertWithDefault(response, "age", Integer.class, 0);
        
        // Then
        assertEquals(0, result);
    }

    @Test
    void validateMapperExpression_ValidExpression_ReturnsTrue() {
        // Given
        Map<String, Object> response = new HashMap<>();
        response.put("name", "John");
        
        // When
        boolean result = fieldMappingUtils.validateMapperExpression(response, "name");
        
        // Then
        assertTrue(result);
    }

    @Test
    void validateMapperExpression_InvalidExpression_ReturnsFalse() {
        // Given
        Map<String, Object> response = new HashMap<>();
        response.put("name", "John");
        
        // When
        boolean result = fieldMappingUtils.validateMapperExpression(response, "nonexistent");
        
        // Then
        assertFalse(result);
    }

    @Test
    void validateMapperExpressionDetailed_ValidExpression_ReturnsSuccessResult() {
        // Given
        Map<String, Object> response = new HashMap<>();
        response.put("name", "John");
        
        // When
        FieldMappingUtils.ValidationResult result = 
            fieldMappingUtils.validateMapperExpressionDetailed(response, "name");
        
        // Then
        assertTrue(result.isValid());
        assertNull(result.getErrorMessage());
        assertNull(result.getSuggestion());
        assertEquals("John", result.getExtractedValue());
    }

    @Test
    void validateMapperExpressionDetailed_InvalidExpression_ReturnsFailureResult() {
        // Given
        Map<String, Object> response = new HashMap<>();
        response.put("name", "John");
        
        // When
        FieldMappingUtils.ValidationResult result = 
            fieldMappingUtils.validateMapperExpressionDetailed(response, "nonexistent");
        
        // Then
        assertFalse(result.isValid());
        assertNotNull(result.getErrorMessage());
        assertNotNull(result.getSuggestion());
        assertNull(result.getExtractedValue());
    }

    @Test
    void extractFromMultipleResponses_Success() {
        // Given
        List<Object> responses = Arrays.asList(
            createResponse("John", 30),
            createResponse("Jane", 25),
            createResponse("Bob", 35)
        );
        
        // When
        List<Object> result = fieldMappingUtils.extractFromMultipleResponses(responses, "name");
        
        // Then
        assertEquals(3, result.size());
        assertEquals("John", result.get(0));
        assertEquals("Jane", result.get(1));
        assertEquals("Bob", result.get(2));
    }

    @Test
    void extractFromMultipleResponses_SomeFailures_ReturnsNullForFailures() {
        // Given
        List<Object> responses = Arrays.asList(
            createResponse("John", 30),
            new HashMap<>(), // This will fail extraction
            createResponse("Bob", 35)
        );
        
        // When
        List<Object> result = fieldMappingUtils.extractFromMultipleResponses(responses, "name");
        
        // Then
        assertEquals(3, result.size());
        assertEquals("John", result.get(0));
        assertNull(result.get(1)); // Failed extraction
        assertEquals("Bob", result.get(2));
    }

    private Map<String, Object> createResponse(String name, int age) {
        Map<String, Object> response = new HashMap<>();
        response.put("name", name);
        response.put("age", age);
        return response;
    }
}