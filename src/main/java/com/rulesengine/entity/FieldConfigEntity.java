package com.rulesengine.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "field_configs")
public class FieldConfigEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Field name is required")
    @Column(name = "field_name", nullable = false, unique = true)
    private String fieldName;

    @NotBlank(message = "Field type is required")
    @Column(name = "field_type", nullable = false)
    private String fieldType; // STRING, NUMBER, DATE, BOOLEAN, ARRAY, OBJECT

    @Column(length = 1000)
    private String description;

    @Column(name = "data_service_config", columnDefinition = "TEXT")
    private String dataServiceConfigJson;

    @Column(name = "mapper_expression", length = 500)
    private String mapperExpression;

    @NotNull(message = "Calculated field flag is required")
    @Column(name = "is_calculated", nullable = false)
    private Boolean isCalculated = false;

    @Column(name = "calculator_config", columnDefinition = "TEXT")
    private String calculatorConfigJson;

    @Column(name = "dependencies", columnDefinition = "TEXT")
    private String dependenciesJson; // JSON array of field names this field depends on

    @Column(name = "default_value", length = 500)
    private String defaultValue;

    @NotNull(message = "Required field flag is required")
    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = false;

    @NotNull(message = "Version is required")
    @Column(nullable = false)
    private Integer version = 1;

    // Constructors
    public FieldConfigEntity() {}

    public FieldConfigEntity(String fieldName, String fieldType, String description) {
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.description = description;
    }

    // Business methods
    public void incrementVersion() {
        this.version = (this.version == null) ? 1 : this.version + 1;
    }

    public boolean hasDataService() {
        return dataServiceConfigJson != null && !dataServiceConfigJson.trim().isEmpty();
    }

    public boolean hasMapper() {
        return mapperExpression != null && !mapperExpression.trim().isEmpty();
    }

    public boolean hasCalculator() {
        return isCalculated && calculatorConfigJson != null && !calculatorConfigJson.trim().isEmpty();
    }

    public boolean hasDependencies() {
        return dependenciesJson != null && !dependenciesJson.trim().isEmpty();
    }

    public boolean hasDefaultValue() {
        return defaultValue != null && !defaultValue.trim().isEmpty();
    }

    // Validation methods
    public boolean isValidFieldType() {
        return fieldType != null && 
               (fieldType.equals("STRING") || fieldType.equals("NUMBER") || 
                fieldType.equals("DATE") || fieldType.equals("BOOLEAN") || 
                fieldType.equals("ARRAY") || fieldType.equals("OBJECT"));
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

    @Override
    public String toString() {
        return "FieldConfigEntity{" +
                "id=" + id +
                ", fieldName='" + fieldName + '\'' +
                ", fieldType='" + fieldType + '\'' +
                ", isCalculated=" + isCalculated +
                ", isRequired=" + isRequired +
                ", version=" + version +
                '}';
    }
}