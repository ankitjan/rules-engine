package com.rulesengine.calculator;

import com.rulesengine.calculator.builtin.SumCalculator;
import com.rulesengine.calculator.builtin.AverageCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CalculatorRegistryTest {

    private CalculatorRegistry calculatorRegistry;

    @BeforeEach
    void setUp() {
        calculatorRegistry = new CalculatorRegistry(Arrays.asList(
            new SumCalculator(),
            new AverageCalculator()
        ));
    }

    @Test
    void testGetCalculator_ExistingCalculator() throws CalculatorException {
        FieldCalculator calculator = calculatorRegistry.getCalculator("sum");
        
        assertNotNull(calculator);
        assertEquals("sum", calculator.getName());
        assertTrue(calculator instanceof SumCalculator);
    }

    @Test
    void testGetCalculator_NonExistentCalculator() {
        CalculatorException exception = assertThrows(CalculatorException.class,
            () -> calculatorRegistry.getCalculator("nonexistent"));
        
        assertTrue(exception.getMessage().contains("Calculator not found: nonexistent"));
    }

    @Test
    void testHasCalculator() {
        assertTrue(calculatorRegistry.hasCalculator("sum"));
        assertTrue(calculatorRegistry.hasCalculator("average"));
        assertFalse(calculatorRegistry.hasCalculator("nonexistent"));
    }

    @Test
    void testGetCalculatorNames() {
        Set<String> names = calculatorRegistry.getCalculatorNames();
        
        assertEquals(2, names.size());
        assertTrue(names.contains("sum"));
        assertTrue(names.contains("average"));
    }

    @Test
    void testRegisterNewCalculator() {
        FieldCalculator mockCalculator = new FieldCalculator() {
            @Override
            public Object calculate(java.util.Map<String, Object> parameters, java.util.Map<String, Object> context) {
                return "test";
            }

            @Override
            public void validateParameters(java.util.Map<String, Object> parameters) {
                // No validation needed for test
            }

            @Override
            public String getName() {
                return "test";
            }

            @Override
            public String getDescription() {
                return "Test calculator";
            }
        };

        calculatorRegistry.register(mockCalculator);

        assertTrue(calculatorRegistry.hasCalculator("test"));
        assertEquals(3, calculatorRegistry.getCalculatorNames().size());
    }

    @Test
    void testUnregisterCalculator() {
        assertTrue(calculatorRegistry.unregister("sum"));
        assertFalse(calculatorRegistry.hasCalculator("sum"));
        assertEquals(1, calculatorRegistry.getCalculatorNames().size());

        // Try to unregister non-existent calculator
        assertFalse(calculatorRegistry.unregister("nonexistent"));
    }
}