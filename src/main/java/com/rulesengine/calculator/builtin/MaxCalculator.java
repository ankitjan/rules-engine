package com.rulesengine.calculator.builtin;

import com.rulesengine.calculator.AbstractFieldCalculator;
import com.rulesengine.calculator.CalculatorException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Calculator that finds the maximum value among numeric fields
 */
@Component
public class MaxCalculator extends AbstractFieldCalculator {
    
    @Override
    public Object calculate(Map<String, Object> parameters, Map<String, Object> context) throws CalculatorException {
        List<String> fieldNames = getRequiredParameter(parameters, "fields", List.class);
        validateFieldsNotEmpty(fieldNames);
        
        List<Number> values = getNumericFieldValues(fieldNames, context);
        
        if (values.isEmpty()) {
            return null;
        }
        
        return values.stream()
                .mapToDouble(this::toDouble)
                .max()
                .orElse(Double.NaN);
    }
    
    @Override
    public void validateParameters(Map<String, Object> parameters) throws CalculatorException {
        List<String> fieldNames = getRequiredParameter(parameters, "fields", List.class);
        validateFieldsNotEmpty(fieldNames);
    }
    
    @Override
    public String getName() {
        return "max";
    }
    
    @Override
    public String getDescription() {
        return "Finds the maximum value among numeric field values";
    }
}