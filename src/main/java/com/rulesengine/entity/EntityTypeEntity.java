package com.rulesengine.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "entity_types")
public class EntityTypeEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Type name is required")
    @Column(name = "type_name", nullable = false, unique = true)
    private String typeName;

    @Column(length = 1000)
    private String description;

    @NotBlank(message = "Data service configuration is required")
    @Column(name = "data_service_config", columnDefinition = "TEXT", nullable = false)
    private String dataServiceConfigJson;

    @Column(name = "field_mappings", columnDefinition = "TEXT")
    private String fieldMappingsJson;

    @Column(name = "parent_type_name")
    private String parentTypeName;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "is_active")
    private Boolean isActive = true;

    // Constructors
    public EntityTypeEntity() {}

    public EntityTypeEntity(String typeName, String description, String dataServiceConfigJson) {
        this.typeName = typeName;
        this.description = description;
        this.dataServiceConfigJson = dataServiceConfigJson;
    }

    // Business methods
    public boolean hasFieldMappings() {
        return fieldMappingsJson != null && !fieldMappingsJson.trim().isEmpty();
    }

    public boolean hasDataServiceConfig() {
        return dataServiceConfigJson != null && !dataServiceConfigJson.trim().isEmpty();
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

    public String getDataServiceConfigJson() {
        return dataServiceConfigJson;
    }

    public void setDataServiceConfigJson(String dataServiceConfigJson) {
        this.dataServiceConfigJson = dataServiceConfigJson;
    }

    public String getFieldMappingsJson() {
        return fieldMappingsJson;
    }

    public void setFieldMappingsJson(String fieldMappingsJson) {
        this.fieldMappingsJson = fieldMappingsJson;
    }

    public String getParentTypeName() {
        return parentTypeName;
    }

    public void setParentTypeName(String parentTypeName) {
        this.parentTypeName = parentTypeName;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    @Override
    public String toString() {
        return "EntityTypeEntity{" +
                "id=" + id +
                ", typeName='" + typeName + '\'' +
                ", description='" + description + '\'' +
                ", hasFieldMappings=" + hasFieldMappings() +
                '}';
    }
}