package com.rulesengine.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "rule_versions")
public class RuleVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private RuleEntity rule;

    @NotNull(message = "Version number is required")
    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @NotBlank(message = "Rule definition is required")
    @Column(name = "rule_definition", columnDefinition = "TEXT", nullable = false)
    private String ruleDefinitionJson;

    @Column(name = "change_description", length = 1000)
    private String changeDescription;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @Column(name = "is_tagged", nullable = false)
    private Boolean isTagged = false;

    @Column(name = "tag_name")
    private String tagName;

    // Constructors
    public RuleVersionEntity() {}

    public RuleVersionEntity(RuleEntity rule, Integer versionNumber, String ruleDefinitionJson, String changeDescription) {
        this.rule = rule;
        this.versionNumber = versionNumber;
        this.ruleDefinitionJson = ruleDefinitionJson;
        this.changeDescription = changeDescription;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isTagged == null) {
            isTagged = false;
        }
    }

    // Business methods
    public void tag(String tagName) {
        this.isTagged = true;
        this.tagName = tagName;
    }

    public void untag() {
        this.isTagged = false;
        this.tagName = null;
    }

    public boolean isTagged() {
        return Boolean.TRUE.equals(isTagged);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RuleEntity getRule() {
        return rule;
    }

    public void setRule(RuleEntity rule) {
        this.rule = rule;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getRuleDefinitionJson() {
        return ruleDefinitionJson;
    }

    public void setRuleDefinitionJson(String ruleDefinitionJson) {
        this.ruleDefinitionJson = ruleDefinitionJson;
    }

    public String getChangeDescription() {
        return changeDescription;
    }

    public void setChangeDescription(String changeDescription) {
        this.changeDescription = changeDescription;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Boolean getIsTagged() {
        return isTagged;
    }

    public void setIsTagged(Boolean isTagged) {
        this.isTagged = isTagged;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    @Override
    public String toString() {
        return "RuleVersionEntity{" +
                "id=" + id +
                ", versionNumber=" + versionNumber +
                ", isTagged=" + isTagged +
                ", tagName='" + tagName + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}