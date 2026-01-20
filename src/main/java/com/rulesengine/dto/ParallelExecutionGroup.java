package com.rulesengine.dto;

import com.rulesengine.entity.FieldConfigEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Collection of independent data services that can be executed concurrently
 */
public class ParallelExecutionGroup {
    
    private List<FieldConfigEntity> fields;
    private String groupName;
    private int priority;
    private long estimatedExecutionTimeMs;
    
    public ParallelExecutionGroup() {
        this.fields = new ArrayList<>();
        this.priority = 0;
    }
    
    public ParallelExecutionGroup(String groupName) {
        this.groupName = groupName;
        this.fields = new ArrayList<>();
        this.priority = 0;
    }
    
    public ParallelExecutionGroup(String groupName, List<FieldConfigEntity> fields) {
        this.groupName = groupName;
        this.fields = fields != null ? new ArrayList<>(fields) : new ArrayList<>();
        this.priority = 0;
    }
    
    // Business methods
    public void addField(FieldConfigEntity field) {
        if (field != null && !fields.contains(field)) {
            fields.add(field);
        }
    }
    
    public void removeField(FieldConfigEntity field) {
        fields.remove(field);
    }
    
    public boolean containsField(String fieldName) {
        return fields.stream().anyMatch(f -> f.getFieldName().equals(fieldName));
    }
    
    public int getFieldCount() {
        return fields.size();
    }
    
    public int getServiceCount() {
        // Count unique data services in this group
        return (int) fields.stream()
                .filter(f -> f.hasDataService())
                .map(f -> f.getDataServiceConfigJson())
                .distinct()
                .count();
    }
    
    public List<String> getFieldNames() {
        return fields.stream()
                .map(FieldConfigEntity::getFieldName)
                .toList();
    }
    
    public boolean isEmpty() {
        return fields.isEmpty();
    }
    
    // Getters and Setters
    public List<FieldConfigEntity> getFields() {
        return fields;
    }
    
    public void setFields(List<FieldConfigEntity> fields) {
        this.fields = fields != null ? fields : new ArrayList<>();
    }
    
    public String getGroupName() {
        return groupName;
    }
    
    public void setGroupName(String groupName) {
        this.groupName = groupName;
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
        return "ParallelExecutionGroup{" +
                "groupName='" + groupName + '\'' +
                ", fieldCount=" + getFieldCount() +
                ", serviceCount=" + getServiceCount() +
                ", priority=" + priority +
                ", estimatedTimeMs=" + estimatedExecutionTimeMs +
                '}';
    }
}