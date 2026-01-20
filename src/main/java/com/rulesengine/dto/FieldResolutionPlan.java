package com.rulesengine.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Execution plan that defines the order of field retrieval and calculation
 */
public class FieldResolutionPlan {
    
    private List<ParallelExecutionGroup> parallelGroups;
    private List<SequentialExecutionChain> sequentialChains;
    private Map<String, Object> staticFieldValues;
    private long estimatedExecutionTimeMs;
    private Map<String, Integer> calculatedFieldLevels; // Dependency levels for calculated fields
    private List<String> calculatedFieldOrder; // Optimal calculation order
    
    public FieldResolutionPlan() {
        this.parallelGroups = new ArrayList<>();
        this.sequentialChains = new ArrayList<>();
        this.calculatedFieldLevels = new HashMap<>();
        this.calculatedFieldOrder = new ArrayList<>();
    }
    
    public FieldResolutionPlan(List<ParallelExecutionGroup> parallelGroups, 
                              List<SequentialExecutionChain> sequentialChains) {
        this.parallelGroups = parallelGroups != null ? parallelGroups : new ArrayList<>();
        this.sequentialChains = sequentialChains != null ? sequentialChains : new ArrayList<>();
        this.calculatedFieldLevels = new HashMap<>();
        this.calculatedFieldOrder = new ArrayList<>();
    }
    
    // Business methods
    public boolean hasParallelExecution() {
        return parallelGroups != null && !parallelGroups.isEmpty();
    }
    
    public boolean hasSequentialExecution() {
        return sequentialChains != null && !sequentialChains.isEmpty();
    }
    
    public int getTotalFieldCount() {
        int count = 0;
        if (parallelGroups != null) {
            count += parallelGroups.stream().mapToInt(ParallelExecutionGroup::getFieldCount).sum();
        }
        if (sequentialChains != null) {
            count += sequentialChains.stream().mapToInt(SequentialExecutionChain::getFieldCount).sum();
        }
        return count;
    }
    
    public int getTotalServiceCount() {
        int count = 0;
        if (parallelGroups != null) {
            count += parallelGroups.stream().mapToInt(ParallelExecutionGroup::getServiceCount).sum();
        }
        if (sequentialChains != null) {
            count += sequentialChains.stream().mapToInt(SequentialExecutionChain::getServiceCount).sum();
        }
        return count;
    }
    
    public int getTotalCalculatedFields() {
        return calculatedFieldOrder != null ? calculatedFieldOrder.size() : 0;
    }
    
    public boolean hasCalculatedFields() {
        return calculatedFieldOrder != null && !calculatedFieldOrder.isEmpty();
    }
    
    // Getters and Setters for new fields
    public Map<String, Integer> getCalculatedFieldLevels() {
        return calculatedFieldLevels;
    }
    
    public void setCalculatedFieldLevels(Map<String, Integer> calculatedFieldLevels) {
        this.calculatedFieldLevels = calculatedFieldLevels;
    }
    
    public List<String> getCalculatedFieldOrder() {
        return calculatedFieldOrder;
    }
    
    public void setCalculatedFieldOrder(List<String> calculatedFieldOrder) {
        this.calculatedFieldOrder = calculatedFieldOrder;
    }
    
    // Getters and Setters
    public List<ParallelExecutionGroup> getParallelGroups() {
        return parallelGroups;
    }
    
    public void setParallelGroups(List<ParallelExecutionGroup> parallelGroups) {
        this.parallelGroups = parallelGroups;
    }
    
    public List<SequentialExecutionChain> getSequentialChains() {
        return sequentialChains;
    }
    
    public void setSequentialChains(List<SequentialExecutionChain> sequentialChains) {
        this.sequentialChains = sequentialChains;
    }
    
    public Map<String, Object> getStaticFieldValues() {
        return staticFieldValues;
    }
    
    public void setStaticFieldValues(Map<String, Object> staticFieldValues) {
        this.staticFieldValues = staticFieldValues;
    }
    
    public long getEstimatedExecutionTimeMs() {
        return estimatedExecutionTimeMs;
    }
    
    public void setEstimatedExecutionTimeMs(long estimatedExecutionTimeMs) {
        this.estimatedExecutionTimeMs = estimatedExecutionTimeMs;
    }
    
    @Override
    public String toString() {
        return "FieldResolutionPlan{" +
                "parallelGroups=" + (parallelGroups != null ? parallelGroups.size() : 0) +
                ", sequentialChains=" + (sequentialChains != null ? sequentialChains.size() : 0) +
                ", totalFields=" + getTotalFieldCount() +
                ", totalServices=" + getTotalServiceCount() +
                ", calculatedFields=" + getTotalCalculatedFields() +
                ", estimatedTimeMs=" + estimatedExecutionTimeMs +
                '}';
    }
}