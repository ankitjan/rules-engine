package com.rulesengine.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rulesengine.dto.RuleExecutionResult;
import com.rulesengine.model.RuleDefinition;
import com.rulesengine.model.RuleItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Core component for evaluating rule conditions against provided data contexts
 */
@Component
public class RuleEvaluator {
    
    private static final Logger logger = LoggerFactory.getLogger(RuleEvaluator.class);
    
    private final ObjectMapper objectMapper;
    
    @Autowired
    public RuleEvaluator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * Evaluate a rule against field values and return boolean result
     */
    public boolean evaluateRule(RuleDefinition rule, Map<String, Object> fieldValues) {
        try {
            return evaluateRuleDefinition(rule, fieldValues, "root");
        } catch (Exception e) {
            logger.error("Error evaluating rule: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Evaluate a rule with detailed execution traces for debugging
     */
    public RuleExecutionResult evaluateWithTrace(RuleDefinition rule, Map<String, Object> fieldValues) {
        RuleExecutionResult result = new RuleExecutionResult();
        
        try {
            long startTime = System.currentTimeMillis();
            
            boolean outcome = evaluateRuleDefinitionWithTrace(rule, fieldValues, "root", result);
            
            long endTime = System.currentTimeMillis();
            result.setExecutionDurationMs(endTime - startTime);
            result.setResult(outcome);
            result.setFieldValues(fieldValues);
            
            logger.debug("Rule evaluation completed in {}ms with result: {}", 
                result.getExecutionDurationMs(), outcome);
            
        } catch (Exception e) {
            logger.error("Error evaluating rule with trace: {}", e.getMessage(), e);
            result.setError("Evaluation error: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Parse rule definition from JSON string
     */
    public RuleDefinition parseRuleDefinition(String ruleDefinitionJson) {
        try {
            return objectMapper.readValue(ruleDefinitionJson, RuleDefinition.class);
        } catch (Exception e) {
            logger.error("Error parsing rule definition: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Invalid rule definition JSON: " + e.getMessage(), e);
        }
    }
    
    /**
     * Evaluate rule definition without traces
     */
    private boolean evaluateRuleDefinition(RuleDefinition rule, Map<String, Object> fieldValues, String path) {
        if (rule == null) {
            return false; // Null rule evaluates to false
        }
        if (rule.getRules() == null || rule.getRules().isEmpty()) {
            return true; // Empty rule evaluates to true
        }
        
        boolean result = evaluateRuleItems(rule.getRules(), rule.getCombinator(), fieldValues, path);
        
        // Apply NOT operator if specified
        if (Boolean.TRUE.equals(rule.getNot())) {
            result = !result;
        }
        
        return result;
    }
    
    /**
     * Evaluate rule definition with traces
     */
    private boolean evaluateRuleDefinitionWithTrace(RuleDefinition rule, Map<String, Object> fieldValues, 
                                                   String path, RuleExecutionResult executionResult) {
        if (rule == null) {
            executionResult.addTrace(path, "Null rule - evaluates to false", false);
            return false;
        }
        if (rule.getRules() == null || rule.getRules().isEmpty()) {
            executionResult.addTrace(path, "Empty rule - evaluates to true", true);
            return true;
        }
        
        boolean result = evaluateRuleItemsWithTrace(rule.getRules(), rule.getCombinator(), 
                                                   fieldValues, path, executionResult);
        
        // Apply NOT operator if specified
        if (Boolean.TRUE.equals(rule.getNot())) {
            result = !result;
            executionResult.addTrace(path, "Applied NOT operator", result, !result, result);
        }
        
        executionResult.addTrace(path, "Rule evaluation completed", result);
        return result;
    }
    
    /**
     * Evaluate a list of rule items with combinator
     */
    private boolean evaluateRuleItems(List<RuleItem> ruleItems, String combinator, 
                                     Map<String, Object> fieldValues, String path) {
        if (ruleItems == null || ruleItems.isEmpty()) {
            return true;
        }
        
        boolean isAnd = "and".equalsIgnoreCase(combinator);
        boolean result = isAnd; // Start with true for AND, false for OR
        
        for (int i = 0; i < ruleItems.size(); i++) {
            RuleItem item = ruleItems.get(i);
            String itemPath = path + "[" + i + "]";
            
            boolean itemResult = evaluateRuleItem(item, fieldValues, itemPath);
            
            if (isAnd) {
                result = result && itemResult;
                if (!result) {
                    break; // Short-circuit for AND
                }
            } else {
                result = result || itemResult;
                if (result) {
                    break; // Short-circuit for OR
                }
            }
        }
        
        return result;
    }
    
    /**
     * Evaluate a list of rule items with combinator and traces
     */
    private boolean evaluateRuleItemsWithTrace(List<RuleItem> ruleItems, String combinator, 
                                              Map<String, Object> fieldValues, String path, 
                                              RuleExecutionResult executionResult) {
        if (ruleItems == null || ruleItems.isEmpty()) {
            executionResult.addTrace(path, "Empty rule items - evaluates to true", true);
            return true;
        }
        
        boolean isAnd = "and".equalsIgnoreCase(combinator);
        boolean result = isAnd; // Start with true for AND, false for OR
        
        executionResult.addTrace(path, "Evaluating " + ruleItems.size() + " items with " + combinator, true);
        
        for (int i = 0; i < ruleItems.size(); i++) {
            RuleItem item = ruleItems.get(i);
            String itemPath = path + "[" + i + "]";
            
            boolean itemResult = evaluateRuleItemWithTrace(item, fieldValues, itemPath, executionResult);
            
            if (isAnd) {
                result = result && itemResult;
                executionResult.addTrace(itemPath, "AND operation: " + result + " && " + itemResult + " = " + result, result);
                if (!result) {
                    executionResult.addTrace(path, "Short-circuit AND evaluation (false result)", false);
                    break;
                }
            } else {
                result = result || itemResult;
                executionResult.addTrace(itemPath, "OR operation: " + result + " || " + itemResult + " = " + result, result);
                if (result) {
                    executionResult.addTrace(path, "Short-circuit OR evaluation (true result)", true);
                    break;
                }
            }
        }
        
        return result;
    }
    
    /**
     * Evaluate a single rule item (condition or group)
     */
    private boolean evaluateRuleItem(RuleItem item, Map<String, Object> fieldValues, String path) {
        if (item == null) {
            return true;
        }
        
        boolean result;
        
        if (item.isCondition()) {
            result = evaluateCondition(item, fieldValues, path);
        } else if (item.isGroup()) {
            result = evaluateRuleItems(item.getRules(), item.getCombinator(), fieldValues, path);
        } else {
            logger.warn("Invalid rule item at path {}: {}", path, item);
            return false;
        }
        
        // Apply NOT operator if specified
        if (Boolean.TRUE.equals(item.getNot())) {
            result = !result;
        }
        
        return result;
    }
    
    /**
     * Evaluate a single rule item with traces
     */
    private boolean evaluateRuleItemWithTrace(RuleItem item, Map<String, Object> fieldValues, 
                                             String path, RuleExecutionResult executionResult) {
        if (item == null) {
            executionResult.addTrace(path, "Null rule item - evaluates to true", true);
            return true;
        }
        
        boolean result;
        
        if (item.isCondition()) {
            result = evaluateConditionWithTrace(item, fieldValues, path, executionResult);
        } else if (item.isGroup()) {
            executionResult.addTrace(path, "Evaluating nested group with " + item.getCombinator(), true);
            result = evaluateRuleItemsWithTrace(item.getRules(), item.getCombinator(), fieldValues, path, executionResult);
        } else {
            executionResult.addTrace(path, "Invalid rule item", false);
            logger.warn("Invalid rule item at path {}: {}", path, item);
            return false;
        }
        
        // Apply NOT operator if specified
        if (Boolean.TRUE.equals(item.getNot())) {
            result = !result;
            executionResult.addTrace(path, "Applied NOT operator to item", result, !result, result);
        }
        
        return result;
    }
    
    /**
     * Evaluate a single condition
     */
    private boolean evaluateCondition(RuleItem condition, Map<String, Object> fieldValues, String path) {
        String field = condition.getField();
        String operator = condition.getOperator();
        Object expectedValue = condition.getValue();
        
        Object actualValue = fieldValues.get(field);
        
        return compareValues(actualValue, operator, expectedValue);
    }
    
    /**
     * Evaluate a single condition with traces
     */
    private boolean evaluateConditionWithTrace(RuleItem condition, Map<String, Object> fieldValues, 
                                              String path, RuleExecutionResult executionResult) {
        String field = condition.getField();
        String operator = condition.getOperator();
        Object expectedValue = condition.getValue();
        
        Object actualValue = fieldValues.get(field);
        
        boolean result = compareValues(actualValue, operator, expectedValue);
        
        String description = String.format("Condition: %s %s %s", field, operator, expectedValue);
        executionResult.addTrace(path, description, result, actualValue, expectedValue);
        
        return result;
    }
    
    /**
     * Compare values using the specified operator
     */
    private boolean compareValues(Object actualValue, String operator, Object expectedValue) {
        try {
            switch (operator.toLowerCase()) {
                case "=":
                case "equals":
                    return objectsEqual(actualValue, expectedValue);
                    
                case "!=":
                case "notequals":
                    return !objectsEqual(actualValue, expectedValue);
                    
                case "<":
                case "lessthan":
                    return compareNumbers(actualValue, expectedValue) < 0;
                    
                case "<=":
                case "lessthanorequal":
                    return compareNumbers(actualValue, expectedValue) <= 0;
                    
                case ">":
                case "greaterthan":
                    return compareNumbers(actualValue, expectedValue) > 0;
                    
                case ">=":
                case "greaterthanorequal":
                    return compareNumbers(actualValue, expectedValue) >= 0;
                    
                case "contains":
                    return stringContains(actualValue, expectedValue);
                    
                case "startswith":
                    return stringStartsWith(actualValue, expectedValue);
                    
                case "endswith":
                    return stringEndsWith(actualValue, expectedValue);
                    
                case "in":
                    return valueInCollection(actualValue, expectedValue);
                    
                case "notin":
                    return !valueInCollection(actualValue, expectedValue);
                    
                case "isempty":
                    return isEmpty(actualValue);
                    
                case "isnotempty":
                    return !isEmpty(actualValue);
                    
                default:
                    logger.warn("Unknown operator: {}", operator);
                    return false;
            }
        } catch (Exception e) {
            logger.error("Error comparing values: {} {} {}", actualValue, operator, expectedValue, e);
            return false;
        }
    }
    
    /**
     * Check if two objects are equal
     */
    private boolean objectsEqual(Object actual, Object expected) {
        if (actual == null && expected == null) {
            return true;
        }
        if (actual == null || expected == null) {
            return false;
        }
        
        // Try direct equality first
        if (actual.equals(expected)) {
            return true;
        }
        
        // Try string comparison
        return actual.toString().equals(expected.toString());
    }
    
    /**
     * Compare two values as numbers
     */
    private int compareNumbers(Object actual, Object expected) {
        BigDecimal actualNum = convertToNumber(actual);
        BigDecimal expectedNum = convertToNumber(expected);
        
        if (actualNum == null || expectedNum == null) {
            throw new IllegalArgumentException("Cannot compare non-numeric values: " + actual + " and " + expected);
        }
        
        return actualNum.compareTo(expectedNum);
    }
    
    /**
     * Convert object to BigDecimal for numeric comparison
     */
    private BigDecimal convertToNumber(Object value) {
        if (value == null) {
            return null;
        }
        
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        
        if (value instanceof String) {
            try {
                return new BigDecimal((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        return null;
    }
    
    /**
     * Check if actual string contains expected string
     */
    private boolean stringContains(Object actual, Object expected) {
        if (actual == null || expected == null) {
            return false;
        }
        return actual.toString().toLowerCase().contains(expected.toString().toLowerCase());
    }
    
    /**
     * Check if actual string starts with expected string
     */
    private boolean stringStartsWith(Object actual, Object expected) {
        if (actual == null || expected == null) {
            return false;
        }
        return actual.toString().toLowerCase().startsWith(expected.toString().toLowerCase());
    }
    
    /**
     * Check if actual string ends with expected string
     */
    private boolean stringEndsWith(Object actual, Object expected) {
        if (actual == null || expected == null) {
            return false;
        }
        return actual.toString().toLowerCase().endsWith(expected.toString().toLowerCase());
    }
    
    /**
     * Check if actual value is in expected collection
     */
    private boolean valueInCollection(Object actual, Object expected) {
        if (actual == null || expected == null) {
            return false;
        }
        
        if (expected instanceof Collection) {
            Collection<?> collection = (Collection<?>) expected;
            return collection.contains(actual) || collection.contains(actual.toString());
        }
        
        if (expected instanceof Object[]) {
            Object[] array = (Object[]) expected;
            for (Object item : array) {
                if (objectsEqual(actual, item)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Check if value is empty
     */
    private boolean isEmpty(Object value) {
        if (value == null) {
            return true;
        }
        
        if (value instanceof String) {
            return ((String) value).trim().isEmpty();
        }
        
        if (value instanceof Collection) {
            return ((Collection<?>) value).isEmpty();
        }
        
        if (value instanceof Object[]) {
            return ((Object[]) value).length == 0;
        }
        
        return false;
    }
}