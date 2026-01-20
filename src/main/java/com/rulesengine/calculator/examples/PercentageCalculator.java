package com.rulesengine.calculator.examples;

import com.rulesengine.calculator.AbstractFieldCalculator;
import com.rulesengine.calculator.CalculatorException;

import java.util.Map;

/**
 * Example custom calculator that calculates percentage
 * This demonstrates how to create custom calculators
 */
public class PercentageCalculator extends AbstractFieldCalculator {
    
    @Override
    public Object calculate(Map<String, Object> parameters, Map<String, Object> context) throws CalculatorException {
        String numeratorField = getRequiredParameter(parameters, "numeratorField", String.class);
        String denominatorField = getRequiredParameter(parameters, "denominatorField", String.class);
        Boolean asPercentage = getOptionalParameter(parameters, "asPercentage", Boolean.class, true);
        
        Object numeratorValue = context.get(numeratorField);
        Object denominatorValue = context.get(denominatorField);
        
        if (numeratorValue == null) {
            throw new CalculatorException("Numerator field '" + numeratorField + "' is null", getName(), numeratorField);
        }
        
        if (denominatorValue == null) {
            throw new CalculatorException("Denominator field '" + denominatorField + "' is null", getName(), denominatorField);
        }
        
        if (!(numeratorValue instanceof Number)) {
            throw new CalculatorException("Numerator field '" + numeratorField + "' is not numeric", getName(), numeratorField);
        }
        
        if (!(denominatorValue instanceof Number)) {
            throw new CalculatorException("Denominator field '" + denominatorField + "' is not numeric", getName(), denominatorField);
        }
        
        double numerator = ((Number) numeratorValue).doubleValue();
        double denominator = ((Number) denominatorValue).doubleValue();
        
        if (denominator == 0) {
            throw new CalculatorException("Division by zero: denominator field '" + denominatorField + "' is zero", getName(), denominatorField);
        }
        
        double result = numerator / denominator;
        
        if (asPercentage) {
            result *= 100;
        }
        
        return result;
    }
    
    @Override
    public void validateParameters(Map<String, Object> parameters) throws CalculatorException {
        getRequiredParameter(parameters, "numeratorField", String.class);
        getRequiredParameter(parameters, "denominatorField", String.class);
        // asPercentage is optional
    }
    
    @Override
    public String getName() {
        return "percentage";
    }
    
    @Override
    public String getDescription() {
        return "Calculates percentage by dividing numerator field by denominator field";
    }
}