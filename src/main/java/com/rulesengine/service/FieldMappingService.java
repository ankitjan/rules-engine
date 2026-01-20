package com.rulesengine.service;

import com.rulesengine.entity.FieldConfigEntity;
import com.rulesengine.mapper.FieldMapper;
import com.rulesengine.mapper.FieldMappingUtils;
import com.rulesengine.exception.FieldMappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service that integrates FieldMapper with FieldConfig entities
 * to provide field value extraction from external service responses.
 */
@Service
public class FieldMappingService {
    
    private static final Logger logger = LoggerFactory.getLogger(FieldMappingService.class);
    
    private final FieldMapper fieldMapper;
    private final FieldMappingUtils fieldMappingUtils;
    
    @Autowired
    public FieldMappingService(FieldMapper fieldMapper, FieldMappingUtils fieldMappingUtils) {
        this.fieldMapper = fieldMapper;
        this.fieldMappingUtils = fieldMappingUtils;
    }
    
    /**
     * Extracts field values from a service response using field configurations.
     * 
     * @param response The response object from an external service
     * @param fieldConfigs List of field configurations with mapper expressions
     * @return Map of field names to extracted values
     */
    public Map<String, Object> extractFieldValues(Object response, List<FieldConfigEntity> fieldConfigs) {
        Map<String, Object> extractedValues = new HashMap<>();
        
        for (FieldConfigEntity fieldConfig : fieldConfigs) {
            if (fieldConfig.hasMapper()) {
                try {
                    String fieldName = fieldConfig.getFieldName();
                    String mapperExpression = fieldConfig.getMapperExpression();
                    
                    logger.debug("Extracting field '{}' using expression: {}", fieldName, mapperExpression);
                    
                    Object value = fieldMapper.extractValue(response, mapperExpression);
                    
                    // Apply type conversion if needed
                    if (value != null) {
                        value = convertToFieldType(value, fieldConfig.getFieldType());
                    }
                    
                    extractedValues.put(fieldName, value);
                    
                } catch (FieldMappingException e) {
                    logger.warn("Failed to extract field '{}': {}", fieldConfig.getFieldName(), e.getMessage());
                    
                    // Use default value if available
                    if (fieldConfig.hasDefaultValue()) {
                        extractedValues.put(fieldConfig.getFieldName(), fieldConfig.getDefaultValue());
                    } else if (fieldConfig.getIsRequired()) {
                        // Re-throw for required fields
                        throw new FieldMappingException(
                            "Required field '" + fieldConfig.getFieldName() + "' extraction failed: " + e.getMessage(),
                            e.getMapperExpression(),
                            e.getFailingPath(),
                            e
                        );
                    }
                    // For optional fields without defaults, we simply don't include them
                }
            }
        }
        
        return extractedValues;
    }
    
    /**
     * Extracts a single field value using its configuration.
     * 
     * @param response The response object from an external service
     * @param fieldConfig The field configuration
     * @return The extracted value, or null if extraction fails and field is optional
     */
    public Object extractFieldValue(Object response, FieldConfigEntity fieldConfig) {
        if (!fieldConfig.hasMapper()) {
            return fieldConfig.hasDefaultValue() ? fieldConfig.getDefaultValue() : null;
        }
        
        try {
            Object value = fieldMapper.extractValue(response, fieldConfig.getMapperExpression());
            
            if (value != null) {
                value = convertToFieldType(value, fieldConfig.getFieldType());
            }
            
            return value;
            
        } catch (FieldMappingException e) {
            logger.warn("Failed to extract field '{}': {}", fieldConfig.getFieldName(), e.getMessage());
            
            if (fieldConfig.hasDefaultValue()) {
                return fieldConfig.getDefaultValue();
            } else if (fieldConfig.getIsRequired()) {
                throw e;
            }
            
            return null;
        }
    }
    
    /**
     * Validates that a field configuration's mapper expression works with a sample response.
     * 
     * @param fieldConfig The field configuration to validate
     * @param sampleResponse A sample response to test against
     * @return ValidationResult containing success status and details
     */
    public FieldMappingUtils.ValidationResult validateFieldConfig(FieldConfigEntity fieldConfig, Object sampleResponse) {
        if (!fieldConfig.hasMapper()) {
            return new FieldMappingUtils.ValidationResult(true, null, null, fieldConfig.getDefaultValue());
        }
        
        return fieldMappingUtils.validateMapperExpressionDetailed(sampleResponse, fieldConfig.getMapperExpression());
    }
    
    /**
     * Converts a value to the specified field type.
     */
    private Object convertToFieldType(Object value, String fieldType) {
        if (value == null || fieldType == null) {
            return value;
        }
        
        try {
            switch (fieldType.toUpperCase()) {
                case "STRING":
                    return fieldMapper.convertType(value, String.class);
                case "NUMBER":
                    // Try Integer first, then Double
                    try {
                        return fieldMapper.convertType(value, Integer.class);
                    } catch (Exception e) {
                        return fieldMapper.convertType(value, Double.class);
                    }
                case "BOOLEAN":
                    return fieldMapper.convertType(value, Boolean.class);
                case "DATE":
                    return fieldMapper.convertType(value, java.time.LocalDate.class);
                case "DATETIME":
                    return fieldMapper.convertType(value, java.time.LocalDateTime.class);
                case "ARRAY":
                case "OBJECT":
                default:
                    // Return as-is for complex types
                    return value;
            }
        } catch (FieldMappingException e) {
            logger.warn("Failed to convert value '{}' to type '{}': {}", value, fieldType, e.getMessage());
            return value; // Return original value if conversion fails
        }
    }
}