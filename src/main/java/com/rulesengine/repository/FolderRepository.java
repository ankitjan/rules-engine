package com.rulesengine.repository;

import com.rulesengine.entity.FolderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolderRepository extends JpaRepository<FolderEntity, Long> {

    // Find active folders (not soft deleted)
    @Query("SELECT f FROM FolderEntity f WHERE f.isDeleted = false")
    List<FolderEntity> findAllActive();

    // Find by ID (active only)
    @Query("SELECT f FROM FolderEntity f WHERE f.id = :id AND f.isDeleted = false")
    Optional<FolderEntity> findByIdActive(@Param("id") Long id);

    // Find by name (active only)
    @Query("SELECT f FROM FolderEntity f WHERE f.name = :name AND f.isDeleted = false")
    Optional<FolderEntity> findByNameActive(@Param("name") String name);

    // Find root folders (no parent, active only)
    @Query("SELECT f FROM FolderEntity f WHERE f.parent IS NULL AND f.isDeleted = false")
    List<FolderEntity> findRootFoldersActive();

    // Find children of a folder (active only)
    @Query("SELECT f FROM FolderEntity f WHERE f.parent.id = :parentId AND f.isDeleted = false")
    List<FolderEntity> findByParentIdActive(@Param("parentId") Long parentId);

    // Find by path prefix (for hierarchy queries, active only)
    @Query("SELECT f FROM FolderEntity f WHERE f.path LIKE CONCAT(:pathPrefix, '%') AND f.isDeleted = false")
    List<FolderEntity> findByPathStartingWithActive(@Param("pathPrefix") String pathPrefix);

    // Find folders at specific depth (active only)
    @Query("SELECT f FROM FolderEntity f WHERE LENGTH(f.path) - LENGTH(REPLACE(f.path, '/', '')) = :depth AND f.isDeleted = false")
    List<FolderEntity> findByDepthActive(@Param("depth") int depth);

    // Check if folder name exists under parent (active only)
    @Query("SELECT COUNT(f) > 0 FROM FolderEntity f WHERE f.name = :name AND f.parent.id = :parentId AND f.isDeleted = false")
    boolean existsByNameAndParentIdActive(@Param("name") String name, @Param("parentId") Long parentId);

    // Check if folder name exists at root level (active only)
    @Query("SELECT COUNT(f) > 0 FROM FolderEntity f WHERE f.name = :name AND f.parent IS NULL AND f.isDeleted = false")
    boolean existsByNameAtRootActive(@Param("name") String name);

    // Find folders with rules (active only)
    @Query("SELECT DISTINCT f FROM FolderEntity f JOIN f.rules r WHERE r.isDeleted = false AND f.isDeleted = false")
    List<FolderEntity> findFoldersWithRulesActive();

    // Find empty folders (no rules, no children, active only)
    @Query("SELECT f FROM FolderEntity f WHERE f.isDeleted = false AND " +
           "NOT EXISTS (SELECT r FROM RuleEntity r WHERE r.folder = f AND r.isDeleted = false) AND " +
           "NOT EXISTS (SELECT c FROM FolderEntity c WHERE c.parent = f AND c.isDeleted = false)")
    List<FolderEntity> findEmptyFoldersActive();

    // Count active folders
    @Query("SELECT COUNT(f) FROM FolderEntity f WHERE f.isDeleted = false")
    long countActive();

    // Count children of a folder (active only)
    @Query("SELECT COUNT(f) FROM FolderEntity f WHERE f.parent.id = :parentId AND f.isDeleted = false")
    long countChildrenActive(@Param("parentId") Long parentId);

    // Find by created by user (active only)
    @Query("SELECT f FROM FolderEntity f WHERE f.createdBy = :createdBy AND f.isDeleted = false")
    List<FolderEntity> findByCreatedByActive(@Param("createdBy") String createdBy);
}