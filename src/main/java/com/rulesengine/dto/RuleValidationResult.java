package com.rulesengine.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Result of rule validation with detailed error information")
public class RuleValidationResult {

    @Schema(description = "Whether the rule is valid")
    @JsonProperty("isValid")
    private Boolean isValid;

    @Schema(description = "List of validation errors")
    @JsonProperty("errors")
    private List<ValidationError> errors;

    @Schema(description = "List of validation warnings")
    @JsonProperty("warnings")
    private List<ValidationWarning> warnings;

    @Schema(description = "Suggested corrections for errors")
    @JsonProperty("suggestions")
    private List<ValidationSuggestion> suggestions;

    @Schema(description = "Summary of validation results")
    @JsonProperty("summary")
    private String summary;

    @Schema(description = "Additional validation metadata")
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    // Constructors
    public RuleValidationResult() {}

    public RuleValidationResult(Boolean isValid) {
        this.isValid = isValid;
    }

    public RuleValidationResult(Boolean isValid, List<ValidationError> errors, List<ValidationWarning> warnings,
                              List<ValidationSuggestion> suggestions, String summary, Map<String, Object> metadata) {
        this.isValid = isValid;
        this.errors = errors;
        this.warnings = warnings;
        this.suggestions = suggestions;
        this.summary = summary;
        this.metadata = metadata;
    }

    // Getters and Setters
    public Boolean getIsValid() { return isValid; }
    public void setIsValid(Boolean isValid) { this.isValid = isValid; }

    public List<ValidationError> getErrors() { return errors; }
    public void setErrors(List<ValidationError> errors) { this.errors = errors; }

    public List<ValidationWarning> getWarnings() { return warnings; }
    public void setWarnings(List<ValidationWarning> warnings) { this.warnings = warnings; }

    public List<ValidationSuggestion> getSuggestions() { return suggestions; }
    public void setSuggestions(List<ValidationSuggestion> suggestions) { this.suggestions = suggestions; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    @Schema(description = "Validation error with location information")
    public static class ValidationError {
        @Schema(description = "Error code")
        @JsonProperty("code")
        private String code;

        @Schema(description = "Error message")
        @JsonProperty("message")
        private String message;

        @Schema(description = "Path to the error location in the rule")
        @JsonProperty("path")
        private String path;

        @Schema(description = "Field name related to the error")
        @JsonProperty("fieldName")
        private String fieldName;

        @Schema(description = "Error severity", allowableValues = {"ERROR", "WARNING", "INFO"})
        @JsonProperty("severity")
        private String severity;

        @Schema(description = "Additional error context")
        @JsonProperty("context")
        private Map<String, Object> context;

        public ValidationError() {}

        public ValidationError(String code, String message, String path) {
            this.code = code;
            this.message = message;
            this.path = path;
            this.severity = "ERROR";
        }

        public ValidationError(String code, String message, String path, String fieldName, String severity) {
            this.code = code;
            this.message = message;
            this.path = path;
            this.fieldName = fieldName;
            this.severity = severity;
        }

        // Getters and Setters
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        public String getFieldName() { return fieldName; }
        public void setFieldName(String fieldName) { this.fieldName = fieldName; }

        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }

        public Map<String, Object> getContext() { return context; }
        public void setContext(Map<String, Object> context) { this.context = context; }
    }

    @Schema(description = "Validation warning")
    public static class ValidationWarning {
        @Schema(description = "Warning code")
        @JsonProperty("code")
        private String code;

        @Schema(description = "Warning message")
        @JsonProperty("message")
        private String message;

        @Schema(description = "Path to the warning location")
        @JsonProperty("path")
        private String path;

        public ValidationWarning() {}

        public ValidationWarning(String code, String message, String path) {
            this.code = code;
            this.message = message;
            this.path = path;
        }

        // Getters and Setters
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
    }

    @Schema(description = "Validation suggestion for fixing errors")
    public static class ValidationSuggestion {
        @Schema(description = "Suggestion type")
        @JsonProperty("type")
        private String type;

        @Schema(description = "Suggestion message")
        @JsonProperty("message")
        private String message;

        @Schema(description = "Path where suggestion applies")
        @JsonProperty("path")
        private String path;

        @Schema(description = "Suggested fix or replacement")
        @JsonProperty("suggestedFix")
        private Object suggestedFix;

        public ValidationSuggestion() {}

        public ValidationSuggestion(String type, String message, String path) {
            this.type = type;
            this.message = message;
            this.path = path;
        }

        public ValidationSuggestion(String type, String message, String path, Object suggestedFix) {
            this.type = type;
            this.message = message;
            this.path = path;
            this.suggestedFix = suggestedFix;
        }

        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        public Object getSuggestedFix() { return suggestedFix; }
        public void setSuggestedFix(Object suggestedFix) { this.suggestedFix = suggestedFix; }
    }
}