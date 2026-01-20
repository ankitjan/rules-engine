package com.rulesengine.repository;

import com.rulesengine.entity.FieldConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FieldConfigRepository extends JpaRepository<FieldConfigEntity, Long> {

    // Find active field configs (not soft deleted)
    @Query("SELECT fc FROM FieldConfigEntity fc WHERE fc.isDeleted = false")
    List<FieldConfigEntity> findAllActive();

    // Find by ID (active only)
    @Query("SELECT fc FROM FieldConfigEntity fc WHERE fc.id = :id AND fc.isDeleted = false")
    Optional<FieldConfigEntity> findByIdActive(@Param("id") Long id);

    // Find by field name (active only)
    @Query("SELECT fc FROM FieldConfigEntity fc WHERE fc.fieldName = :fieldName AND fc.isDeleted = false")
    Optional<FieldConfigEntity> findByFieldNameActive(@Param("fieldName") String fieldName);

    // Find by field type (active only)
    @Query("SELECT fc FROM FieldConfigEntity fc WHERE fc.fieldType = :fieldType AND fc.isDeleted = false")
    List<FieldConfigEntity> findByFieldTypeActive(@Param("fieldType") String fieldType);

    // Find calculated fields (active only)
    @Query("SELECT fc FROM FieldConfigEntity fc WHERE fc.isCalculated = true AND fc.isDeleted = false")
    List<FieldConfigEntity> findCalculatedFieldsActive();

    // Find non-calculated fields (active only)
    @Query("SELECT fc FROM FieldConfigEntity fc WHERE fc.isCalculated = false AND fc.isDeleted = false")
    List<FieldConfigEntity> findNonCalculatedFieldsActive();

    // Find required fields (active only)
    @Query("SELECT fc FROM FieldConfigEntity fc WHERE fc.isRequired = true AND fc.isDeleted = false")
    List<FieldConfigEntity> findRequiredFieldsActive();

    // Find fields with data service (active only)
    @Query("SELECT fc FROM FieldConfigEntity fc WHERE fc.dataServiceConfigJson IS NOT NULL AND fc.isDeleted = false")
    List<FieldConfigEntity> findFieldsWithDataServiceActive();

    // Find fields with dependencies (active only)
    @Query("SELECT fc FROM FieldConfigEntity fc WHERE fc.dependenciesJson IS NOT NULL AND fc.isDeleted = false")
    List<FieldConfigEntity> findFieldsWithDependenciesActive();

    // Find fields by name pattern (active only)
    @Query("SELECT fc FROM FieldConfigEntity fc WHERE LOWER(fc.fieldName) LIKE LOWER(CONCAT('%', :pattern, '%')) AND fc.isDeleted = false")
    List<FieldConfigEntity> findByFieldNameContainingIgnoreCaseActive(@Param("pattern") String pattern);

    // Check if field name exists (active only)
    @Query("SELECT COUNT(fc) > 0 FROM FieldConfigEntity fc WHERE fc.fieldName = :fieldName AND fc.isDeleted = false")
    boolean existsByFieldNameActive(@Param("fieldName") String fieldName);

    // Check if field name exists excluding specific ID (for updates, active only)
    @Query("SELECT COUNT(fc) > 0 FROM FieldConfigEntity fc WHERE fc.fieldName = :fieldName AND fc.id != :excludeId AND fc.isDeleted = false")
    boolean existsByFieldNameAndIdNotActive(@Param("fieldName") String fieldName, @Param("excludeId") Long excludeId);

    // Find fields by multiple field names (active only)
    @Query("SELECT fc FROM FieldConfigEntity fc WHERE fc.fieldName IN :fieldNames AND fc.isDeleted = false")
    List<FieldConfigEntity> findByFieldNamesActive(@Param("fieldNames") List<String> fieldNames);

    // Count active field configs
    @Query("SELECT COUNT(fc) FROM FieldConfigEntity fc WHERE fc.isDeleted = false")
    long countActive();

    // Count by field type (active only)
    @Query("SELECT COUNT(fc) FROM FieldConfigEntity fc WHERE fc.fieldType = :fieldType AND fc.isDeleted = false")
    long countByFieldTypeActive(@Param("fieldType") String fieldType);

    // Find by created by user (active only)
    @Query("SELECT fc FROM FieldConfigEntity fc WHERE fc.createdBy = :createdBy AND fc.isDeleted = false")
    List<FieldConfigEntity> findByCreatedByActive(@Param("createdBy") String createdBy);

    // Find latest version of each field (active only)
    @Query("SELECT fc FROM FieldConfigEntity fc WHERE fc.isDeleted = false AND " +
           "fc.version = (SELECT MAX(fc2.version) FROM FieldConfigEntity fc2 WHERE fc2.fieldName = fc.fieldName AND fc2.isDeleted = false)")
    List<FieldConfigEntity> findLatestVersionsActive();
}