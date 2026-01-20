package com.rulesengine.calculator;

import java.util.Map;

/**
 * Interface for field calculators that compute field values based on dependencies
 */
public interface FieldCalculator {
    
    /**
     * Calculate field value based on dependent field values
     * 
     * @param parameters Calculator-specific parameters
     * @param context Map of field names to their values
     * @return Calculated field value
     * @throws CalculatorException if calculation fails
     */
    Object calculate(Map<String, Object> parameters, Map<String, Object> context) throws CalculatorException;
    
    /**
     * Validate calculator parameters
     * 
     * @param parameters Calculator-specific parameters
     * @throws CalculatorException if parameters are invalid
     */
    void validateParameters(Map<String, Object> parameters) throws CalculatorException;
    
    /**
     * Get the calculator name/identifier
     * 
     * @return Calculator name
     */
    String getName();
    
    /**
     * Get description of what this calculator does
     * 
     * @return Calculator description
     */
    String getDescription();
}