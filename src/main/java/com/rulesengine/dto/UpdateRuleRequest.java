package com.rulesengine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateRuleRequest {

    @NotBlank(message = "Rule name is required")
    @Size(max = 255, message = "Rule name must not exceed 255 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotBlank(message = "Rule definition is required")
    private String ruleDefinitionJson;

    private Long folderId;

    private Boolean isActive;

    // Constructors
    public UpdateRuleRequest() {}

    public UpdateRuleRequest(String name, String description, String ruleDefinitionJson, Long folderId, Boolean isActive) {
        this.name = name;
        this.description = description;
        this.ruleDefinitionJson = ruleDefinitionJson;
        this.folderId = folderId;
        this.isActive = isActive;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRuleDefinitionJson() {
        return ruleDefinitionJson;
    }

    public void setRuleDefinitionJson(String ruleDefinitionJson) {
        this.ruleDefinitionJson = ruleDefinitionJson;
    }

    public Long getFolderId() {
        return folderId;
    }

    public void setFolderId(Long folderId) {
        this.folderId = folderId;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    @Override
    public String toString() {
        return "UpdateRuleRequest{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", folderId=" + folderId +
                ", isActive=" + isActive +
                '}';
    }
}