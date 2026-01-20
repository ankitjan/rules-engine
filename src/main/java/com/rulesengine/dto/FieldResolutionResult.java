package com.rulesengine.dto;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Result of field resolution containing resolved values and execution metrics
 */
public class FieldResolutionResult {
    
    private Map<String, Object> resolvedValues;
    private Map<String, String> resolutionErrors;
    private long totalExecutionTimeMs;
    private LocalDateTime executionTime;
    private int successfulFields;
    private int failedFields;
    private boolean hasErrors;
    private String overallStatus;
    
    public FieldResolutionResult() {
        this.resolvedValues = new HashMap<>();
        this.resolutionErrors = new HashMap<>();
        this.executionTime = LocalDateTime.now();
        this.overallStatus = "SUCCESS";
    }
    
    public FieldResolutionResult(Map<String, Object> resolvedValues) {
        this();
        this.resolvedValues = resolvedValues != null ? new HashMap<>(resolvedValues) : new HashMap<>();
        this.successfulFields = this.resolvedValues.size();
    }
    
    // Business methods
    public void addResolvedValue(String fieldName, Object value) {
        resolvedValues.put(fieldName, value);
        successfulFields++;
    }
    
    public void addResolutionError(String fieldName, String error) {
        resolutionErrors.put(fieldName, error);
        failedFields++;
        hasErrors = true;
        if ("SUCCESS".equals(overallStatus)) {
            overallStatus = "PARTIAL_SUCCESS";
        }
    }
    
    public Object getResolvedValue(String fieldName) {
        return resolvedValues.get(fieldName);
    }
    
    public String getResolutionError(String fieldName) {
        return resolutionErrors.get(fieldName);
    }
    
    public boolean hasResolvedValue(String fieldName) {
        return resolvedValues.containsKey(fieldName);
    }
    
    public boolean hasResolutionError(String fieldName) {
        return resolutionErrors.containsKey(fieldName);
    }
    
    public int getTotalFields() {
        return successfulFields + failedFields;
    }
    
    public double getSuccessRate() {
        int total = getTotalFields();
        return total > 0 ? (double) successfulFields / total : 1.0;
    }
    
    public void markAsCompleteFailure(String reason) {
        overallStatus = "FAILURE";
        hasErrors = true;
        if (reason != null) {
            resolutionErrors.put("SYSTEM_ERROR", reason);
        }
    }
    
    // Getters and Setters
    public Map<String, Object> getResolvedValues() {
        return resolvedValues;
    }
    
    public void setResolvedValues(Map<String, Object> resolvedValues) {
        this.resolvedValues = resolvedValues != null ? resolvedValues : new HashMap<>();
    }
    
    public Map<String, String> getResolutionErrors() {
        return resolutionErrors;
    }
    
    public void setResolutionErrors(Map<String, String> resolutionErrors) {
        this.resolutionErrors = resolutionErrors != null ? resolutionErrors : new HashMap<>();
    }
    
    public long getTotalExecutionTimeMs() {
        return totalExecutionTimeMs;
    }
    
    public void setTotalExecutionTimeMs(long totalExecutionTimeMs) {
        this.totalExecutionTimeMs = totalExecutionTimeMs;
    }
    
    public LocalDateTime getExecutionTime() {
        return executionTime;
    }
    
    public void setExecutionTime(LocalDateTime executionTime) {
        this.executionTime = executionTime;
    }
    
    public int getSuccessfulFields() {
        return successfulFields;
    }
    
    public void setSuccessfulFields(int successfulFields) {
        this.successfulFields = successfulFields;
    }
    
    public int getFailedFields() {
        return failedFields;
    }
    
    public void setFailedFields(int failedFields) {
        this.failedFields = failedFields;
    }
    
    public boolean isHasErrors() {
        return hasErrors;
    }
    
    public void setHasErrors(boolean hasErrors) {
        this.hasErrors = hasErrors;
    }
    
    public String getOverallStatus() {
        return overallStatus;
    }
    
    public void setOverallStatus(String overallStatus) {
        this.overallStatus = overallStatus;
    }
    
    @Override
    public String toString() {
        return "FieldResolutionResult{" +
                "successfulFields=" + successfulFields +
                ", failedFields=" + failedFields +
                ", totalExecutionTimeMs=" + totalExecutionTimeMs +
                ", overallStatus='" + overallStatus + '\'' +
                ", successRate=" + String.format("%.2f", getSuccessRate() * 100) + "%" +
                '}';
    }
}