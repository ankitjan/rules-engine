package com.rulesengine.calculator.builtin;

import com.rulesengine.calculator.AbstractFieldCalculator;
import com.rulesengine.calculator.CalculatorException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Calculator that counts non-null field values
 */
@Component
public class CountCalculator extends AbstractFieldCalculator {
    
    @Override
    public Object calculate(Map<String, Object> parameters, Map<String, Object> context) throws CalculatorException {
        List<String> fieldNames = getRequiredParameter(parameters, "fields", List.class);
        validateFieldsNotEmpty(fieldNames);
        
        long count = fieldNames.stream()
                .map(context::get)
                .filter(value -> value != null)
                .count();
        
        return (int) count;
    }
    
    @Override
    public void validateParameters(Map<String, Object> parameters) throws CalculatorException {
        List<String> fieldNames = getRequiredParameter(parameters, "fields", List.class);
        validateFieldsNotEmpty(fieldNames);
    }
    
    @Override
    public String getName() {
        return "count";
    }
    
    @Override
    public String getDescription() {
        return "Counts the number of non-null field values";
    }
}