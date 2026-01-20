package com.rulesengine.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.Map;

@Schema(description = "Request to update an existing entity type")
public class UpdateEntityTypeRequest {

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    @Schema(description = "Updated description of the entity type")
    @JsonProperty("description")
    private String description;

    @Schema(description = "Updated data service configuration for retrieving entity data")
    @JsonProperty("dataServiceConfig")
    private DataServiceConfigDto dataServiceConfig;

    @Schema(description = "Updated field mappings for extracting entity properties")
    @JsonProperty("fieldMappings")
    private Map<String, String> fieldMappings;

    @Schema(description = "Updated parent entity type for inheritance")
    @JsonProperty("parentTypeName")
    private String parentTypeName;

    @Schema(description = "Updated metadata for the entity type")
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    // Constructors
    public UpdateEntityTypeRequest() {}

    // Getters and Setters
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
        return "UpdateEntityTypeRequest{" +
                "description='" + description + '\'' +
                ", parentTypeName='" + parentTypeName + '\'' +
                '}';
    }
}