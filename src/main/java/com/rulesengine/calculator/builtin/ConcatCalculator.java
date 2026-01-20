package com.rulesengine.calculator.builtin;

import com.rulesengine.calculator.AbstractFieldCalculator;
import com.rulesengine.calculator.CalculatorException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Calculator that concatenates string field values
 */
@Component
public class ConcatCalculator extends AbstractFieldCalculator {
    
    @Override
    public Object calculate(Map<String, Object> parameters, Map<String, Object> context) throws CalculatorException {
        List<String> fieldNames = getRequiredParameter(parameters, "fields", List.class);
        validateFieldsNotEmpty(fieldNames);
        
        String separator = getOptionalParameter(parameters, "separator", String.class, "");
        
        return fieldNames.stream()
                .map(context::get)
                .filter(value -> value != null)
                .map(Object::toString)
                .collect(Collectors.joining(separator));
    }
    
    @Override
    public void validateParameters(Map<String, Object> parameters) throws CalculatorException {
        List<String> fieldNames = getRequiredParameter(parameters, "fields", List.class);
        validateFieldsNotEmpty(fieldNames);
    }
    
    @Override
    public String getName() {
        return "concat";
    }
    
    @Override
    public String getDescription() {
        return "Concatenates string field values with an optional separator";
    }
}