package com.rulesengine.calculator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loader for custom calculator classes
 */
@Component
public class CustomCalculatorLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomCalculatorLoader.class);
    
    private final Map<String, FieldCalculator> customCalculatorCache = new ConcurrentHashMap<>();
    
    /**
     * Load and instantiate a custom calculator class
     * 
     * @param calculatorClassName Fully qualified class name
     * @return Calculator instance
     * @throws CalculatorException if loading fails
     */
    public FieldCalculator loadCalculator(String calculatorClassName) throws CalculatorException {
        if (calculatorClassName == null || calculatorClassName.trim().isEmpty()) {
            throw new CalculatorException("Calculator class name cannot be empty");
        }
        
        // Check cache first
        FieldCalculator cached = customCalculatorCache.get(calculatorClassName);
        if (cached != null) {
            return cached;
        }
        
        try {
            logger.debug("Loading custom calculator: {}", calculatorClassName);
            
            // Load the class
            Class<?> calculatorClass = Class.forName(calculatorClassName);
            
            // Verify it implements FieldCalculator
            if (!FieldCalculator.class.isAssignableFrom(calculatorClass)) {
                throw new CalculatorException("Class " + calculatorClassName + 
                        " does not implement FieldCalculator interface");
            }
            
            // Create instance
            Object instance = calculatorClass.getDeclaredConstructor().newInstance();
            FieldCalculator calculator = (FieldCalculator) instance;
            
            // Cache the instance
            customCalculatorCache.put(calculatorClassName, calculator);
            
            logger.info("Successfully loaded custom calculator: {} ({})", 
                    calculator.getName(), calculatorClassName);
            
            return calculator;
            
        } catch (ClassNotFoundException e) {
            throw new CalculatorException("Calculator class not found: " + calculatorClassName, e);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new CalculatorException("Cannot instantiate calculator class: " + calculatorClassName, e);
        } catch (NoSuchMethodException e) {
            throw new CalculatorException("Calculator class must have a no-argument constructor: " + calculatorClassName, e);
        } catch (Exception e) {
            throw new CalculatorException("Error loading calculator class: " + calculatorClassName, e);
        }
    }
    
    /**
     * Execute a custom calculator
     * 
     * @param calculatorClassName Fully qualified class name
     * @param parameters Calculator parameters
     * @param context Field values context
     * @return Calculated result
     * @throws CalculatorException if execution fails
     */
    public Object executeCustomCalculator(String calculatorClassName, Map<String, Object> parameters, 
                                        Map<String, Object> context) throws CalculatorException {
        FieldCalculator calculator = loadCalculator(calculatorClassName);
        
        try {
            // Validate parameters first
            calculator.validateParameters(parameters);
            
            // Execute calculation
            return calculator.calculate(parameters, context);
            
        } catch (CalculatorException e) {
            throw e;
        } catch (Exception e) {
            throw new CalculatorException("Error executing custom calculator: " + calculatorClassName, e);
        }
    }
    
    /**
     * Clear the calculator cache
     */
    public void clearCache() {
        customCalculatorCache.clear();
        logger.debug("Cleared custom calculator cache");
    }
    
    /**
     * Get the number of cached calculators
     * 
     * @return Cache size
     */
    public int getCacheSize() {
        return customCalculatorCache.size();
    }
}