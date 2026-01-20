package com.rulesengine.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

@Schema(description = "Request for real-time rule validation")
public class RuleValidationRequest {

    @Schema(description = "Rule definition to validate", required = true)
    @JsonProperty("ruleDefinition")
    @NotNull(message = "Rule definition is required")
    private Object ruleDefinition;

    @Schema(description = "Context for validation (e.g., available fields, entity type)")
    @JsonProperty("validationContext")
    private Map<String, Object> validationContext;

    @Schema(description = "Whether to perform deep validation including field references")
    @JsonProperty("deepValidation")
    private Boolean deepValidation = true;

    @Schema(description = "Whether to check operator compatibility")
    @JsonProperty("checkOperatorCompatibility")
    private Boolean checkOperatorCompatibility = true;

    @Schema(description = "Whether to validate value constraints")
    @JsonProperty("validateValueConstraints")
    private Boolean validateValueConstraints = true;

    @Schema(description = "Whether to check field dependencies for calculated fields")
    @JsonProperty("checkFieldDependencies")
    private Boolean checkFieldDependencies = true;

    // Constructors
    public RuleValidationRequest() {}

    public RuleValidationRequest(Object ruleDefinition) {
        this.ruleDefinition = ruleDefinition;
    }

    public RuleValidationRequest(Object ruleDefinition, Map<String, Object> validationContext) {
        this.ruleDefinition = ruleDefinition;
        this.validationContext = validationContext;
    }

    // Getters and Setters
    public Object getRuleDefinition() { return ruleDefinition; }
    public void setRuleDefinition(Object ruleDefinition) { this.ruleDefinition = ruleDefinition; }

    public Map<String, Object> getValidationContext() { return validationContext; }
    public void setValidationContext(Map<String, Object> validationContext) { this.validationContext = validationContext; }

    public Boolean getDeepValidation() { return deepValidation; }
    public void setDeepValidation(Boolean deepValidation) { this.deepValidation = deepValidation; }

    public Boolean getCheckOperatorCompatibility() { return checkOperatorCompatibility; }
    public void setCheckOperatorCompatibility(Boolean checkOperatorCompatibility) { this.checkOperatorCompatibility = checkOperatorCompatibility; }

    public Boolean getValidateValueConstraints() { return validateValueConstraints; }
    public void setValidateValueConstraints(Boolean validateValueConstraints) { this.validateValueConstraints = validateValueConstraints; }

    public Boolean getCheckFieldDependencies() { return checkFieldDependencies; }
    public void setCheckFieldDependencies(Boolean checkFieldDependencies) { this.checkFieldDependencies = checkFieldDependencies; }
}