package com.rulesengine.calculator;

/**
 * Exception thrown when field calculation fails
 */
public class CalculatorException extends Exception {
    
    private final String calculatorName;
    private final String fieldName;
    
    public CalculatorException(String message) {
        super(message);
        this.calculatorName = null;
        this.fieldName = null;
    }
    
    public CalculatorException(String message, Throwable cause) {
        super(message, cause);
        this.calculatorName = null;
        this.fieldName = null;
    }
    
    public CalculatorException(String message, String calculatorName, String fieldName) {
        super(message);
        this.calculatorName = calculatorName;
        this.fieldName = fieldName;
    }
    
    public CalculatorException(String message, Throwable cause, String calculatorName, String fieldName) {
        super(message, cause);
        this.calculatorName = calculatorName;
        this.fieldName = fieldName;
    }
    
    public String getCalculatorName() {
        return calculatorName;
    }
    
    public String getFieldName() {
        return fieldName;
    }
    
    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(super.getMessage());
        if (calculatorName != null) {
            sb.append(" [Calculator: ").append(calculatorName).append("]");
        }
        if (fieldName != null) {
            sb.append(" [Field: ").append(fieldName).append("]");
        }
        return sb.toString();
    }
}