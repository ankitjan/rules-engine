package com.rulesengine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rulesengine.entity.FieldConfigEntity;
import com.rulesengine.mapper.FieldMapper;
import com.rulesengine.mapper.FieldMappingUtils;
import com.rulesengine.exception.FieldMappingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FieldMappingServiceTest {

    private FieldMappingService fieldMappingService;
    private FieldMapper fieldMapper;
    private FieldMappingUtils fieldMappingUtils;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        fieldMapper = new FieldMapper(objectMapper);
        fieldMappingUtils = new FieldMappingUtils(fieldMapper);
        fieldMappingService = new FieldMappingService(fieldMapper, fieldMappingUtils);
    }

    @Test
    void extractFieldValues_WithValidConfigs_Success() {
        // Given
        Map<String, Object> response = new HashMap<>();
        response.put("name", "John Doe");
        response.put("age", "30");
        response.put("active", "true");
        
        List<FieldConfigEntity> fieldConfigs = Arrays.asList(
            createFieldConfig("userName", "STRING", "name", null, false),
            createFieldConfig("userAge", "NUMBER", "age", null, false),
            createFieldConfig("isActive", "BOOLEAN", "active", null, false)
        );
        
        // When
        Map<String, Object> result = fieldMappingService.extractFieldValues(response, fieldConfigs);
        
        // Then
        assertEquals(3, result.size());
        assertEquals("John Doe", result.get("userName"));
        assertEquals(30, result.get("userAge")); // Converted to Integer
        assertEquals(true, result.get("isActive")); // Converted to Boolean
    }

    @Test
    void extractFieldValues_WithMissingOptionalField_UsesDefault() {
        // Given
        Map<String, Object> response = new HashMap<>();
        response.put("name", "John Doe");
        
        List<FieldConfigEntity> fieldConfigs = Arrays.asList(
            createFieldConfig("userName", "STRING", "name", null, false),
            createFieldConfig("userEmail", "STRING", "email", "default@example.com", false)
        );
        
        // When
        Map<String, Object> result = fieldMappingService.extractFieldValues(response, fieldConfigs);
        
        // Then
        assertEquals(2, result.size());
        assertEquals("John Doe", result.get("userName"));
        assertEquals("default@example.com", result.get("userEmail"));
    }

    @Test
    void extractFieldValues_WithMissingRequiredField_ThrowsException() {
        // Given
        Map<String, Object> response = new HashMap<>();
        response.put("name", "John Doe");
        
        List<FieldConfigEntity> fieldConfigs = Arrays.asList(
            createFieldConfig("userName", "STRING", "name", null, false),
            createFieldConfig("userEmail", "STRING", "email", null, true) // Required field
        );
        
        // When & Then
        FieldMappingException exception = assertThrows(FieldMappingException.class,
            () -> fieldMappingService.extractFieldValues(response, fieldConfigs));
        
        assertTrue(exception.getMessage().contains("Required field 'userEmail' extraction failed"));
    }

    @Test
    void extractFieldValues_WithMissingOptionalFieldNoDefault_SkipsField() {
        // Given
        Map<String, Object> response = new HashMap<>();
        response.put("name", "John Doe");
        
        List<FieldConfigEntity> fieldConfigs = Arrays.asList(
            createFieldConfig("userName", "STRING", "name", null, false),
            createFieldConfig("userEmail", "STRING", "email", null, false) // Optional, no default
        );
        
        // When
        Map<String, Object> result = fieldMappingService.extractFieldValues(response, fieldConfigs);
        
        // Then
        assertEquals(1, result.size());
        assertEquals("John Doe", result.get("userName"));
        assertFalse(result.containsKey("userEmail"));
    }

    @Test
    void extractFieldValue_WithValidConfig_Success() {
        // Given
        Map<String, Object> response = new HashMap<>();
        response.put("age", "25");
        
        FieldConfigEntity fieldConfig = createFieldConfig("userAge", "NUMBER", "age", null, false);
        
        // When
        Object result = fieldMappingService.extractFieldValue(response, fieldConfig);
        
        // Then
        assertEquals(25, result);
    }

    @Test
    void extractFieldValue_WithNoMapper_ReturnsDefault() {
        // Given
        Map<String, Object> response = new HashMap<>();
        
        FieldConfigEntity fieldConfig = createFieldConfig("staticValue", "STRING", null, "default", false);
        
        // When
        Object result = fieldMappingService.extractFieldValue(response, fieldConfig);
        
        // Then
        assertEquals("default", result);
    }

    @Test
    void extractFieldValue_WithFailedExtractionAndDefault_ReturnsDefault() {
        // Given
        Map<String, Object> response = new HashMap<>();
        
        FieldConfigEntity fieldConfig = createFieldConfig("missingField", "STRING", "nonexistent", "fallback", false);
        
        // When
        Object result = fieldMappingService.extractFieldValue(response, fieldConfig);
        
        // Then
        assertEquals("fallback", result);
    }

    @Test
    void extractFieldValue_WithFailedExtractionRequired_ThrowsException() {
        // Given
        Map<String, Object> response = new HashMap<>();
        
        FieldConfigEntity fieldConfig = createFieldConfig("requiredField", "STRING", "nonexistent", null, true);
        
        // When & Then
        assertThrows(FieldMappingException.class,
            () -> fieldMappingService.extractFieldValue(response, fieldConfig));
    }

    @Test
    void validateFieldConfig_WithValidExpression_ReturnsSuccess() {
        // Given
        Map<String, Object> response = new HashMap<>();
        response.put("name", "John");
        
        FieldConfigEntity fieldConfig = createFieldConfig("userName", "STRING", "name", null, false);
        
        // When
        FieldMappingUtils.ValidationResult result = fieldMappingService.validateFieldConfig(fieldConfig, response);
        
        // Then
        assertTrue(result.isValid());
        assertEquals("John", result.getExtractedValue());
    }

    @Test
    void validateFieldConfig_WithInvalidExpression_ReturnsFailure() {
        // Given
        Map<String, Object> response = new HashMap<>();
        response.put("name", "John");
        
        FieldConfigEntity fieldConfig = createFieldConfig("userEmail", "STRING", "email", null, false);
        
        // When
        FieldMappingUtils.ValidationResult result = fieldMappingService.validateFieldConfig(fieldConfig, response);
        
        // Then
        assertFalse(result.isValid());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    void validateFieldConfig_WithNoMapper_ReturnsSuccessWithDefault() {
        // Given
        Map<String, Object> response = new HashMap<>();
        
        FieldConfigEntity fieldConfig = createFieldConfig("staticField", "STRING", null, "default", false);
        
        // When
        FieldMappingUtils.ValidationResult result = fieldMappingService.validateFieldConfig(fieldConfig, response);
        
        // Then
        assertTrue(result.isValid());
        assertEquals("default", result.getExtractedValue());
    }

    @Test
    void extractFieldValues_WithComplexNestedData_Success() throws Exception {
        // Given
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonResponse = """
            {
              "user": {
                "profile": {
                  "name": "Jane Smith",
                  "age": 28
                },
                "preferences": {
                  "notifications": true
                }
              }
            }
            """;
        Object response = objectMapper.readValue(jsonResponse, Object.class);
        
        List<FieldConfigEntity> fieldConfigs = Arrays.asList(
            createFieldConfig("fullName", "STRING", "user.profile.name", null, false),
            createFieldConfig("age", "NUMBER", "user.profile.age", null, false),
            createFieldConfig("notifications", "BOOLEAN", "user.preferences.notifications", null, false)
        );
        
        // When
        Map<String, Object> result = fieldMappingService.extractFieldValues(response, fieldConfigs);
        
        // Then
        assertEquals(3, result.size());
        assertEquals("Jane Smith", result.get("fullName"));
        assertEquals(28, result.get("age"));
        assertEquals(true, result.get("notifications"));
    }

    private FieldConfigEntity createFieldConfig(String fieldName, String fieldType, String mapperExpression, 
                                              String defaultValue, boolean isRequired) {
        FieldConfigEntity config = new FieldConfigEntity();
        config.setFieldName(fieldName);
        config.setFieldType(fieldType);
        config.setMapperExpression(mapperExpression);
        config.setDefaultValue(defaultValue);
        config.setIsRequired(isRequired);
        config.setIsCalculated(false);
        config.setVersion(1);
        return config;
    }
}