package com.rulesengine.repository;

import com.rulesengine.entity.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    // Find audit logs for a specific table and record
    @Query("SELECT al FROM AuditLogEntity al WHERE al.tableName = :tableName AND al.recordId = :recordId ORDER BY al.changedAt DESC")
    List<AuditLogEntity> findByTableNameAndRecordIdOrderByChangedAtDesc(@Param("tableName") String tableName, @Param("recordId") Long recordId);

    @Query("SELECT al FROM AuditLogEntity al WHERE al.tableName = :tableName AND al.recordId = :recordId ORDER BY al.changedAt DESC")
    Page<AuditLogEntity> findByTableNameAndRecordIdOrderByChangedAtDesc(@Param("tableName") String tableName, @Param("recordId") Long recordId, Pageable pageable);

    // Find audit logs for a specific table
    @Query("SELECT al FROM AuditLogEntity al WHERE al.tableName = :tableName ORDER BY al.changedAt DESC")
    List<AuditLogEntity> findByTableNameOrderByChangedAtDesc(@Param("tableName") String tableName);

    @Query("SELECT al FROM AuditLogEntity al WHERE al.tableName = :tableName ORDER BY al.changedAt DESC")
    Page<AuditLogEntity> findByTableNameOrderByChangedAtDesc(@Param("tableName") String tableName, Pageable pageable);

    // Find audit logs by operation type
    @Query("SELECT al FROM AuditLogEntity al WHERE al.operation = :operation ORDER BY al.changedAt DESC")
    List<AuditLogEntity> findByOperationOrderByChangedAtDesc(@Param("operation") String operation);

    // Find audit logs by user
    @Query("SELECT al FROM AuditLogEntity al WHERE al.changedBy = :changedBy ORDER BY al.changedAt DESC")
    List<AuditLogEntity> findByChangedByOrderByChangedAtDesc(@Param("changedBy") String changedBy);

    @Query("SELECT al FROM AuditLogEntity al WHERE al.changedBy = :changedBy ORDER BY al.changedAt DESC")
    Page<AuditLogEntity> findByChangedByOrderByChangedAtDesc(@Param("changedBy") String changedBy, Pageable pageable);

    // Find audit logs within date range
    @Query("SELECT al FROM AuditLogEntity al WHERE al.changedAt BETWEEN :startDate AND :endDate ORDER BY al.changedAt DESC")
    List<AuditLogEntity> findByChangedAtBetweenOrderByChangedAtDesc(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT al FROM AuditLogEntity al WHERE al.changedAt BETWEEN :startDate AND :endDate ORDER BY al.changedAt DESC")
    Page<AuditLogEntity> findByChangedAtBetweenOrderByChangedAtDesc(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);

    // Find recent audit logs
    @Query("SELECT al FROM AuditLogEntity al WHERE al.changedAt >= :since ORDER BY al.changedAt DESC")
    List<AuditLogEntity> findRecentChanges(@Param("since") LocalDateTime since);

    // Count audit logs by table
    @Query("SELECT COUNT(al) FROM AuditLogEntity al WHERE al.tableName = :tableName")
    long countByTableName(@Param("tableName") String tableName);

    // Count audit logs by user
    @Query("SELECT COUNT(al) FROM AuditLogEntity al WHERE al.changedBy = :changedBy")
    long countByChangedBy(@Param("changedBy") String changedBy);

    // Find audit logs for multiple tables
    @Query("SELECT al FROM AuditLogEntity al WHERE al.tableName IN :tableNames ORDER BY al.changedAt DESC")
    List<AuditLogEntity> findByTableNameInOrderByChangedAtDesc(@Param("tableNames") List<String> tableNames);

    // Delete old audit logs (for cleanup)
    @Query("DELETE FROM AuditLogEntity al WHERE al.changedAt < :cutoffDate")
    void deleteOldAuditLogs(@Param("cutoffDate") LocalDateTime cutoffDate);
}