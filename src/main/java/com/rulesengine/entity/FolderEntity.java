package com.rulesengine.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "folders")
public class FolderEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Folder name is required")
    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private FolderEntity parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FolderEntity> children = new ArrayList<>();

    @OneToMany(mappedBy = "folder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RuleEntity> rules = new ArrayList<>();

    @Column(length = 2000)
    private String path; // Materialized path for efficient hierarchy queries

    // Constructors
    public FolderEntity() {}

    public FolderEntity(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public FolderEntity(String name, String description, FolderEntity parent) {
        this.name = name;
        this.description = description;
        this.parent = parent;
        // Path will be updated after ID is assigned during save
    }

    // Business methods
    public void updatePath() {
        if (id == null) {
            this.path = null; // Path will be set after ID is assigned
        } else if (parent == null) {
            this.path = "/" + id;
        } else {
            this.path = parent.getPath() + "/" + id;
        }
    }

    public void addChild(FolderEntity child) {
        children.add(child);
        child.setParent(this);
        child.updatePath();
    }

    public void removeChild(FolderEntity child) {
        children.remove(child);
        child.setParent(null);
        child.updatePath();
    }

    public void addRule(RuleEntity rule) {
        rules.add(rule);
        rule.setFolder(this);
    }

    public void removeRule(RuleEntity rule) {
        rules.remove(rule);
        rule.setFolder(null);
    }

    public boolean isRoot() {
        return parent == null;
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public boolean hasRules() {
        return !rules.isEmpty();
    }

    public int getDepth() {
        if (path == null) return 0;
        return path.split("/").length - 1;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
        updatePath(); // Update path when ID changes
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

    public FolderEntity getParent() {
        return parent;
    }

    public void setParent(FolderEntity parent) {
        this.parent = parent;
        updatePath();
    }

    public List<FolderEntity> getChildren() {
        return children;
    }

    public void setChildren(List<FolderEntity> children) {
        this.children = children;
    }

    public List<RuleEntity> getRules() {
        return rules;
    }

    public void setRules(List<RuleEntity> rules) {
        this.rules = rules;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return "FolderEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", childrenCount=" + (children != null ? children.size() : 0) +
                ", rulesCount=" + (rules != null ? rules.size() : 0) +
                '}';
    }
}