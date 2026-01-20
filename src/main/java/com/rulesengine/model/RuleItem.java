package com.rulesengine.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents either a condition or a nested rule group
 */
public class RuleItem {
    
    // For conditions
    @JsonProperty("field")
    private String field;
    
    @JsonProperty("operator")
    private String operator;
    
    @JsonProperty("value")
    private Object value;
    
    // For nested groups
    @JsonProperty("combinator")
    private String combinator;
    
    @JsonProperty("rules")
    private List<RuleItem> rules;
    
    @JsonProperty("not")
    private Boolean not = false;
    
    // Constructors
    public RuleItem() {}
    
    // Constructor for condition
    public RuleItem(String field, String operator, Object value) {
        this.field = field;
        this.operator = operator;
        this.value = value;
    }
    
    // Constructor for group
    public RuleItem(String combinator, List<RuleItem> rules) {
        this.combinator = combinator;
        this.rules = rules;
    }
    
    // Utility methods
    public boolean isCondition() {
        return field != null && operator != null;
    }
    
    public boolean isGroup() {
        return combinator != null && rules != null;
    }
    
    // Getters and Setters
    public String getField() {
        return field;
    }
    
    public void setField(String field) {
        this.field = field;
    }
    
    public String getOperator() {
        return operator;
    }
    
    public void setOperator(String operator) {
        this.operator = operator;
    }
    
    public Object getValue() {
        return value;
    }
    
    public void setValue(Object value) {
        this.value = value;
    }
    
    public String getCombinator() {
        return combinator;
    }
    
    public void setCombinator(String combinator) {
        this.combinator = combinator;
    }
    
    public List<RuleItem> getRules() {
        return rules;
    }
    
    public void setRules(List<RuleItem> rules) {
        this.rules = rules;
    }
    
    public Boolean getNot() {
        return not;
    }
    
    public void setNot(Boolean not) {
        this.not = not;
    }
    
    @Override
    public String toString() {
        if (isCondition()) {
            return "RuleItem{condition: " + field + " " + operator + " " + value + ", not=" + not + "}";
        } else if (isGroup()) {
            return "RuleItem{group: " + combinator + " with " + (rules != null ? rules.size() : 0) + " rules, not=" + not + "}";
        } else {
            return "RuleItem{invalid}";
        }
    }
}