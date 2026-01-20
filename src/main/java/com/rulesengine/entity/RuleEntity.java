package com.rulesengine.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Entity
@Table(name = "rules")
public class RuleEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Rule name is required")
    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @NotBlank(message = "Rule definition is required")
    @Column(name = "rule_definition", columnDefinition = "TEXT", nullable = false)
    private String ruleDefinitionJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private FolderEntity folder;

    @NotNull(message = "Version is required")
    @Column(nullable = false)
    private Integer version = 1;

    @NotNull(message = "Active status is required")
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RuleVersionEntity> versions;

    // Constructors
    public RuleEntity() {}

    public RuleEntity(String name, String description, String ruleDefinitionJson) {
        this.name = name;
        this.description = description;
        this.ruleDefinitionJson = ruleDefinitionJson;
    }

    // Business methods
    public void incrementVersion() {
        this.version = (this.version == null) ? 1 : this.version + 1;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
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

    public FolderEntity getFolder() {
        return folder;
    }

    public void setFolder(FolderEntity folder) {
        this.folder = folder;
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

    public List<RuleVersionEntity> getVersions() {
        return versions;
    }

    public void setVersions(List<RuleVersionEntity> versions) {
        this.versions = versions;
    }

    @Override
    public String toString() {
        return "RuleEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", version=" + version +
                ", isActive=" + isActive +
                '}';
    }
}