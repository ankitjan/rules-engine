package com.rulesengine.repository;

import com.rulesengine.entity.RuleVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RuleVersionRepository extends JpaRepository<RuleVersionEntity, Long> {

    // Find all versions for a rule
    @Query("SELECT rv FROM RuleVersionEntity rv WHERE rv.rule.id = :ruleId ORDER BY rv.versionNumber DESC")
    List<RuleVersionEntity> findByRuleIdOrderByVersionDesc(@Param("ruleId") Long ruleId);

    // Find specific version of a rule
    @Query("SELECT rv FROM RuleVersionEntity rv WHERE rv.rule.id = :ruleId AND rv.versionNumber = :versionNumber")
    Optional<RuleVersionEntity> findByRuleIdAndVersionNumber(@Param("ruleId") Long ruleId, @Param("versionNumber") Integer versionNumber);

    // Find latest version of a rule
    @Query("SELECT rv FROM RuleVersionEntity rv WHERE rv.rule.id = :ruleId ORDER BY rv.versionNumber DESC LIMIT 1")
    Optional<RuleVersionEntity> findLatestVersionByRuleId(@Param("ruleId") Long ruleId);

    // Find tagged versions for a rule
    @Query("SELECT rv FROM RuleVersionEntity rv WHERE rv.rule.id = :ruleId AND rv.isTagged = true ORDER BY rv.versionNumber DESC")
    List<RuleVersionEntity> findTaggedVersionsByRuleId(@Param("ruleId") Long ruleId);

    // Find version by tag name
    @Query("SELECT rv FROM RuleVersionEntity rv WHERE rv.rule.id = :ruleId AND rv.tagName = :tagName")
    Optional<RuleVersionEntity> findByRuleIdAndTagName(@Param("ruleId") Long ruleId, @Param("tagName") String tagName);

    // Get next version number for a rule
    @Query("SELECT COALESCE(MAX(rv.versionNumber), 0) + 1 FROM RuleVersionEntity rv WHERE rv.rule.id = :ruleId")
    Integer getNextVersionNumber(@Param("ruleId") Long ruleId);

    // Count versions for a rule
    @Query("SELECT COUNT(rv) FROM RuleVersionEntity rv WHERE rv.rule.id = :ruleId")
    long countByRuleId(@Param("ruleId") Long ruleId);

    // Find versions by created by user
    @Query("SELECT rv FROM RuleVersionEntity rv WHERE rv.createdBy = :createdBy ORDER BY rv.createdAt DESC")
    List<RuleVersionEntity> findByCreatedByOrderByCreatedAtDesc(@Param("createdBy") String createdBy);

    // Find all tagged versions
    @Query("SELECT rv FROM RuleVersionEntity rv WHERE rv.isTagged = true ORDER BY rv.createdAt DESC")
    List<RuleVersionEntity> findAllTaggedVersions();

    // Check if tag name exists for a rule
    @Query("SELECT COUNT(rv) > 0 FROM RuleVersionEntity rv WHERE rv.rule.id = :ruleId AND rv.tagName = :tagName")
    boolean existsByRuleIdAndTagName(@Param("ruleId") Long ruleId, @Param("tagName") String tagName);
}