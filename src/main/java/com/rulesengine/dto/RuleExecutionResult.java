package com.rulesengine.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Result of rule execution with outcome and detailed traces
 */
public class RuleExecutionResult {
    
    private Long ruleId;
    private String ruleName;
    private boolean result;
    private LocalDateTime executionTime;
    private long executionDurationMs;
    private List<ExecutionTrace> traces;
    private Map<String, Object> fieldValues;
    private String errorMessage;
    private boolean hasError;
    
    // Constructors
    public RuleExecutionResult() {
        this.traces = new ArrayList<>();
        this.executionTime = LocalDateTime.now();
        this.hasError = false;
    }
    
    public RuleExecutionResult(Long ruleId, String ruleName, boolean result) {
        this();
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.result = result;
    }
    
    // Utility methods
    public void addTrace(ExecutionTrace trace) {
        this.traces.add(trace);
    }
    
    public void addTrace(String path, String description, boolean result) {
        this.traces.add(new ExecutionTrace(path, description, result));
    }
    
    public void addTrace(String path, String description, boolean result, Object actualValue, Object expectedValue) {
        this.traces.add(new ExecutionTrace(path, description, result, actualValue, expectedValue));
    }
    
    public void setError(String errorMessage) {
        this.hasError = true;
        this.errorMessage = errorMessage;
        this.result = false;
    }
    
    // Getters and Setters
    public Long getRuleId() {
        return ruleId;
    }
    
    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }
    
    public String getRuleName() {
        return ruleName;
    }
    
    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }
    
    public boolean isResult() {
        return result;
    }
    
    public void setResult(boolean result) {
        this.result = result;
    }
    
    public LocalDateTime getExecutionTime() {
        return executionTime;
    }
    
    public void setExecutionTime(LocalDateTime executionTime) {
        this.executionTime = executionTime;
    }
    
    public long getExecutionDurationMs() {
        return executionDurationMs;
    }
    
    public void setExecutionDurationMs(long executionDurationMs) {
        this.executionDurationMs = executionDurationMs;
    }
    
    public List<ExecutionTrace> getTraces() {
        return traces;
    }
    
    public void setTraces(List<ExecutionTrace> traces) {
        this.traces = traces != null ? traces : new ArrayList<>();
    }
    
    public Map<String, Object> getFieldValues() {
        return fieldValues;
    }
    
    public void setFieldValues(Map<String, Object> fieldValues) {
        this.fieldValues = fieldValues;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public boolean isHasError() {
        return hasError;
    }
    
    public void setHasError(boolean hasError) {
        this.hasError = hasError;
    }
    
    @Override
    public String toString() {
        return "RuleExecutionResult{" +
                "ruleId=" + ruleId +
                ", ruleName='" + ruleName + '\'' +
                ", result=" + result +
                ", executionDurationMs=" + executionDurationMs +
                ", hasError=" + hasError +
                ", tracesCount=" + traces.size() +
                '}';
    }
    
    /**
     * Execution trace for debugging rule evaluation
     */
    public static class ExecutionTrace {
        private String path;
        private String description;
        private boolean result;
        private Object actualValue;
        private Object expectedValue;
        private LocalDateTime timestamp;
        
        public ExecutionTrace() {
            this.timestamp = LocalDateTime.now();
        }
        
        public ExecutionTrace(String path, String description, boolean result) {
            this();
            this.path = path;
            this.description = description;
            this.result = result;
        }
        
        public ExecutionTrace(String path, String description, boolean result, Object actualValue, Object expectedValue) {
            this(path, description, result);
            this.actualValue = actualValue;
            this.expectedValue = expectedValue;
        }
        
        // Getters and Setters
        public String getPath() {
            return path;
        }
        
        public void setPath(String path) {
            this.path = path;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public boolean isResult() {
            return result;
        }
        
        public void setResult(boolean result) {
            this.result = result;
        }
        
        public Object getActualValue() {
            return actualValue;
        }
        
        public void setActualValue(Object actualValue) {
            this.actualValue = actualValue;
        }
        
        public Object getExpectedValue() {
            return expectedValue;
        }
        
        public void setExpectedValue(Object expectedValue) {
            this.expectedValue = expectedValue;
        }
        
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }
        
        @Override
        public String toString() {
            return "ExecutionTrace{" +
                    "path='" + path + '\'' +
                    ", description='" + description + '\'' +
                    ", result=" + result +
                    ", actualValue=" + actualValue +
                    ", expectedValue=" + expectedValue +
                    '}';
        }
    }
}