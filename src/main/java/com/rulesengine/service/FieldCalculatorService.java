package com.rulesengine.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rulesengine.analyzer.DependencyAnalyzer;
import com.rulesengine.calculator.CalculatorException;
import com.rulesengine.calculator.CalculatorRegistry;
import com.rulesengine.calculator.CustomCalculatorLoader;
import com.rulesengine.calculator.FieldCalculator;
import com.rulesengine.dto.DependencyGraph;
import com.rulesengine.entity.FieldConfigEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for calculating field values based on dependencies and calculator definitions
 */
@Service
public class FieldCalculatorService {
    
    private static final Logger logger = LoggerFactory.getLogger(FieldCalculatorService.class);
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private DependencyAnalyzer dependencyAnalyzer;
    
    @Autowired
    private CalculatorRegistry calculatorRegistry;
    
    @Autowired
    private CustomCalculatorLoader customCalculatorLoader;
    
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    
    /**
     * Calculate all calculated fields in the correct dependency order
     */
    public Map<String, Object> calculateFields(List<FieldConfigEntity> calculatedFields, 
                                             Map<String, Object> baseFieldValues) {
        logger.debug("Calculating {} calculated fields", calculatedFields.size());
        
        Map<String, Object> results = new HashMap<>(baseFieldValues);
        
        if (calculatedFields.isEmpty()) {
            return results;
        }
        
        // Build dependency graph for calculated fields
        DependencyGraph graph = dependencyAnalyzer.buildDependencyGraph(calculatedFields);
        
        // Validate no circular dependencies
        dependencyAnalyzer.validateNoCycles(graph);
        
        // Get topological order for calculation
        List<String> calculationOrder = graph.getTopologicalOrder();
        
        // Calculate fields in dependency order
        for (String fieldName : calculationOrder) {
            FieldConfigEntity field = graph.getField(fieldName);
            if (field != null && field.getIsCalculated()) {
                try {
                    Object calculatedValue = calculateField(field, results);
                    results.put(fieldName, calculatedValue);
                    logger.debug("Calculated field '{}' = {}", fieldName, calculatedValue);
                } catch (Exception e) {
                    logger.error("Error calculating field '{}': {}", fieldName, e.getMessage());
                    // Use default value if calculation fails
                    if (field.hasDefaultValue()) {
                        results.put(fieldName, parseDefaultValue(field.getDefaultValue(), field.getFieldType()));
                    }
                }
            }
        }
        
        return results;
    }
    
    /**
     * Calculate a single field value
     */
    public Object calculateField(FieldConfigEntity field, Map<String, Object> dependentValues) {
        if (!field.getIsCalculated() || !field.hasCalculator()) {
            throw new IllegalArgumentException("Field is not configured for calculation: " + field.getFieldName());
        }
        
        try {
            CalculatorConfig config = parseCalculatorConfig(field.getCalculatorConfigJson());
            
            switch (config.getType().toUpperCase()) {
                case "EXPRESSION":
                    return executeExpressionCalculator(config.getExpression(), dependentValues);
                case "CUSTOM":
                    return executeCustomCalculator(config.getCalculatorClass(), config.getParameters(), dependentValues);
                case "BUILTIN":
                    return executeBuiltinCalculator(config.getFunction(), config.getParameters(), dependentValues);
                default:
                    throw new IllegalArgumentException("Unknown calculator type: " + config.getType());
            }
        } catch (Exception e) {
            logger.error("Error calculating field '{}': {}", field.getFieldName(), e.getMessage());
            throw new RuntimeException("Field calculation failed for: " + field.getFieldName(), e);
        }
    }
    
