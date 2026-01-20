package com.rulesengine.repository;

import com.rulesengine.entity.RuleEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RuleRepository extends JpaRepository<RuleEntity, Long> {

    // Find active rules (not soft deleted)
    @Query("SELECT r FROM RuleEntity r WHERE r.isDeleted = false")
    List<RuleEntity> findAllActive();

    @Query("SELECT r FROM RuleEntity r WHERE r.isDeleted = false")
    Page<RuleEntity> findAllActive(Pageable pageable);

    // Find by ID (active only)
    @Query("SELECT r FROM RuleEntity r WHERE r.id = :id AND r.isDeleted = false")
    Optional<RuleEntity> findByIdActive(@Param("id") Long id);

    // Find by name (active only)
    @Query("SELECT r FROM RuleEntity r WHERE r.name = :name AND r.isDeleted = false")
    Optional<RuleEntity> findByNameActive(@Param("name") String name);

    // Find by folder (active only)
    @Query("SELECT r FROM RuleEntity r WHERE r.folder.id = :folderId AND r.isDeleted = false")
    List<RuleEntity> findByFolderIdActive(@Param("folderId") Long folderId);

    @Query("SELECT r FROM RuleEntity r WHERE r.folder.id = :folderId AND r.isDeleted = false")
    Page<RuleEntity> findByFolderIdActive(@Param("folderId") Long folderId, Pageable pageable);

    // Find by active status
    @Query("SELECT r FROM RuleEntity r WHERE r.isActive = :isActive AND r.isDeleted = false")
    List<RuleEntity> findByIsActiveAndNotDeleted(@Param("isActive") Boolean isActive);

    // Search by name containing (active only)
    @Query("SELECT r FROM RuleEntity r WHERE LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%')) AND r.isDeleted = false")
    List<RuleEntity> findByNameContainingIgnoreCaseActive(@Param("name") String name);

    @Query("SELECT r FROM RuleEntity r WHERE LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%')) AND r.isDeleted = false")
    Page<RuleEntity> findByNameContainingIgnoreCaseActive(@Param("name") String name, Pageable pageable);

    // Find rules without folder (active only)
    @Query("SELECT r FROM RuleEntity r WHERE r.folder IS NULL AND r.isDeleted = false")
    List<RuleEntity> findRulesWithoutFolderActive();

    // Count active rules
    @Query("SELECT COUNT(r) FROM RuleEntity r WHERE r.isDeleted = false")
    long countActive();

    // Count active rules by folder
    @Query("SELECT COUNT(r) FROM RuleEntity r WHERE r.folder.id = :folderId AND r.isDeleted = false")
    long countByFolderIdActive(@Param("folderId") Long folderId);

    // Find by created by user (active only)
    @Query("SELECT r FROM RuleEntity r WHERE r.createdBy = :createdBy AND r.isDeleted = false")
    List<RuleEntity> findByCreatedByActive(@Param("createdBy") String createdBy);
}