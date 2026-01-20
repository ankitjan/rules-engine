package com.rulesengine.exception;

public class RuleNotFoundException extends RuntimeException {

    public RuleNotFoundException(String message) {
        super(message);
    }

    public RuleNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public RuleNotFoundException(Long ruleId) {
        super("Rule not found with ID: " + ruleId);
    }
}