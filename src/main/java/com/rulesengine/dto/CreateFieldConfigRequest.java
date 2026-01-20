package com.rulesengine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateFieldConfigRequest {

    @NotBlank(message = "Field name is required")
    @Size(max = 255, message = "Field name must not exceed 255 characters")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]*$", message = "Field name must start with a letter and contain only letters, numbers, and underscores")
    private String fieldName;

    @NotBlank(message = "Field type is required")
    @Pattern(regexp = "^(STRING|NUMBER|DATE|BOOLEAN|ARRAY|OBJECT)$", message = "Field type must be one of: STRING, NUMBER, DATE, BOOLEAN, ARRAY, OBJECT")
    private String fieldType;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private String dataServiceConfigJson;

    @Size(max = 500, message = "Mapper expression must not exceed 500 characters")
    private String mapperExpression;

    @NotNull(message = "Calculated field flag is required")
    private Boolean isCalculated = false;

    private String calculatorConfigJson;

    private String dependenciesJson;

    @Size(max = 500, message = "Default value must not exceed 500 characters")
    private String defaultValue;

    @NotNull(message = "Required field flag is required")
    private Boolean isRequired = false;

    // Constructors
    public CreateFieldConfigRequest() {}

    public CreateFieldConfigRequest(String fieldName, String fieldType, String description) {
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.description = description;
    }

    // Getters and Setters
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

    @Override
    public String toString() {
        return "CreateFieldConfigRequest{" +
                "fieldName='" + fieldName + '\'' +
                ", fieldType='" + fieldType + '\'' +
                ", isCalculated=" + isCalculated +
                ", isRequired=" + isRequired +
                '}';
    }
}