package com.rulesengine.dto;

import com.rulesengine.entity.RuleEntity;

import java.time.LocalDateTime;

public class RuleResponse {

    private Long id;
    private String name;
    private String description;
    private String ruleDefinitionJson;
    private Long folderId;
    private String folderName;
    private Integer version;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    // Constructors
    public RuleResponse() {}

    public RuleResponse(RuleEntity entity) {
        this.id = entity.getId();
        this.name = entity.getName();
        this.description = entity.getDescription();
        this.ruleDefinitionJson = entity.getRuleDefinitionJson();
        this.folderId = entity.getFolder() != null ? entity.getFolder().getId() : null;
        this.folderName = entity.getFolder() != null ? entity.getFolder().getName() : null;
        this.version = entity.getVersion();
        this.isActive = entity.getIsActive();
        this.createdAt = entity.getCreatedAt();
        this.updatedAt = entity.getUpdatedAt();
        this.createdBy = entity.getCreatedBy();
        this.updatedBy = entity.getUpdatedBy();
    }

    // Static factory method
    public static RuleResponse from(RuleEntity entity) {
        return new RuleResponse(entity);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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
        return "RuleResponse{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", version=" + version +
                ", isActive=" + isActive +
                ", folderName='" + folderName + '\'' +
                '}';
    }
}