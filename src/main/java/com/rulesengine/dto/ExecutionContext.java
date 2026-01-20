package com.rulesengine.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * Context for rule execution containing entity data and field values
 */
public class ExecutionContext {
    
    private Map<String, Object> fieldValues;
    private String entityId;
    private String entityType;
    private Map<String, Object> metadata;
    
    // Constructors
    public ExecutionContext() {
        this.fieldValues = new HashMap<>();
        this.metadata = new HashMap<>();
    }
    
    public ExecutionContext(Map<String, Object> fieldValues) {
        this.fieldValues = fieldValues != null ? new HashMap<>(fieldValues) : new HashMap<>();
        this.metadata = new HashMap<>();
    }
    
    public ExecutionContext(String entityId, String entityType, Map<String, Object> fieldValues) {
        this.entityId = entityId;
        this.entityType = entityType;
        this.fieldValues = fieldValues != null ? new HashMap<>(fieldValues) : new HashMap<>();
        this.metadata = new HashMap<>();
    }
    
    // Utility methods
    public void addFieldValue(String fieldName, Object value) {
        this.fieldValues.put(fieldName, value);
    }
    
    public Object getFieldValue(String fieldName) {
        return this.fieldValues.get(fieldName);
    }
    
    public boolean hasFieldValue(String fieldName) {
        return this.fieldValues.containsKey(fieldName);
    }
    
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }
    
    public Object getMetadata(String key) {
        return this.metadata.get(key);
    }
    
    // Getters and Setters
    public Map<String, Object> getFieldValues() {
        return fieldValues;
    }
    
    public void setFieldValues(Map<String, Object> fieldValues) {
        this.fieldValues = fieldValues != null ? fieldValues : new HashMap<>();
    }
    
    public String getEntityId() {
        return entityId;
    }
    
    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }
    
    public String getEntityType() {
        return entityType;
    }
    
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }
    
    @Override
    public String toString() {
        return "ExecutionContext{" +
                "entityId='" + entityId + '\'' +
                ", entityType='" + entityType + '\'' +
                ", fieldValues=" + fieldValues.keySet() +
                ", metadata=" + metadata.keySet() +
                '}';
    }
}