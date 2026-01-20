package com.rulesengine.dto;

import com.rulesengine.entity.FieldConfigEntity;

import java.time.LocalDateTime;

public class FieldConfigResponse {

    private Long id;
    private String fieldName;
    private String fieldType;
    private String description;
    private String dataServiceConfigJson;
    private String mapperExpression;
    private Boolean isCalculated;
    private String calculatorConfigJson;
    private String dependenciesJson;
    private String defaultValue;
    private Boolean isRequired;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    // Constructors
    public FieldConfigResponse() {}

    // Static factory method
    public static FieldConfigResponse from(FieldConfigEntity entity) {
        FieldConfigResponse response = new FieldConfigResponse();
        response.setId(entity.getId());
        response.setFieldName(entity.getFieldName());
        response.setFieldType(entity.getFieldType());
        response.setDescription(entity.getDescription());
        response.setDataServiceConfigJson(entity.getDataServiceConfigJson());
        response.setMapperExpression(entity.getMapperExpression());
        response.setIsCalculated(entity.getIsCalculated());
        response.setCalculatorConfigJson(entity.getCalculatorConfigJson());
        response.setDependenciesJson(entity.getDependenciesJson());
        response.setDefaultValue(entity.getDefaultValue());
        response.setIsRequired(entity.getIsRequired());
        response.setVersion(entity.getVersion());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        response.setCreatedBy(entity.getCreatedBy());
        response.setUpdatedBy(entity.getUpdatedBy());
        return response;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDataServiceConfigJson() {
        return dataServiceConfigJson;
    }

    public void setDataServiceConfigJson(String dataServiceConfigJson) {
        this.dataServiceConfigJson = dataServiceConfigJson;
    }

    public String getMapperExpression() {
        return mapperExpression;
    }

    public void setMapperExpression(String mapperExpression) {
        this.mapperExpression = mapperExpression;
    }

    public Boolean getIsCalculated() {
        return isCalculated;
    }

    public void setIsCalculated(Boolean isCalculated) {
        this.isCalculated = isCalculated;
    }

    public String getCalculatorConfigJson() {
        return calculatorConfigJson;
    }

    public void setCalculatorConfigJson(String calculatorConfigJson) {
        this.calculatorConfigJson = calculatorConfigJson;
    }

    public String getDependenciesJson() {
        return dependenciesJson;
    }

    public void setDependenciesJson(String dependenciesJson) {
        this.dependenciesJson = dependenciesJson;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Boolean getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(Boolean isRequired) {
        this.isRequired = isRequired;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @Override
    public String toString() {
        return "FieldConfigResponse{" +
                "id=" + id +
                ", fieldName='" + fieldName + '\'' +
                ", fieldType='" + fieldType + '\'' +
                ", isCalculated=" + isCalculated +
                ", isRequired=" + isRequired +
                ", version=" + version +
                '}';
    }
}