    /**
     * Execute expression-based calculator using SpEL
     */
    private Object executeExpressionCalculator(String expression, Map<String, Object> context) {
        if (expression == null || expression.trim().isEmpty()) {
            throw new IllegalArgumentException("Calculator expression cannot be empty");
        }
        
        try {
            Expression spelExpression = expressionParser.parseExpression(expression);
            StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
            
            // Add all field values to the context
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                evaluationContext.setVariable(entry.getKey(), entry.getValue());
            }
            
            return spelExpression.getValue(evaluationContext);
        } catch (Exception e) {
            logger.error("Error executing expression '{}': {}", expression, e.getMessage());
            throw new RuntimeException("Expression evaluation failed: " + expression, e);
        }
    }
    
    /**
     * Execute custom calculator class
     */
    private Object executeCustomCalculator(String calculatorClass, Map<String, Object> parameters, 
                                         Map<String, Object> context) {
        try {
            return customCalculatorLoader.executeCustomCalculator(calculatorClass, parameters, context);
        } catch (CalculatorException e) {
            logger.error("Error executing custom calculator '{}': {}", calculatorClass, e.getMessage());
            throw new RuntimeException("Custom calculator execution failed: " + calculatorClass, e);
        }
    }
    
    /**
     * Execute built-in calculator functions
     */
    private Object executeBuiltinCalculator(String function, Map<String, Object> parameters, 
                                          Map<String, Object> context) {
        try {
            FieldCalculator calculator = calculatorRegistry.getCalculator(function);
            
            // Validate parameters
            calculator.validateParameters(parameters);
            
            // Execute calculation
            return calculator.calculate(parameters, context);
            
        } catch (CalculatorException e) {
            logger.error("Error executing built-in calculator '{}': {}", function, e.getMessage());
            throw new RuntimeException("Built-in calculator execution failed: " + function, e);
        }
    }
    
    /**
     * Validate calculator configuration
     */
    public void validateCalculatorConfig(CalculatorConfig config) throws RuntimeException {
        if (config == null) {
            throw new IllegalArgumentException("Calculator configuration cannot be null");
        }
        
        if (config.getType() == null || config.getType().trim().isEmpty()) {
            throw new IllegalArgumentException("Calculator type is required");
        }
        
        try {
            switch (config.getType().toUpperCase()) {
                case "EXPRESSION":
                    validateCalculatorExpression(config.getExpression());
                    break;
                case "CUSTOM":
                    validateCustomCalculator(config.getCalculatorClass());
                    break;
                case "BUILTIN":
                    validateBuiltinCalculator(config.getFunction(), config.getParameters());
                    break;
                default:
                    throw new IllegalArgumentException("Unknown calculator type: " + config.getType());
            }
        } catch (Exception e) {
            throw new RuntimeException("Calculator configuration validation failed", e);
        }
    }
    
    /**
     * Validate custom calculator class
     */
    private void validateCustomCalculator(String calculatorClass) {
        if (calculatorClass == null || calculatorClass.trim().isEmpty()) {
            throw new IllegalArgumentException("Calculator class name is required for CUSTOM type");
        }
        
        try {
            // Try to load the calculator to validate it exists and is valid
            customCalculatorLoader.loadCalculator(calculatorClass);
        } catch (CalculatorException e) {
            throw new IllegalArgumentException("Invalid custom calculator: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate built-in calculator function
     */
    private void validateBuiltinCalculator(String function, Map<String, Object> parameters) {
        if (function == null || function.trim().isEmpty()) {
            throw new IllegalArgumentException("Function name is required for BUILTIN type");
        }
        
        try {
            FieldCalculator calculator = calculatorRegistry.getCalculator(function);
            if (parameters != null) {
                calculator.validateParameters(parameters);
            }
        } catch (CalculatorException e) {
            throw new IllegalArgumentException("Invalid built-in calculator: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parse calculator configuration JSON
     */
    private CalculatorConfig parseCalculatorConfig(String configJson) {
        try {
            return objectMapper.readValue(configJson, CalculatorConfig.class);
        } catch (Exception e) {
            logger.error("Error parsing calculator config: {}", configJson, e);
            throw new RuntimeException("Invalid calculator configuration", e);
        }
    }
    
    /**
     * Parse default value based on field type
     */
    private Object parseDefaultValue(String defaultValue, String fieldType) {
        if (defaultValue == null) {
            return null;
        }
        
        try {
            switch (fieldType.toUpperCase()) {
                case "NUMBER":
                    return Double.parseDouble(defaultValue);
                case "BOOLEAN":
                    return Boolean.parseBoolean(defaultValue);
                case "STRING":
                default:
                    return defaultValue;
            }
        } catch (Exception e) {
            logger.warn("Error parsing default value '{}' for type '{}', using as string", 
                       defaultValue, fieldType);
            return defaultValue;
        }
    }
    
    /**
     * Validate calculator expression
     */
    public void validateCalculatorExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            throw new IllegalArgumentException("Calculator expression cannot be empty");
        }
        
        try {
            expressionParser.parseExpression(expression);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid calculator expression: " + expression, e);
        }
    }
    
    /**
     * Get available built-in calculator functions
     */
    public Set<String> getAvailableCalculators() {
        return calculatorRegistry.getCalculatorNames();
    }
    
    /**
     * Get calculator information
     */
    public Map<String, String> getCalculatorInfo(String calculatorName) throws CalculatorException {
        FieldCalculator calculator = calculatorRegistry.getCalculator(calculatorName);
        Map<String, String> info = new HashMap<>();
        info.put("name", calculator.getName());
        info.put("description", calculator.getDescription());
        return info;
    }
    
    /**
     * Calculator configuration class
     */
    public static class CalculatorConfig {
        private String type; // EXPRESSION, CUSTOM, BUILTIN
        private String expression; // For EXPRESSION type
        private String calculatorClass; // For CUSTOM type
        private String function; // For BUILTIN type
        private Map<String, Object> parameters; // For BUILTIN type
        
        // Getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getExpression() { return expression; }
        public void setExpression(String expression) { this.expression = expression; }
        
        public String getCalculatorClass() { return calculatorClass; }
        public void setCalculatorClass(String calculatorClass) { this.calculatorClass = calculatorClass; }
        
        public String getFunction() { return function; }
        public void setFunction(String function) { this.function = function; }
        
        public Map<String, Object> getParameters() { return parameters; }
        public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
    }
}