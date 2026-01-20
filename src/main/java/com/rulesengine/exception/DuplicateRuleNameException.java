package com.rulesengine.exception;

public class DuplicateRuleNameException extends RuntimeException {

    public DuplicateRuleNameException(String message) {
        super(message);
    }

    public DuplicateRuleNameException(String message, Throwable cause) {
        super(message, cause);
    }

    public static DuplicateRuleNameException forRuleName(String ruleName) {
        return new DuplicateRuleNameException("Rule with name '" + ruleName + "' already exists");
    }
}