package com.rulesengine.calculator;

import java.util.List;
import java.util.Map;

/**
 * Abstract base class for field calculators providing common functionality
 */
public abstract class AbstractFieldCalculator implements FieldCalculator {
    
    /**
     * Extract a required parameter from the parameters map
     * 
     * @param parameters Parameters map
     * @param key Parameter key
     * @param type Expected parameter type
     * @return Parameter value
     * @throws CalculatorException if parameter is missing or wrong type
     */
    @SuppressWarnings("unchecked")
    protected <T> T getRequiredParameter(Map<String, Object> parameters, String key, Class<T> type) 
            throws CalculatorException {
        Object value = parameters.get(key);
        if (value == null) {
            throw new CalculatorException("Required parameter '" + key + "' is missing", getName(), null);
        }
        
        if (!type.isInstance(value)) {
            throw new CalculatorException("Parameter '" + key + "' must be of type " + type.getSimpleName() + 
                    " but was " + value.getClass().getSimpleName(), getName(), null);
        }
        
        return (T) value;
    }
    
    /**
     * Extract an optional parameter from the parameters map
     * 
     * @param parameters Parameters map
     * @param key Parameter key
     * @param type Expected parameter type
     * @param defaultValue Default value if parameter is missing
     * @return Parameter value or default
     */
    @SuppressWarnings("unchecked")
    protected <T> T getOptionalParameter(Map<String, Object> parameters, String key, Class<T> type, T defaultValue) {
        Object value = parameters.get(key);
        if (value == null) {
            return defaultValue;
        }
        
        if (!type.isInstance(value)) {
            return defaultValue;
        }
        
        return (T) value;
    }
    
    /**
     * Get field values from context, filtering out null values
     * 
     * @param fieldNames List of field names
     * @param context Field values context
     * @return List of non-null field values
     */
    protected List<Object> getFieldValues(List<String> fieldNames, Map<String, Object> context) {
        return fieldNames.stream()
                .map(context::get)
                .filter(value -> value != null)
                .toList();
    }
    
    /**
     * Get numeric field values from context
     * 
     * @param fieldNames List of field names
     * @param context Field values context
     * @return List of numeric values
     * @throws CalculatorException if any field is not numeric
     */
    protected List<Number> getNumericFieldValues(List<String> fieldNames, Map<String, Object> context) 
            throws CalculatorException {
        return fieldNames.stream()
                .map(fieldName -> {
                    Object value = context.get(fieldName);
                    if (value == null) {
                        return null;
                    }
                    if (!(value instanceof Number)) {
                        throw new RuntimeException("Field '" + fieldName + "' is not numeric: " + value);
                    }
                    return (Number) value;
                })
                .filter(value -> value != null)
                .toList();
    }
    
    /**
     * Convert number to double safely
     * 
     * @param number Number to convert
     * @return Double value
     */
    protected double toDouble(Number number) {
        return number.doubleValue();
    }
    
    /**
     * Validate that at least one field is specified
     * 
     * @param fieldNames List of field names
     * @throws CalculatorException if no fields specified
     */
    protected void validateFieldsNotEmpty(List<String> fieldNames) throws CalculatorException {
        if (fieldNames == null || fieldNames.isEmpty()) {
            throw new CalculatorException("At least one field must be specified", getName(), null);
        }
    }
}