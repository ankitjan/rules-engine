package com.rulesengine.entity;

import com.rulesengine.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class EntityIntegrationTest {

    @Autowired
    private RuleRepository ruleRepository;

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private FieldConfigRepository fieldConfigRepository;

    @Autowired
    private EntityTypeRepository entityTypeRepository;

    @Autowired
    private RuleVersionRepository ruleVersionRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    public void testFolderEntityCreationAndRetrieval() {
        // Create a folder
        FolderEntity folder = new FolderEntity("Test Folder", "A test folder for rules");
        FolderEntity savedFolder = folderRepository.save(folder);

        // Verify it was saved
        assertThat(savedFolder.getId()).isNotNull();
        assertThat(savedFolder.getName()).isEqualTo("Test Folder");
        assertThat(savedFolder.getDescription()).isEqualTo("A test folder for rules");
        assertThat(savedFolder.getIsDeleted()).isFalse();
        assertThat(savedFolder.getCreatedAt()).isNotNull();

        // Test retrieval
        FolderEntity retrievedFolder = folderRepository.findByIdActive(savedFolder.getId()).orElse(null);
        assertThat(retrievedFolder).isNotNull();
        assertThat(retrievedFolder.getName()).isEqualTo("Test Folder");
    }

    @Test
    public void testRuleEntityCreationAndRetrieval() {
        // Create a folder first
        FolderEntity folder = new FolderEntity("Rules Folder", "Folder for test rules");
        FolderEntity savedFolder = folderRepository.save(folder);

        // Create a rule
        RuleEntity rule = new RuleEntity("Test Rule", "A test rule", "{\"conditions\": []}");
        rule.setFolder(savedFolder);
        RuleEntity savedRule = ruleRepository.save(rule);

        // Verify it was saved
        assertThat(savedRule.getId()).isNotNull();
        assertThat(savedRule.getName()).isEqualTo("Test Rule");
        assertThat(savedRule.getDescription()).isEqualTo("A test rule");
        assertThat(savedRule.getRuleDefinitionJson()).isEqualTo("{\"conditions\": []}");
        assertThat(savedRule.getVersion()).isEqualTo(1);
        assertThat(savedRule.getIsActive()).isTrue();
        assertThat(savedRule.getIsDeleted()).isFalse();
        assertThat(savedRule.getCreatedAt()).isNotNull();

        // Test retrieval
        RuleEntity retrievedRule = ruleRepository.findByIdActive(savedRule.getId()).orElse(null);
        assertThat(retrievedRule).isNotNull();
        assertThat(retrievedRule.getName()).isEqualTo("Test Rule");
        assertThat(retrievedRule.getFolder().getId()).isEqualTo(savedFolder.getId());
    }

    @Test
    public void testFieldConfigEntityCreationAndRetrieval() {
        // Create a field config
        FieldConfigEntity fieldConfig = new FieldConfigEntity("user.email", "STRING", "User email address");
        fieldConfig.setMapperExpression("user.email");
        fieldConfig.setIsRequired(true);
        FieldConfigEntity savedFieldConfig = fieldConfigRepository.save(fieldConfig);

        // Verify it was saved
        assertThat(savedFieldConfig.getId()).isNotNull();
        assertThat(savedFieldConfig.getFieldName()).isEqualTo("user.email");
        assertThat(savedFieldConfig.getFieldType()).isEqualTo("STRING");
        assertThat(savedFieldConfig.getDescription()).isEqualTo("User email address");
        assertThat(savedFieldConfig.getMapperExpression()).isEqualTo("user.email");
        assertThat(savedFieldConfig.getIsRequired()).isTrue();
        assertThat(savedFieldConfig.getIsCalculated()).isFalse();
        assertThat(savedFieldConfig.getVersion()).isEqualTo(1);
        assertThat(savedFieldConfig.getIsDeleted()).isFalse();

        // Test retrieval
        FieldConfigEntity retrievedFieldConfig = fieldConfigRepository.findByFieldNameActive("user.email").orElse(null);
        assertThat(retrievedFieldConfig).isNotNull();
        assertThat(retrievedFieldConfig.getFieldName()).isEqualTo("user.email");
    }

    @Test
    public void testEntityTypeCreationAndRetrieval() {
        // Create an entity type
        EntityTypeEntity entityType = new EntityTypeEntity("User", "User entity type", "{\"endpoint\": \"/api/users\"}");
        entityType.setFieldMappingsJson("{\"id\": \"user.id\", \"email\": \"user.email\"}");
        EntityTypeEntity savedEntityType = entityTypeRepository.save(entityType);

        // Verify it was saved
        assertThat(savedEntityType.getId()).isNotNull();
        assertThat(savedEntityType.getTypeName()).isEqualTo("User");
        assertThat(savedEntityType.getDescription()).isEqualTo("User entity type");
        assertThat(savedEntityType.getDataServiceConfigJson()).isEqualTo("{\"endpoint\": \"/api/users\"}");
        assertThat(savedEntityType.getFieldMappingsJson()).isEqualTo("{\"id\": \"user.id\", \"email\": \"user.email\"}");
        assertThat(savedEntityType.getIsDeleted()).isFalse();

        // Test retrieval
        EntityTypeEntity retrievedEntityType = entityTypeRepository.findByTypeNameActive("User").orElse(null);
        assertThat(retrievedEntityType).isNotNull();
        assertThat(retrievedEntityType.getTypeName()).isEqualTo("User");
    }

    @Test
    public void testRuleVersionCreationAndRetrieval() {
        // Create a rule first
        RuleEntity rule = new RuleEntity("Versioned Rule", "A rule with versions", "{\"conditions\": []}");
        RuleEntity savedRule = ruleRepository.save(rule);

        // Create a rule version
        RuleVersionEntity ruleVersion = new RuleVersionEntity(savedRule, 1, "{\"conditions\": []}", "Initial version");
        RuleVersionEntity savedRuleVersion = ruleVersionRepository.save(ruleVersion);

        // Verify it was saved
        assertThat(savedRuleVersion.getId()).isNotNull();
        assertThat(savedRuleVersion.getVersionNumber()).isEqualTo(1);
        assertThat(savedRuleVersion.getRuleDefinitionJson()).isEqualTo("{\"conditions\": []}");
        assertThat(savedRuleVersion.getChangeDescription()).isEqualTo("Initial version");
        assertThat(savedRuleVersion.getIsTagged()).isFalse();
        assertThat(savedRuleVersion.getCreatedAt()).isNotNull();

        // Test retrieval
        RuleVersionEntity retrievedVersion = ruleVersionRepository.findByRuleIdAndVersionNumber(savedRule.getId(), 1).orElse(null);
        assertThat(retrievedVersion).isNotNull();
        assertThat(retrievedVersion.getVersionNumber()).isEqualTo(1);
        assertThat(retrievedVersion.getRule().getId()).isEqualTo(savedRule.getId());
    }

    @Test
    public void testAuditLogCreationAndRetrieval() {
        // Create an audit log entry
        AuditLogEntity auditLog = new AuditLogEntity("rules", 1L, AuditLogEntity.Operation.INSERT, "system");
        auditLog.setNewValuesJson("{\"name\": \"Test Rule\"}");
        AuditLogEntity savedAuditLog = auditLogRepository.save(auditLog);

        // Verify it was saved
        assertThat(savedAuditLog.getId()).isNotNull();
        assertThat(savedAuditLog.getTableName()).isEqualTo("rules");
        assertThat(savedAuditLog.getRecordId()).isEqualTo(1L);
        assertThat(savedAuditLog.getOperation()).isEqualTo("INSERT");
        assertThat(savedAuditLog.getChangedBy()).isEqualTo("system");
        assertThat(savedAuditLog.getNewValuesJson()).isEqualTo("{\"name\": \"Test Rule\"}");
        assertThat(savedAuditLog.getChangedAt()).isNotNull();

        // Test retrieval
        var auditLogs = auditLogRepository.findByTableNameAndRecordIdOrderByChangedAtDesc("rules", 1L);
        assertThat(auditLogs).hasSize(1);
        assertThat(auditLogs.get(0).getOperation()).isEqualTo("INSERT");
    }

    @Test
    public void testSoftDeleteFunctionality() {
        // Create a rule
        RuleEntity rule = new RuleEntity("Rule to Delete", "This rule will be soft deleted", "{\"conditions\": []}");
        RuleEntity savedRule = ruleRepository.save(rule);

        // Verify it exists in active queries
        assertThat(ruleRepository.findByIdActive(savedRule.getId())).isPresent();

        // Soft delete the rule
        savedRule.softDelete("test-user");
        ruleRepository.save(savedRule);

        // Verify it no longer appears in active queries
        assertThat(ruleRepository.findByIdActive(savedRule.getId())).isEmpty();

        // But still exists in the database
        assertThat(ruleRepository.findById(savedRule.getId())).isPresent();
        RuleEntity deletedRule = ruleRepository.findById(savedRule.getId()).get();
        assertThat(deletedRule.getIsDeleted()).isTrue();
        assertThat(deletedRule.getDeletedBy()).isEqualTo("test-user");
        assertThat(deletedRule.getDeletedAt()).isNotNull();
    }

    @Test
    public void testFolderHierarchy() {
        // Create parent folder
        FolderEntity parentFolder = new FolderEntity("Parent Folder", "Parent folder");
        FolderEntity savedParent = folderRepository.save(parentFolder);

        // Create child folder
        FolderEntity childFolder = new FolderEntity("Child Folder", "Child folder", savedParent);
        FolderEntity savedChild = folderRepository.save(childFolder);

        // Verify hierarchy
        assertThat(savedChild.getParent().getId()).isEqualTo(savedParent.getId());
        assertThat(savedChild.isRoot()).isFalse();
        assertThat(savedParent.isRoot()).isTrue();

        // Test path functionality (note: path is updated after ID is set)
        savedChild.setId(savedChild.getId()); // Trigger path update
        assertThat(savedChild.getPath()).contains(savedChild.getId().toString());
    }
}