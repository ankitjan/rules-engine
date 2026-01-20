package com.rulesengine.mapper;

import com.rulesengine.exception.FieldMappingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class providing high-level field mapping operations
 * for common use cases in the Rules Engine.
 */
@Component
public class FieldMappingUtils {
    
    private final FieldMapper fieldMapper;
    
    @Autowired
    public FieldMappingUtils(FieldMapper fieldMapper) {
        this.fieldMapper = fieldMapper;
    }
    
    /**
     * Extracts multiple field values from a response using a map of field names to mapper expressions.
     * 
     * @param response The response object to extract from
     * @param fieldMappings Map of field names to mapper expressions
     * @return Map of field names to extracted values
     */
    public Map<String, Object> extractMultipleFields(Object response, Map<String, String> fieldMappings) {
        Map<String, Object> results = new HashMap<>();
        
        for (Map.Entry<String, String> entry : fieldMappings.entrySet()) {
            String fieldName = entry.getKey();
            String mapperExpression = entry.getValue();
            
            try {
                Object value = fieldMapper.extractValue(response, mapperExpression);
                results.put(fieldName, value);
            } catch (FieldMappingException e) {
                // Re-throw with field context
                throw new FieldMappingException(
                    "Failed to extract field '" + fieldName + "': " + e.getMessage(),
                    mapperExpression,
                    e.getFailingPath(),
                    e
                );
            }
        }
        
        return results;
    }
    
    /**
     * Safely extracts a field value with a default value if extraction fails.
     * 
     * @param response The response object to extract from
     * @param mapperExpression The mapper expression
     * @param defaultValue The default value to return if extraction fails
     * @return The extracted value or default value
     */
    public Object extractWithDefault(Object response, String mapperExpression, Object defaultValue) {
        try {
            return fieldMapper.extractValue(response, mapperExpression);
        } catch (FieldMappingException e) {
            return defaultValue;
        }
    }
    
    /**
     * Extracts and converts a field value to the specified type.
     * 
     * @param response The response object to extract from
     * @param mapperExpression The mapper expression
     * @param targetType The target type to convert to
     * @return The extracted and converted value
     */
    public <T> T extractAndConvert(Object response, String mapperExpression, Class<T> targetType) {
        Object value = fieldMapper.extractValue(response, mapperExpression);
        if (value == null) {
            return null;
        }
        
        Object converted = fieldMapper.convertType(value, targetType);
        return targetType.cast(converted);
    }
    
    /**
     * Safely extracts and converts a field value with a default value.
     * 
     * @param response The response object to extract from
     * @param mapperExpression The mapper expression
     * @param targetType The target type to convert to
     * @param defaultValue The default value to return if extraction or conversion fails
     * @return The extracted and converted value or default value
     */
    public <T> T extractAndConvertWithDefault(Object response, String mapperExpression, 
                                            Class<T> targetType, T defaultValue) {
        try {
            return extractAndConvert(response, mapperExpression, targetType);
        } catch (FieldMappingException e) {
            return defaultValue;
        }
    }
    
    /**
     * Validates that a mapper expression can be successfully applied to a sample response.
     * 
     * @param sampleResponse A sample response object for validation
     * @param mapperExpression The mapper expression to validate
     * @return true if the expression is valid, false otherwise
     */
    public boolean validateMapperExpression(Object sampleResponse, String mapperExpression) {
        try {
            fieldMapper.extractValue(sampleResponse, mapperExpression);
            return true;
        } catch (FieldMappingException e) {
            return false;
        }
    }
    
    /**
     * Gets detailed validation information for a mapper expression.
     * 
     * @param sampleResponse A sample response object for validation
     * @param mapperExpression The mapper expression to validate
     * @return ValidationResult containing success status and error details
     */
    public ValidationResult validateMapperExpressionDetailed(Object sampleResponse, String mapperExpression) {
        try {
            Object result = fieldMapper.extractValue(sampleResponse, mapperExpression);
            return new ValidationResult(true, null, null, result);
        } catch (FieldMappingException e) {
            return new ValidationResult(false, e.getMessage(), e.getSuggestion(), null);
        }
    }
    
    /**
     * Extracts values from an array of responses using the same mapper expression.
     * 
     * @param responses Array of response objects
     * @param mapperExpression The mapper expression to apply to each response
     * @return List of extracted values
     */
    public List<Object> extractFromMultipleResponses(List<Object> responses, String mapperExpression) {
        return responses.stream()
                .map(response -> {
                    try {
                        return fieldMapper.extractValue(response, mapperExpression);
                    } catch (FieldMappingException e) {
                        // Return null for failed extractions, let caller decide how to handle
                        return null;
                    }
                })
                .toList();
    }
    
    /**
     * Result of mapper expression validation.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final String suggestion;
        private final Object extractedValue;
        
        public ValidationResult(boolean valid, String errorMessage, String suggestion, Object extractedValue) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.suggestion = suggestion;
            this.extractedValue = extractedValue;
        }
        
        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
        public String getSuggestion() { return suggestion; }
        public Object getExtractedValue() { return extractedValue; }
    }
}