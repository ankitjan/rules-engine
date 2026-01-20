package com.rulesengine.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request for batch rule execution
 */
public class BatchExecutionRequest {
    
    @NotEmpty(message = "Rule IDs cannot be empty")
    private List<Long> ruleIds;
    
    @NotNull(message = "Execution context is required")
    @Valid
    private ExecutionContext context;
    
    private boolean stopOnFirstFailure = false;
    private boolean includeTraces = true;
    
    // Constructors
    public BatchExecutionRequest() {}
    
    public BatchExecutionRequest(List<Long> ruleIds, ExecutionContext context) {
        this.ruleIds = ruleIds;
        this.context = context;
    }
    
    // Getters and Setters
    public List<Long> getRuleIds() {
        return ruleIds;
    }
    
    public void setRuleIds(List<Long> ruleIds) {
        this.ruleIds = ruleIds;
    }
    
    public ExecutionContext getContext() {
        return context;
    }
    
    public void setContext(ExecutionContext context) {
        this.context = context;
    }
    
    public boolean isStopOnFirstFailure() {
        return stopOnFirstFailure;
    }
    
    public void setStopOnFirstFailure(boolean stopOnFirstFailure) {
        this.stopOnFirstFailure = stopOnFirstFailure;
    }
    
    public boolean isIncludeTraces() {
        return includeTraces;
    }
    
    public void setIncludeTraces(boolean includeTraces) {
        this.includeTraces = includeTraces;
    }
    
    @Override
    public String toString() {
        return "BatchExecutionRequest{" +
                "ruleIds=" + ruleIds +
                ", context=" + context +
                ", stopOnFirstFailure=" + stopOnFirstFailure +
                ", includeTraces=" + includeTraces +
                '}';
    }
}