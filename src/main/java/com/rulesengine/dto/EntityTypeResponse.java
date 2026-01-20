package com.rulesengine.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "Entity type information")
public class EntityTypeResponse {

    @Schema(description = "Unique identifier of the entity type", example = "1")
    @JsonProperty("id")
    private Long id;

    @Schema(description = "Name of the entity type", example = "Customer")
    @JsonProperty("typeName")
    private String typeName;

    @Schema(description = "Description of the entity type")
    @JsonProperty("description")
    private String description;

    @Schema(description = "Data service configuration for retrieving entity data")
    @JsonProperty("dataServiceConfig")
    private DataServiceConfigDto dataServiceConfig;

    @Schema(description = "Field mappings for extracting entity properties")
    @JsonProperty("fieldMappings")
    private Map<String, String> fieldMappings;

    @Schema(description = "Parent entity type for inheritance")
    @JsonProperty("parentTypeName")
    private String parentTypeName;

    @Schema(description = "Additional metadata for the entity type")
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @Schema(description = "Creation timestamp")
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;

    @Schema(description = "User who created the entity type")
    @JsonProperty("createdBy")
    private String createdBy;

    @Schema(description = "User who last updated the entity type")
    @JsonProperty("updatedBy")
    private String updatedBy;

    @Schema(description = "Whether the entity type is active")
    @JsonProperty("isActive")
    private Boolean isActive;

    // Constructors
    public EntityTypeResponse() {}

    public EntityTypeResponse(Long id, String typeName, String description) {
        this.id = id;
        this.typeName = typeName;
        this.description = description;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DataServiceConfigDto getDataServiceConfig() {
        return dataServiceConfig;
    }

    public void setDataServiceConfig(DataServiceConfigDto dataServiceConfig) {
        this.dataServiceConfig = dataServiceConfig;
    }

    public Map<String, String> getFieldMappings() {
        return fieldMappings;
    }

    public void setFieldMappings(Map<String, String> fieldMappings) {
        this.fieldMappings = fieldMappings;
    }

    public String getParentTypeName() {
        return parentTypeName;
    }

    public void setParentTypeName(String parentTypeName) {
        this.parentTypeName = parentTypeName;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    @Override
    public String toString() {
        return "EntityTypeResponse{" +
                "id=" + id +
                ", typeName='" + typeName + '\'' +
                ", description='" + description + '\'' +
                ", parentTypeName='" + parentTypeName + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}