package com.rulesengine.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rulesengine.exception.FieldMappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FieldMapper provides reflection-based value extraction from complex nested objects
 * with support for dot notation, array indexing, and automatic type conversion.
 * 
 * Supports mapper expressions like:
 * - "user.profile.email" (dot notation)
 * - "orders[0].amount" (array indexing)
 * - "data.items[*].name" (array iteration)
 * - "response.users[id=123].name" (array filtering)
 */
@Component
public class FieldMapper {
    
    private static final Logger logger = LoggerFactory.getLogger(FieldMapper.class);
    
    private final ObjectMapper objectMapper;
    
    // Cache for reflection metadata to improve performance
    private final Map<String, Method> methodCache = new ConcurrentHashMap<>();
    private final Map<String, Field> fieldCache = new ConcurrentHashMap<>();
    
    // Patterns for parsing mapper expressions
    private static final Pattern ARRAY_INDEX_PATTERN = Pattern.compile("\\[(\\d+)\\]");
    private static final Pattern ARRAY_FILTER_PATTERN = Pattern.compile("\\[([^=]+)=([^\\]]+)\\]");
    private static final Pattern ARRAY_ALL_PATTERN = Pattern.compile("\\[\\*\\]");
    
    @Autowired
    public FieldMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * Extracts a value from a response object using the specified mapper expression.
     * 
     * @param response The response object to extract from
     * @param mapperExpression The dot notation expression (e.g., "user.profile.email")
     * @return The extracted value
     * @throws FieldMappingException if extraction fails
     */
    public Object extractValue(Object response, String mapperExpression) {
        if (response == null) {
            throw new FieldMappingException("Cannot extract value from null response", mapperExpression, "root");
        }
        
        if (mapperExpression == null || mapperExpression.trim().isEmpty()) {
            throw new FieldMappingException("Mapper expression cannot be null or empty", mapperExpression, "root");
        }
        
        try {
            logger.debug("Extracting value using expression: {} from response type: {}", 
                        mapperExpression, response.getClass().getSimpleName());
            
            String[] pathSegments = mapperExpression.split("\\.");
            Object currentValue = response;
            String currentPath = "";
            
            for (int i = 0; i < pathSegments.length; i++) {
                String segment = pathSegments[i];
                currentPath = currentPath.isEmpty() ? segment : currentPath + "." + segment;
                
                if (currentValue == null) {
                    throw new FieldMappingException("Null value encountered in path", mapperExpression, currentPath);
                }
                
                currentValue = navigatePathSegment(currentValue, segment, mapperExpression, currentPath);
            }
            
            logger.debug("Successfully extracted value: {} using expression: {}", currentValue, mapperExpression);
            return currentValue;
            
        } catch (FieldMappingException e) {
            throw e;
        } catch (Exception e) {
            throw new FieldMappingException("Unexpected error during value extraction: " + e.getMessage(), 
                                          mapperExpression, "unknown", e);
        }
    }
    
    /**
     * Converts a value to the specified target type with automatic type conversion.
     * 
     * @param value The value to convert
     * @param targetType The target class type
     * @return The converted value
     * @throws FieldMappingException if conversion fails
     */
    public Object convertType(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }
        
