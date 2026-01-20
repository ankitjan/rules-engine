package com.rulesengine.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "Field configuration response optimized for Rule Builder UI")
public class BuilderFieldConfigResponse {

    @Schema(description = "Field configuration ID", example = "1")
    private Long id;

    @Schema(description = "Field name", example = "user_age")
    @JsonProperty("fieldName")
    private String fieldName;

    @Schema(description = "Field type", example = "NUMBER", allowableValues = {"STRING", "NUMBER", "DATE", "BOOLEAN", "ARRAY", "OBJECT"})
    @JsonProperty("fieldType")
    private String fieldType;

    @Schema(description = "Field description", example = "User's age in years")
    private String description;

    @Schema(description = "Available operators for this field type")
    @JsonProperty("availableOperators")
    private List<OperatorInfo> availableOperators;

    @Schema(description = "Validation rules and constraints")
    @JsonProperty("validationRules")
    private ValidationRules validationRules;

    @Schema(description = "Whether this field has searchable values")
    @JsonProperty("hasSearchableValues")
    private Boolean hasSearchableValues;

    @Schema(description = "Whether this field is calculated")
    @JsonProperty("isCalculated")
    private Boolean isCalculated;

    @Schema(description = "Field dependencies for calculated fields")
    private List<String> dependencies;

    @Schema(description = "Default value for the field")
    @JsonProperty("defaultValue")
    private String defaultValue;

    @Schema(description = "Whether this field is required")
    @JsonProperty("isRequired")
    private Boolean isRequired;

    @Schema(description = "Field configuration version")
    private Integer version;

    @Schema(description = "Creation timestamp")
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @Schema(description = "Additional metadata for UI rendering")
    private Map<String, Object> metadata;

    // Constructors
    public BuilderFieldConfigResponse() {}

    public BuilderFieldConfigResponse(Long id, String fieldName, String fieldType, String description,
                                    List<OperatorInfo> availableOperators, ValidationRules validationRules,
                                    Boolean hasSearchableValues, Boolean isCalculated, List<String> dependencies,
                                    String defaultValue, Boolean isRequired, Integer version,
                                    LocalDateTime createdAt, Map<String, Object> metadata) {
        this.id = id;
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.description = description;
        this.availableOperators = availableOperators;
        this.validationRules = validationRules;
        this.hasSearchableValues = hasSearchableValues;
        this.isCalculated = isCalculated;
        this.dependencies = dependencies;
        this.defaultValue = defaultValue;
        this.isRequired = isRequired;
        this.version = version;
        this.createdAt = createdAt;
        this.metadata = metadata;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getFieldType() { return fieldType; }
    public void setFieldType(String fieldType) { this.fieldType = fieldType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<OperatorInfo> getAvailableOperators() { return availableOperators; }
    public void setAvailableOperators(List<OperatorInfo> availableOperators) { this.availableOperators = availableOperators; }

    public ValidationRules getValidationRules() { return validationRules; }
    public void setValidationRules(ValidationRules validationRules) { this.validationRules = validationRules; }

    public Boolean getHasSearchableValues() { return hasSearchableValues; }
    public void setHasSearchableValues(Boolean hasSearchableValues) { this.hasSearchableValues = hasSearchableValues; }

    public Boolean getIsCalculated() { return isCalculated; }
    public void setIsCalculated(Boolean isCalculated) { this.isCalculated = isCalculated; }

    public List<String> getDependencies() { return dependencies; }
    public void setDependencies(List<String> dependencies) { this.dependencies = dependencies; }

    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }

    public Boolean getIsRequired() { return isRequired; }
    public void setIsRequired(Boolean isRequired) { this.isRequired = isRequired; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    @Schema(description = "Operator information for field types")
    public static class OperatorInfo {
        @Schema(description = "Operator code", example = "EQUALS")
        private String operator;

        @Schema(description = "Display label", example = "equals")
        private String label;

        @Schema(description = "Operator description", example = "Exact match comparison")
        private String description;

        @Schema(description = "Whether operator requires a value")
        @JsonProperty("requiresValue")
        private Boolean requiresValue;

        @Schema(description = "Expected value type for this operator")
        @JsonProperty("valueType")
        private String valueType;

        public OperatorInfo() {}

        public OperatorInfo(String operator, String label, String description, Boolean requiresValue, String valueType) {
            this.operator = operator;
            this.label = label;
            this.description = description;
            this.requiresValue = requiresValue;
            this.valueType = valueType;
        }

        // Getters and Setters
        public String getOperator() { return operator; }
        public void setOperator(String operator) { this.operator = operator; }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public Boolean getRequiresValue() { return requiresValue; }
        public void setRequiresValue(Boolean requiresValue) { this.requiresValue = requiresValue; }

        public String getValueType() { return valueType; }
        public void setValueType(String valueType) { this.valueType = valueType; }
    }

    @Schema(description = "Validation rules and constraints for fields")
    public static class ValidationRules {
        @Schema(description = "Minimum value for numeric fields")
        private Object minValue;

        @Schema(description = "Maximum value for numeric fields")
        private Object maxValue;

        @Schema(description = "Minimum length for string fields")
        @JsonProperty("minLength")
        private Integer minLength;

        @Schema(description = "Maximum length for string fields")
        @JsonProperty("maxLength")
        private Integer maxLength;

        @Schema(description = "Regular expression pattern for string validation")
        private String pattern;

        @Schema(description = "List of allowed values")
        @JsonProperty("allowedValues")
        private List<Object> allowedValues;

        @Schema(description = "Custom validation message")
        @JsonProperty("validationMessage")
        private String validationMessage;

        public ValidationRules() {}

        // Getters and Setters
        public Object getMinValue() { return minValue; }
        public void setMinValue(Object minValue) { this.minValue = minValue; }

        public Object getMaxValue() { return maxValue; }
        public void setMaxValue(Object maxValue) { this.maxValue = maxValue; }

        public Integer getMinLength() { return minLength; }
        public void setMinLength(Integer minLength) { this.minLength = minLength; }

        public Integer getMaxLength() { return maxLength; }
        public void setMaxLength(Integer maxLength) { this.maxLength = maxLength; }

        public String getPattern() { return pattern; }
        public void setPattern(String pattern) { this.pattern = pattern; }

        public List<Object> getAllowedValues() { return allowedValues; }
        public void setAllowedValues(List<Object> allowedValues) { this.allowedValues = allowedValues; }

        public String getValidationMessage() { return validationMessage; }
        public void setValidationMessage(String validationMessage) { this.validationMessage = validationMessage; }
    }
}