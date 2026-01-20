package com.rulesengine.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents a complete rule definition with combinator and rules
 */
public class RuleDefinition {
    
    @JsonProperty("combinator")
    private String combinator; // "and" or "or"
    
    @JsonProperty("rules")
    private List<RuleItem> rules;
    
    @JsonProperty("not")
    private Boolean not = false;
    
    // Constructors
    public RuleDefinition() {}
    
    public RuleDefinition(String combinator, List<RuleItem> rules) {
        this.combinator = combinator;
        this.rules = rules;
    }
    
    // Getters and Setters
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
        return "RuleDefinition{" +
                "combinator='" + combinator + '\'' +
                ", rules=" + (rules != null ? rules.size() : 0) +
                ", not=" + not +
                '}';
    }
}