        try {
            logger.debug("Converting value: {} from {} to {}", value, value.getClass().getSimpleName(), targetType.getSimpleName());
            
            // String conversions
            if (targetType == String.class) {
                return value.toString();
            }
            
            String stringValue = value.toString();
            
            // Number conversions
            if (targetType == Integer.class || targetType == int.class) {
                return convertToInteger(stringValue);
            }
            if (targetType == Long.class || targetType == long.class) {
                return convertToLong(stringValue);
            }
            if (targetType == Double.class || targetType == double.class) {
                return convertToDouble(stringValue);
            }
            if (targetType == Float.class || targetType == float.class) {
                return convertToFloat(stringValue);
            }
            if (targetType == BigDecimal.class) {
                return new BigDecimal(stringValue);
            }
            
            // Boolean conversions
            if (targetType == Boolean.class || targetType == boolean.class) {
                return convertToBoolean(stringValue);
            }
            
            // Date conversions
            if (targetType == LocalDate.class) {
                return convertToLocalDate(stringValue);
            }
            if (targetType == LocalDateTime.class) {
                return convertToLocalDateTime(stringValue);
            }
            if (targetType == Date.class) {
                return convertToDate(stringValue);
            }
            
            // Collection conversions
            if (List.class.isAssignableFrom(targetType) && value instanceof Collection) {
                return new ArrayList<>((Collection<?>) value);
            }
            if (Set.class.isAssignableFrom(targetType) && value instanceof Collection) {
                return new HashSet<>((Collection<?>) value);
            }
            
            // Try Jackson conversion for complex objects
            return objectMapper.convertValue(value, targetType);
            
        } catch (Exception e) {
            throw new FieldMappingException("Failed to convert value '" + value + "' to type " + targetType.getSimpleName() + ": " + e.getMessage(), 
                                          null, "type_conversion", e);
        }
    }
    
    /**
     * Navigates a single path segment, handling array indexing and object property access.
     */
    private Object navigatePathSegment(Object currentValue, String segment, String fullExpression, String currentPath) {
        // Check for array operations
        if (segment.contains("[")) {
            return handleArrayAccess(currentValue, segment, fullExpression, currentPath);
        }
        
        // Regular property access
        return getPropertyValue(currentValue, segment, fullExpression, currentPath);
    }
    
    /**
     * Handles array access operations including indexing, filtering, and iteration.
     */
    private Object handleArrayAccess(Object currentValue, String segment, String fullExpression, String currentPath) {
        String propertyName = segment.substring(0, segment.indexOf('['));
        String arrayExpression = segment.substring(segment.indexOf('['));
        
        // Get the array/collection property first
        Object arrayValue = propertyName.isEmpty() ? currentValue : getPropertyValue(currentValue, propertyName, fullExpression, currentPath);
        
        if (arrayValue == null) {
            throw new FieldMappingException("Array property is null", fullExpression, currentPath);
        }
        
        // Convert to array/list for processing
        List<?> list = convertToList(arrayValue, fullExpression, currentPath);
        
        // Handle different array access patterns
        Matcher indexMatcher = ARRAY_INDEX_PATTERN.matcher(arrayExpression);
        Matcher filterMatcher = ARRAY_FILTER_PATTERN.matcher(arrayExpression);
        Matcher allMatcher = ARRAY_ALL_PATTERN.matcher(arrayExpression);
        
        if (indexMatcher.matches()) {
            // Array indexing: [0], [1], etc.
            int index = Integer.parseInt(indexMatcher.group(1));
            if (index < 0 || index >= list.size()) {
                throw new FieldMappingException("Array index " + index + " out of bounds (size: " + list.size() + ")", 
                                              fullExpression, currentPath);
            }
            return list.get(index);
            
        } else if (filterMatcher.matches()) {
            // Array filtering: [id=123], [name=John], etc.
            String filterField = filterMatcher.group(1);
            String filterValue = filterMatcher.group(2);
            
            for (Object item : list) {
                if (item != null) {
                    Object itemFieldValue = getPropertyValue(item, filterField, fullExpression, currentPath + "[" + filterField + "]");
                    if (itemFieldValue != null && itemFieldValue.toString().equals(filterValue)) {
                        return item;
                    }
                }
            }
            throw new FieldMappingException("No array item found with " + filterField + "=" + filterValue, 
                                          fullExpression, currentPath);
            
        } else if (allMatcher.matches()) {
            // Array iteration: [*] - returns the list itself for further processing
            return list;
            
        } else {
            throw new FieldMappingException("Invalid array expression: " + arrayExpression, fullExpression, currentPath);
        }
    }
    
    /**
     * Gets a property value from an object using reflection or Map access.
     */
    private Object getPropertyValue(Object object, String propertyName, String fullExpression, String currentPath) {
        if (object == null) {
            throw new FieldMappingException("Cannot access property '" + propertyName + "' on null object", 
                                          fullExpression, currentPath);
        }
        
        try {
            // Handle Map access
            if (object instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) object;
                if (map.containsKey(propertyName)) {
                    return map.get(propertyName);
                }
                throw new FieldMappingException("Map does not contain key: " + propertyName, fullExpression, currentPath);
            }
            
            // Handle JsonNode access
            if (object instanceof JsonNode) {
                JsonNode node = (JsonNode) object;
                if (node.has(propertyName)) {
                    JsonNode childNode = node.get(propertyName);
                    return convertJsonNodeToObject(childNode);
                }
                throw new FieldMappingException("JsonNode does not contain property: " + propertyName, fullExpression, currentPath);
            }
            
            // Handle regular object property access via reflection
            return getReflectionPropertyValue(object, propertyName, fullExpression, currentPath);
            
        } catch (FieldMappingException e) {
            throw e;
        } catch (Exception e) {
            throw new FieldMappingException("Failed to access property '" + propertyName + "': " + e.getMessage(), 
                                          fullExpression, currentPath, e);
        }
    }
    
    /**
     * Gets property value using reflection (getter methods or direct field access).
     */
    private Object getReflectionPropertyValue(Object object, String propertyName, String fullExpression, String currentPath) {
        Class<?> clazz = object.getClass();
        String cacheKey = clazz.getName() + "." + propertyName;
        
        // Try getter method first
        Method getter = methodCache.computeIfAbsent(cacheKey + ".getter", k -> findGetterMethod(clazz, propertyName));
        if (getter != null) {
            try {
                getter.setAccessible(true);
                return getter.invoke(object);
            } catch (Exception e) {
                throw new FieldMappingException("Failed to invoke getter method for property '" + propertyName + "': " + e.getMessage(), 
                                              fullExpression, currentPath, e);
            }
        }
        
        // Try direct field access
        Field field = fieldCache.computeIfAbsent(cacheKey + ".field", k -> findField(clazz, propertyName));
        if (field != null) {
            try {
                field.setAccessible(true);
                return field.get(object);
            } catch (Exception e) {
                throw new FieldMappingException("Failed to access field '" + propertyName + "': " + e.getMessage(), 
                                              fullExpression, currentPath, e);
            }
        }
        
        throw new FieldMappingException("Property '" + propertyName + "' not found in class " + clazz.getSimpleName(), 
                                      fullExpression, currentPath);
    }
    
    /**
     * Finds a getter method for the specified property name.
     */
    private Method findGetterMethod(Class<?> clazz, String propertyName) {
        String getterName = "get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        String booleanGetterName = "is" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        
        try {
            return clazz.getMethod(getterName);
        } catch (NoSuchMethodException e) {
            try {
                return clazz.getMethod(booleanGetterName);
            } catch (NoSuchMethodException e2) {
                return null;
            }
        }
    }
    
    /**
     * Finds a field with the specified name in the class hierarchy.
     */
    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> currentClass = clazz;
        while (currentClass != null) {
            try {
                return currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();
            }
        }
        return null;
    }
    
    /**
     * Converts various collection types to List for uniform processing.
     */
    private List<?> convertToList(Object value, String fullExpression, String currentPath) {
        if (value instanceof List) {
            return (List<?>) value;
        }
        if (value instanceof Collection) {
            return new ArrayList<>((Collection<?>) value);
        }
        if (value.getClass().isArray()) {
            List<Object> list = new ArrayList<>();
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                list.add(Array.get(value, i));
            }
            return list;
        }
        throw new FieldMappingException("Expected array or collection but got: " + value.getClass().getSimpleName(), 
                                      fullExpression, currentPath);
    }
    
    /**
     * Converts JsonNode to appropriate Java object.
     */
    private Object convertJsonNodeToObject(JsonNode node) {
        if (node.isNull()) {
            return null;
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        if (node.isInt()) {
            return node.intValue();
        }
        if (node.isLong()) {
            return node.longValue();
        }
        if (node.isDouble()) {
            return node.doubleValue();
        }
        if (node.isTextual()) {
            return node.textValue();
        }
        if (node.isArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonNode item : node) {
                list.add(convertJsonNodeToObject(item));
            }
            return list;
        }
        if (node.isObject()) {
            return objectMapper.convertValue(node, Map.class);
        }
        return node.toString();
    }
    
    // Type conversion helper methods
    
    private Integer convertToInteger(String value) {
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            // Try parsing as double first, then convert to int
            try {
                return Double.valueOf(value.trim()).intValue();
            } catch (NumberFormatException e2) {
                throw new NumberFormatException("Cannot convert '" + value + "' to Integer");
            }
        }
    }
    
    private Long convertToLong(String value) {
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            try {
                return Double.valueOf(value.trim()).longValue();
            } catch (NumberFormatException e2) {
                throw new NumberFormatException("Cannot convert '" + value + "' to Long");
            }
        }
    }
    
    private Double convertToDouble(String value) {
        try {
            return Double.valueOf(value.trim());
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Cannot convert '" + value + "' to Double");
        }
    }
    
    private Float convertToFloat(String value) {
        try {
            return Float.valueOf(value.trim());
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Cannot convert '" + value + "' to Float");
        }
    }
    
    private Boolean convertToBoolean(String value) {
        String trimmed = value.trim().toLowerCase();
        if ("true".equals(trimmed) || "1".equals(trimmed) || "yes".equals(trimmed)) {
            return true;
        }
        if ("false".equals(trimmed) || "0".equals(trimmed) || "no".equals(trimmed)) {
            return false;
        }
        throw new IllegalArgumentException("Cannot convert '" + value + "' to Boolean");
    }
    
    private LocalDate convertToLocalDate(String value) {
        try {
            // Try common date formats
            String trimmed = value.trim();
            if (trimmed.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE);
            }
            if (trimmed.matches("\\d{2}/\\d{2}/\\d{4}")) {
                return LocalDate.parse(trimmed, DateTimeFormatter.ofPattern("MM/dd/yyyy"));
            }
            if (trimmed.matches("\\d{2}-\\d{2}-\\d{4}")) {
                return LocalDate.parse(trimmed, DateTimeFormatter.ofPattern("MM-dd-yyyy"));
            }
            // Default ISO parsing
            return LocalDate.parse(trimmed);
        } catch (DateTimeParseException e) {
            throw new DateTimeParseException("Cannot convert '" + value + "' to LocalDate", value, 0);
        }
    }
    
    private LocalDateTime convertToLocalDateTime(String value) {
        try {
            String trimmed = value.trim();
            // Try ISO format first
            if (trimmed.contains("T")) {
                return LocalDateTime.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
            // Try space-separated format
            if (trimmed.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
                return LocalDateTime.parse(trimmed, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
            // Default parsing
            return LocalDateTime.parse(trimmed);
        } catch (DateTimeParseException e) {
            throw new DateTimeParseException("Cannot convert '" + value + "' to LocalDateTime", value, 0);
        }
    }
    
    private Date convertToDate(String value) {
        // Convert to LocalDateTime first, then to Date
        LocalDateTime localDateTime = convertToLocalDateTime(value);
        return java.sql.Timestamp.valueOf(localDateTime);
    }
}