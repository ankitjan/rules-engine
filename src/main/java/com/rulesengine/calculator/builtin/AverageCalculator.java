package com.rulesengine.calculator.builtin;

import com.rulesengine.calculator.AbstractFieldCalculator;
import com.rulesengine.calculator.CalculatorException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Calculator that computes the average of numeric field values
 */
@Component
public class AverageCalculator extends AbstractFieldCalculator {
    
    @Override
    public Object calculate(Map<String, Object> parameters, Map<String, Object> context) throws CalculatorException {
        List<String> fieldNames = getRequiredParameter(parameters, "fields", List.class);
        validateFieldsNotEmpty(fieldNames);
        
        List<Number> values = getNumericFieldValues(fieldNames, context);
        
        if (values.isEmpty()) {
            return 0.0;
        }
        
        double sum = values.stream()
                .mapToDouble(this::toDouble)
                .sum();
        
        return sum / values.size();
    }
    
    @Override
    public void validateParameters(Map<String, Object> parameters) throws CalculatorException {
        List<String> fieldNames = getRequiredParameter(parameters, "fields", List.class);
        validateFieldsNotEmpty(fieldNames);
    }
    
    @Override
    public String getName() {
        return "average";
    }
    
    @Override
    public String getDescription() {
        return "Calculates the average of numeric field values";
    }
}