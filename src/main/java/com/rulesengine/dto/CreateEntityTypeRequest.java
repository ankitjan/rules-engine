package com.rulesengine.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

@Schema(description = "Request to create a new entity type")
public class CreateEntityTypeRequest {

    @NotBlank(message = "Type name is required")
    @Size(max = 100, message = "Type name must not exceed 100 characters")
    @Schema(description = "Unique name for the entity type", example = "Customer")
    @JsonProperty("typeName")
    private String typeName;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    @Schema(description = "Description of the entity type", example = "Customer entity with profile and transaction data")
    @JsonProperty("description")
    private String description;

    @Schema(description = "Data service configuration for retrieving entity data")
    @JsonProperty("dataServiceConfig")
    private DataServiceConfigDto dataServiceConfig;

    @Schema(description = "Field mappings for extracting entity properties")
    @JsonProperty("fieldMappings")
    private Map<String, String> fieldMappings;

    @Schema(description = "Parent entity type for inheritance", example = "BaseEntity")
    @JsonProperty("parentTypeName")
    private String parentTypeName;

    @Schema(description = "Additional metadata for the entity type")
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    // Constructors
    public CreateEntityTypeRequest() {}

    public CreateEntityTypeRequest(String typeName, String description, DataServiceConfigDto dataServiceConfig) {
        this.typeName = typeName;
        this.description = description;
        this.dataServiceConfig = dataServiceConfig;
    }

    // Getters and Setters
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

    @Override
    public String toString() {
        return "CreateEntityTypeRequest{" +
                "typeName='" + typeName + '\'' +
                ", description='" + description + '\'' +
                ", parentTypeName='" + parentTypeName + '\'' +
                '}';
    }
}