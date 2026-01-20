package com.rulesengine.repository;

import com.rulesengine.entity.EntityTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntityTypeRepository extends JpaRepository<EntityTypeEntity, Long> {

    // Find active entity types (not soft deleted)
    @Query("SELECT et FROM EntityTypeEntity et WHERE et.isDeleted = false")
    List<EntityTypeEntity> findAllActive();

    // Find by ID (active only)
    @Query("SELECT et FROM EntityTypeEntity et WHERE et.id = :id AND et.isDeleted = false")
    Optional<EntityTypeEntity> findByIdActive(@Param("id") Long id);

    // Find by type name (active only)
    @Query("SELECT et FROM EntityTypeEntity et WHERE et.typeName = :typeName AND et.isDeleted = false")
    Optional<EntityTypeEntity> findByTypeNameActive(@Param("typeName") String typeName);

    // Find by type name pattern (active only)
    @Query("SELECT et FROM EntityTypeEntity et WHERE LOWER(et.typeName) LIKE LOWER(CONCAT('%', :pattern, '%')) AND et.isDeleted = false")
    List<EntityTypeEntity> findByTypeNameContainingIgnoreCaseActive(@Param("pattern") String pattern);

    // Find entity types with field mappings (active only)
    @Query("SELECT et FROM EntityTypeEntity et WHERE et.fieldMappingsJson IS NOT NULL AND et.isDeleted = false")
    List<EntityTypeEntity> findWithFieldMappingsActive();

    // Check if type name exists (active only)
    @Query("SELECT COUNT(et) > 0 FROM EntityTypeEntity et WHERE et.typeName = :typeName AND et.isDeleted = false")
    boolean existsByTypeNameActive(@Param("typeName") String typeName);

    // Check if type name exists excluding specific ID (for updates, active only)
    @Query("SELECT COUNT(et) > 0 FROM EntityTypeEntity et WHERE et.typeName = :typeName AND et.id != :excludeId AND et.isDeleted = false")
    boolean existsByTypeNameAndIdNotActive(@Param("typeName") String typeName, @Param("excludeId") Long excludeId);

    // Count active entity types
    @Query("SELECT COUNT(et) FROM EntityTypeEntity et WHERE et.isDeleted = false")
    long countActive();

    // Find by created by user (active only)
    @Query("SELECT et FROM EntityTypeEntity et WHERE et.createdBy = :createdBy AND et.isDeleted = false")
    List<EntityTypeEntity> findByCreatedByActive(@Param("createdBy") String createdBy);
}