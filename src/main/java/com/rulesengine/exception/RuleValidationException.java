package com.rulesengine.exception;

import java.util.List;

public class RuleValidationException extends RuntimeException {

    private final List<String> validationErrors;

    public RuleValidationException(String message) {
        super(message);
        this.validationErrors = List.of(message);
    }

    public RuleValidationException(String message, Throwable cause) {
        super(message, cause);
        this.validationErrors = List.of(message);
    }

    public RuleValidationException(List<String> validationErrors) {
        super("Rule validation failed: " + String.join(", ", validationErrors));
        this.validationErrors = validationErrors;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }
}