package com.rulesengine.calculator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registry for managing field calculators
 */
@Component
public class CalculatorRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(CalculatorRegistry.class);
    
    private final Map<String, FieldCalculator> calculators = new HashMap<>();
    
    @Autowired
    public CalculatorRegistry(List<FieldCalculator> calculatorList) {
        for (FieldCalculator calculator : calculatorList) {
            register(calculator);
        }
        logger.info("Registered {} calculators", calculators.size());
    }
    
    /**
     * Register a calculator
     * 
     * @param calculator Calculator to register
     */
    public void register(FieldCalculator calculator) {
        String name = calculator.getName();
        if (calculators.containsKey(name)) {
            logger.warn("Overriding existing calculator: {}", name);
        }
        calculators.put(name, calculator);
        logger.debug("Registered calculator: {} - {}", name, calculator.getDescription());
    }
    
    /**
     * Get a calculator by name
     * 
     * @param name Calculator name
     * @return Calculator instance
     * @throws CalculatorException if calculator not found
     */
    public FieldCalculator getCalculator(String name) throws CalculatorException {
        FieldCalculator calculator = calculators.get(name);
        if (calculator == null) {
            throw new CalculatorException("Calculator not found: " + name);
        }
        return calculator;
    }
    
    /**
     * Check if a calculator exists
     * 
     * @param name Calculator name
     * @return true if calculator exists
     */
    public boolean hasCalculator(String name) {
        return calculators.containsKey(name);
    }
    
    /**
     * Get all registered calculator names
     * 
     * @return Set of calculator names
     */
    public Set<String> getCalculatorNames() {
        return calculators.keySet();
    }
    
    /**
     * Get all registered calculators
     * 
     * @return Map of calculator name to calculator instance
     */
    public Map<String, FieldCalculator> getAllCalculators() {
        return new HashMap<>(calculators);
    }
    
    /**
     * Unregister a calculator
     * 
     * @param name Calculator name
     * @return true if calculator was removed
     */
    public boolean unregister(String name) {
        FieldCalculator removed = calculators.remove(name);
        if (removed != null) {
            logger.debug("Unregistered calculator: {}", name);
            return true;
        }
        return false;
    }
}