package com.rulesengine.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FieldMappingExceptionTest {

    @Test
    void constructor_WithMessageExpressionAndPath_SetsAllFields() {
        // Given
        String message = "Test error";
        String expression = "user.name";
        String path = "user";
        
        // When
        FieldMappingException exception = new FieldMappingException(message, expression, path);
        
        // Then
        assertEquals(expression, exception.getMapperExpression());
        assertEquals(path, exception.getFailingPath());
        assertTrue(exception.getMessage().contains(message));
        assertTrue(exception.getMessage().contains(expression));
        assertTrue(exception.getMessage().contains(path));
    }

    @Test
    void constructor_WithCause_SetsCause() {
        // Given
        String message = "Test error";
        String expression = "user.name";
        String path = "user";
        RuntimeException cause = new RuntimeException("Root cause");
        
        // When
        FieldMappingException exception = new FieldMappingException(message, expression, path, cause);
        
        // Then
        assertEquals(cause, exception.getCause());
    }

    @Test
    void getUserFriendlyMessage_WithExpressionAndPath_ReturnsFormattedMessage() {
        // Given
        FieldMappingException exception = new FieldMappingException(
            "Property not found", "user.profile.email", "user.profile");
        
        // When
        String friendlyMessage = exception.getUserFriendlyMessage();
        
        // Then
        assertTrue(friendlyMessage.contains("Field mapping failed for expression 'user.profile.email'"));
        assertTrue(friendlyMessage.contains("at path 'user.profile'"));
        assertTrue(friendlyMessage.contains("Property not found"));
    }

    @Test
    void getUserFriendlyMessage_WithRootPath_DoesNotIncludePath() {
        // Given
        FieldMappingException exception = new FieldMappingException(
            "Property not found", "name", "root");
        
        // When
        String friendlyMessage = exception.getUserFriendlyMessage();
        
        // Then
        assertTrue(friendlyMessage.contains("Field mapping failed for expression 'name'"));
        assertFalse(friendlyMessage.contains("at path"));
    }

    @Test
    void getSuggestion_NullValueError_ReturnsAppropriateAdvice() {
        // Given
        FieldMappingException exception = new FieldMappingException(
            "Null value encountered", "user.name", "user");
        
        // When
        String suggestion = exception.getSuggestion();
        
        // Then
        assertTrue(suggestion.contains("Check if the data source returns the expected structure"));
    }

    @Test
    void getSuggestion_PropertyNotFoundError_ReturnsAppropriateAdvice() {
        // Given
        FieldMappingException exception = new FieldMappingException(
            "Property 'email' not found", "user.email", "user");
        
        // When
        String suggestion = exception.getSuggestion();
        
        // Then
        assertTrue(suggestion.contains("Verify the property name exists"));
        assertTrue(suggestion.contains("Check for typos or case sensitivity"));
    }

    @Test
    void getSuggestion_ArrayIndexOutOfBoundsError_ReturnsAppropriateAdvice() {
        // Given
        FieldMappingException exception = new FieldMappingException(
            "Array index 5 out of bounds", "items[5]", "items");
        
        // When
        String suggestion = exception.getSuggestion();
        
        // Then
        assertTrue(suggestion.contains("Ensure the array has enough elements"));
    }

    @Test
    void getSuggestion_ConversionError_ReturnsAppropriateAdvice() {
        // Given
        FieldMappingException exception = new FieldMappingException(
            "Cannot convert 'abc' to Integer", "age", "age");
        
        // When
        String suggestion = exception.getSuggestion();
        
        // Then
        assertTrue(suggestion.contains("Check the data type of the source value"));
    }

    @Test
    void getSuggestion_InvalidArrayExpressionError_ReturnsAppropriateAdvice() {
        // Given
        FieldMappingException exception = new FieldMappingException(
            "Invalid array expression: [invalid]", "items[invalid]", "items");
        
        // When
        String suggestion = exception.getSuggestion();
        
        // Then
        assertTrue(suggestion.contains("Use valid array syntax"));
        assertTrue(suggestion.contains("[0] for indexing"));
    }

    @Test
    void getSuggestion_MapKeyNotFoundError_ReturnsAppropriateAdvice() {
        // Given
        FieldMappingException exception = new FieldMappingException(
            "Map does not contain key: missing", "missing", "missing");
        
        // When
        String suggestion = exception.getSuggestion();
        
        // Then
        assertTrue(suggestion.contains("Verify the key exists in the response data"));
    }

    @Test
    void getSuggestion_UnknownError_ReturnsGenericAdvice() {
        // Given
        FieldMappingException exception = new FieldMappingException(
            "Some unknown error", "field", "field");
        
        // When
        String suggestion = exception.getSuggestion();
        
        // Then
        assertTrue(suggestion.contains("Review the mapper expression syntax"));
    }

    @Test
    void buildDetailedMessage_WithAllParameters_IncludesAllInformation() {
        // Given
        FieldMappingException exception = new FieldMappingException(
            "Test message", "test.expression", "test.path");
        
        // When
        String message = exception.getMessage();
        
        // Then
        assertTrue(message.contains("Test message"));
        assertTrue(message.contains("Expression: 'test.expression'"));
        assertTrue(message.contains("Failed at: 'test.path'"));
    }

    @Test
    void buildDetailedMessage_WithNullExpression_DoesNotIncludeExpression() {
        // Given
        FieldMappingException exception = new FieldMappingException(
            "Test message", null, "test.path");
        
        // When
        String message = exception.getMessage();
        
        // Then
        assertTrue(message.contains("Test message"));
        assertFalse(message.contains("Expression:"));
    }

    @Test
    void buildDetailedMessage_WithRootPath_DoesNotIncludePath() {
        // Given
        FieldMappingException exception = new FieldMappingException(
            "Test message", "test.expression", "root");
        
        // When
        String message = exception.getMessage();
        
        // Then
        assertTrue(message.contains("Test message"));
        assertTrue(message.contains("Expression: 'test.expression'"));
        assertFalse(message.contains("Failed at:"));
    }
}