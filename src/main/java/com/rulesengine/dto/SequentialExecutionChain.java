package com.rulesengine.dto;

import com.rulesengine.entity.FieldConfigEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Ordered sequence of dependent data services that must execute in sequence
 */
public class SequentialExecutionChain {
    
    private List<FieldConfigEntity> orderedFields;
    private String chainName;
    private int priority;
    private long estimatedExecutionTimeMs;
    
    public SequentialExecutionChain() {
        this.orderedFields = new ArrayList<>();
        this.priority = 0;
    }
    
    public SequentialExecutionChain(String chainName) {
        this.chainName = chainName;
        this.orderedFields = new ArrayList<>();
        this.priority = 0;
    }
    
    public SequentialExecutionChain(String chainName, List<FieldConfigEntity> orderedFields) {
        this.chainName = chainName;
        this.orderedFields = orderedFields != null ? new ArrayList<>(orderedFields) : new ArrayList<>();
        this.priority = 0;
    }
    
    // Business methods
    public void addField(FieldConfigEntity field) {
        if (field != null && !orderedFields.contains(field)) {
            orderedFields.add(field);
        }
    }
    
    public void addFieldAtPosition(int position, FieldConfigEntity field) {
        if (field != null && !orderedFields.contains(field)) {
            if (position >= 0 && position <= orderedFields.size()) {
                orderedFields.add(position, field);
            } else {
                orderedFields.add(field);
            }
        }
    }
    
    public void removeField(FieldConfigEntity field) {
        orderedFields.remove(field);
    }
    
    public boolean containsField(String fieldName) {
        return orderedFields.stream().anyMatch(f -> f.getFieldName().equals(fieldName));
    }
    
    public int getFieldCount() {
        return orderedFields.size();
    }
    
    public int getServiceCount() {
        // Count unique data services in this chain
        return (int) orderedFields.stream()
                .filter(f -> f.hasDataService())
                .map(f -> f.getDataServiceConfigJson())
                .distinct()
                .count();
    }
    
    public List<String> getFieldNames() {
        return orderedFields.stream()
                .map(FieldConfigEntity::getFieldName)
                .toList();
    }
    
    public boolean isEmpty() {
        return orderedFields.isEmpty();
    }
    
    public FieldConfigEntity getFirstField() {
        return orderedFields.isEmpty() ? null : orderedFields.get(0);
    }
    
    public FieldConfigEntity getLastField() {
        return orderedFields.isEmpty() ? null : orderedFields.get(orderedFields.size() - 1);
    }
    
    // Getters and Setters
    public List<FieldConfigEntity> getOrderedFields() {
        return orderedFields;
    }
    
    public void setOrderedFields(List<FieldConfigEntity> orderedFields) {
        this.orderedFields = orderedFields != null ? orderedFields : new ArrayList<>();
    }
    
    public String getChainName() {
        return chainName;
    }
    
    public void setChainName(String chainName) {
        this.chainName = chainName;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public void setPriority(int priority) {
        this.priority = priority;
    }
    
    public long getEstimatedExecutionTimeMs() {
        return estimatedExecutionTimeMs;
    }
    
    public void setEstimatedExecutionTimeMs(long estimatedExecutionTimeMs) {
        this.estimatedExecutionTimeMs = estimatedExecutionTimeMs;
    }
    
    @Override
    public String toString() {
        return "SequentialExecutionChain{" +
                "chainName='" + chainName + '\'' +
                ", fieldCount=" + getFieldCount() +
                ", serviceCount=" + getServiceCount() +
                ", priority=" + priority +
                ", estimatedTimeMs=" + estimatedExecutionTimeMs +
                '}';
    }
}