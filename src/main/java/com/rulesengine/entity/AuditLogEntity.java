package com.rulesengine.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Table name is required")
    @Column(name = "table_name", nullable = false, length = 100)
    private String tableName;

    @NotNull(message = "Record ID is required")
    @Column(name = "record_id", nullable = false)
    private Long recordId;

    @NotBlank(message = "Operation is required")
    @Column(nullable = false, length = 20)
    private String operation; // INSERT, UPDATE, DELETE

    @Column(name = "old_values", columnDefinition = "TEXT")
    private String oldValuesJson;

    @Column(name = "new_values", columnDefinition = "TEXT")
    private String newValuesJson;

    @Column(name = "changed_by")
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    // Enum for operations
    public enum Operation {
        INSERT, UPDATE, DELETE
    }

    // Constructors
    public AuditLogEntity() {}

    public AuditLogEntity(String tableName, Long recordId, Operation operation, String changedBy) {
        this.tableName = tableName;
        this.recordId = recordId;
        this.operation = operation.name();
        this.changedBy = changedBy;
        this.changedAt = LocalDateTime.now();
    }

    public AuditLogEntity(String tableName, Long recordId, Operation operation, 
                         String oldValuesJson, String newValuesJson, String changedBy) {
        this.tableName = tableName;
        this.recordId = recordId;
        this.operation = operation.name();
        this.oldValuesJson = oldValuesJson;
        this.newValuesJson = newValuesJson;
        this.changedBy = changedBy;
        this.changedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (changedAt == null) {
            changedAt = LocalDateTime.now();
        }
    }

    // Business methods
    public Operation getOperationEnum() {
        return Operation.valueOf(operation);
    }

    public void setOperationEnum(Operation operation) {
        this.operation = operation.name();
    }

    public boolean hasOldValues() {
        return oldValuesJson != null && !oldValuesJson.trim().isEmpty();
    }

    public boolean hasNewValues() {
        return newValuesJson != null && !newValuesJson.trim().isEmpty();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getOldValuesJson() {
        return oldValuesJson;
    }

    public void setOldValuesJson(String oldValuesJson) {
        this.oldValuesJson = oldValuesJson;
    }

    public String getNewValuesJson() {
        return newValuesJson;
    }

    public void setNewValuesJson(String newValuesJson) {
        this.newValuesJson = newValuesJson;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    @Override
    public String toString() {
        return "AuditLogEntity{" +
                "id=" + id +
                ", tableName='" + tableName + '\'' +
                ", recordId=" + recordId +
                ", operation='" + operation + '\'' +
                ", changedBy='" + changedBy + '\'' +
                ", changedAt=" + changedAt +
                '}';
    }
}