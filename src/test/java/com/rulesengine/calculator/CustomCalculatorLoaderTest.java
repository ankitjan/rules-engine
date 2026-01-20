package com.rulesengine.calculator;

import com.rulesengine.calculator.examples.PercentageCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CustomCalculatorLoaderTest {

    private CustomCalculatorLoader customCalculatorLoader;

    @BeforeEach
    void setUp() {
        customCalculatorLoader = new CustomCalculatorLoader();
    }

    @Test
    void testLoadCalculator_ValidClass() throws CalculatorException {
        String className = "com.rulesengine.calculator.examples.PercentageCalculator";
        
        FieldCalculator calculator = customCalculatorLoader.loadCalculator(className);
        
        assertNotNull(calculator);
        assertTrue(calculator instanceof PercentageCalculator);
        assertEquals("percentage", calculator.getName());
    }

    @Test
    void testLoadCalculator_NonExistentClass() {
        String className = "com.example.NonExistentCalculator";
        
        CalculatorException exception = assertThrows(CalculatorException.class,
            () -> customCalculatorLoader.loadCalculator(className));
        
        assertTrue(exception.getMessage().contains("Calculator class not found"));
    }

    @Test
    void testLoadCalculator_InvalidClass() {
        String className = "java.lang.String"; // String doesn't implement FieldCalculator
        
        CalculatorException exception = assertThrows(CalculatorException.class,
            () -> customCalculatorLoader.loadCalculator(className));
        
        assertTrue(exception.getMessage().contains("Error loading calculator class"));
    }

    @Test
    void testLoadCalculator_EmptyClassName() {
        CalculatorException exception = assertThrows(CalculatorException.class,
            () -> customCalculatorLoader.loadCalculator(""));
        
        assertTrue(exception.getMessage().contains("Calculator class name cannot be empty"));
    }

    @Test
    void testLoadCalculator_NullClassName() {
        CalculatorException exception = assertThrows(CalculatorException.class,
            () -> customCalculatorLoader.loadCalculator(null));
        
        assertTrue(exception.getMessage().contains("Calculator class name cannot be empty"));
    }

    @Test
    void testExecuteCustomCalculator() throws CalculatorException {
        String className = "com.rulesengine.calculator.examples.PercentageCalculator";
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("numeratorField", "numerator");
        parameters.put("denominatorField", "denominator");
        parameters.put("asPercentage", true);
        
        Map<String, Object> context = new HashMap<>();
        context.put("numerator", 25.0);
        context.put("denominator", 100.0);
        
        Object result = customCalculatorLoader.executeCustomCalculator(className, parameters, context);
        
        assertNotNull(result);
        assertEquals(25.0, result); // 25/100 * 100 = 25%
    }

    @Test
    void testCacheSize() throws CalculatorException {
        assertEquals(0, customCalculatorLoader.getCacheSize());
        
        String className = "com.rulesengine.calculator.examples.PercentageCalculator";
        customCalculatorLoader.loadCalculator(className);
        
        assertEquals(1, customCalculatorLoader.getCacheSize());
        
        // Loading same calculator again should use cache
        customCalculatorLoader.loadCalculator(className);
        assertEquals(1, customCalculatorLoader.getCacheSize());
    }

    @Test
    void testClearCache() throws CalculatorException {
        String className = "com.rulesengine.calculator.examples.PercentageCalculator";
        customCalculatorLoader.loadCalculator(className);
        
        assertEquals(1, customCalculatorLoader.getCacheSize());
        
        customCalculatorLoader.clearCache();
        assertEquals(0, customCalculatorLoader.getCacheSize());
    }
}