package com.rulesengine.service;

import com.rulesengine.dto.EntityFilterRequest;
import com.rulesengine.dto.EntityFilterResult;
import com.rulesengine.dto.ExecutionContext;
import com.rulesengine.dto.RuleExecutionResult;
import com.rulesengine.entity.EntityTypeEntity;
import com.rulesengine.exception.RuleNotFoundException;
import com.rulesengine.model.RuleDefinition;
import com.rulesengine.model.RuleItem;
import com.rulesengine.repository.EntityTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EntityFilterServiceTest {

    @Mock
    private EntityTypeRepository entityTypeRepository;

    @Mock
    private EntityTypeService entityTypeService;

    @Mock
    private RuleExecutionService ruleExecutionService;

    @Mock
    private FieldResolutionService fieldResolutionService;

    @InjectMocks
    private EntityFilterService entityFilterService;

    private EntityTypeEntity testEntityType;
    private EntityFilterRequest testRequest;
    private RuleDefinition testRule;

    @BeforeEach
    void setUp() {
        // Reset all mocks
        reset(entityTypeRepository, entityTypeService, ruleExecutionService, fieldResolutionService);
        
        // Create test entity type
        testEntityType = new EntityTypeEntity();
        testEntityType.setId(1L);
        testEntityType.setTypeName("User");
        testEntityType.setDescription("User entity type");
        testEntityType.setDataServiceConfigJson("{\"serviceType\":\"REST\",\"endpoint\":\"http://api.example.com/users\"}");

        // Create test rule
        RuleItem condition = new RuleItem("status", "equals", "active");
        testRule = new RuleDefinition("and", Arrays.asList(condition));

        // Create test request
        testRequest = new EntityFilterRequest();
        testRequest.setEntityType("User");
        testRequest.setEntityIds(Arrays.asList("user1", "user2", "user3"));
        testRequest.setRule(testRule);
        testRequest.setPage(0);
        testRequest.setSize(20);
        testRequest.setIncludeEntityData(true);
        testRequest.setIncludeTrace(false);
    }

    @Test
    void testFilterEntities_Success() {
        // Given
        when(entityTypeRepository.findByTypeNameActive("User"))
            .thenReturn(Optional.of(testEntityType));

        // Mock entity data retrieval
        Map<String, Object> user1Data = Map.of("id", "user1", "status", "active", "name", "John");
        Map<String, Object> user2Data = Map.of("id", "user2", "status", "active", "name", "Jane");
        Map<String, Object> user3Data = Map.of("id", "user3", "status", "inactive", "name", "Bob");

        when(entityTypeService.retrieveEntityData("User", "user1")).thenReturn(user1Data);
        when(entityTypeService.retrieveEntityData("User", "user2")).thenReturn(user2Data);
        when(entityTypeService.retrieveEntityData("User", "user3")).thenReturn(user3Data);

        // Mock rule execution results
        RuleExecutionResult result1 = new RuleExecutionResult(1L, "Test Rule", true);
        result1.setExecutionTime(LocalDateTime.now());
        result1.setExecutionDurationMs(10L);

        RuleExecutionResult result2 = new RuleExecutionResult(1L, "Test Rule", true);
        result2.setExecutionTime(LocalDateTime.now());
        result2.setExecutionDurationMs(12L);

        RuleExecutionResult result3 = new RuleExecutionResult(1L, "Test Rule", false);
        result3.setExecutionTime(LocalDateTime.now());
        result3.setExecutionDurationMs(8L);

        when(ruleExecutionService.executeRuleWithDefinition(eq(testRule), any(ExecutionContext.class)))
            .thenReturn(result1, result2, result3);

        // When
        EntityFilterResult result = entityFilterService.filterEntities(testRequest);

        // Then
        assertNotNull(result);
        assertEquals(3L, result.getTotalProcessed());
        assertEquals(2L, result.getTotalMatched());
        assertEquals(0L, result.getTotalFailed());
        assertNotNull(result.getEntities());
        assertEquals(3, result.getEntities().size());

        // Verify first entity (matched)
        EntityFilterResult.FilteredEntity entity1 = result.getEntities().get(0);
        assertEquals("user1", entity1.getEntityId());
        assertEquals(Boolean.TRUE, entity1.getMatched());
        assertNotNull(entity1.getEntityData());
        assertEquals("John", entity1.getEntityData().get("name"));

        // Verify third entity (not matched)
        EntityFilterResult.FilteredEntity entity3 = result.getEntities().get(2);
        assertEquals("user3", entity3.getEntityId());
        assertEquals(Boolean.FALSE, entity3.getMatched());

        // Verify pagination
        assertNotNull(result.getPagination());
        assertEquals(0, result.getPagination().getPage());
        assertEquals(20, result.getPagination().getSize());
        assertEquals(3L, result.getPagination().getTotalElements());

        // Verify metrics
        assertNotNull(result.getMetrics());
        assertEquals(1, result.getMetrics().getBatchesProcessed());
        assertTrue(result.getMetrics().getDataRetrievalTimeMs() >= 0);
        assertTrue(result.getMetrics().getRuleEvaluationTimeMs() >= 0);

        // Verify service calls
        verify(entityTypeRepository).findByTypeNameActive("User");
        verify(entityTypeService, times(3)).retrieveEntityData(eq("User"), anyString());
        verify(ruleExecutionService, times(3)).executeRuleWithDefinition(eq(testRule), any(ExecutionContext.class));
    }

    @Test
    void testFilterEntities_EntityTypeNotFound() {
        // Given
        when(entityTypeRepository.findByTypeNameActive("NonExistentType"))
            .thenReturn(Optional.empty());

        testRequest.setEntityType("NonExistentType");

        // When & Then
        assertThrows(RuleNotFoundException.class, () -> {
            entityFilterService.filterEntities(testRequest);
        });

        verify(entityTypeRepository).findByTypeNameActive("NonExistentType");
        verifyNoInteractions(entityTypeService, ruleExecutionService);
    }

    @Test
    void testFilterEntities_WithEntityDataRetrievalFailure() {
        // Given
        when(entityTypeRepository.findByTypeNameActive("User"))
            .thenReturn(Optional.of(testEntityType));

        // Mock successful retrieval for first entity
        Map<String, Object> user1Data = Map.of("id", "user1", "status", "active");
        when(entityTypeService.retrieveEntityData("User", "user1")).thenReturn(user1Data);

        // Mock failure for second entity
        when(entityTypeService.retrieveEntityData("User", "user2"))
            .thenThrow(new RuntimeException("Data service error"));

        // Mock successful retrieval for third entity
        Map<String, Object> user3Data = Map.of("id", "user3", "status", "inactive");
        when(entityTypeService.retrieveEntityData("User", "user3")).thenReturn(user3Data);

        // Mock rule execution for successful entities
        RuleExecutionResult result1 = new RuleExecutionResult(1L, "Test Rule", true);
        RuleExecutionResult result3 = new RuleExecutionResult(1L, "Test Rule", false);

        when(ruleExecutionService.executeRuleWithDefinition(eq(testRule), any(ExecutionContext.class)))
            .thenReturn(result1, result3);

        // When
        EntityFilterResult result = entityFilterService.filterEntities(testRequest);

        // Then
        assertNotNull(result);
        assertEquals(3L, result.getTotalProcessed());
        assertEquals(1L, result.getTotalMatched());
        assertEquals(1L, result.getTotalFailed());
        assertNotNull(result.getErrors());
        assertEquals(1, result.getErrors().size());

        // Verify error details
        EntityFilterResult.EntityProcessingError error = result.getErrors().get(0);
        assertEquals("user2", error.getEntityId());
        assertEquals("PROCESSING_ERROR", error.getCode());
        assertTrue(error.getMessage().contains("Data service error"));

        // Verify failed entity
        EntityFilterResult.FilteredEntity failedEntity = result.getEntities().stream()
            .filter(e -> "user2".equals(e.getEntityId()))
            .findFirst()
            .orElse(null);
        assertNotNull(failedEntity);
        assertEquals(Boolean.FALSE, failedEntity.getMatched());
        assertNotNull(failedEntity.getError());
    }

    @Test
    void testFilterEntitiesBatch_Success() {
        // Given
        testRequest.setBatchSize(2);
        when(entityTypeRepository.findByTypeNameActive("User"))
            .thenReturn(Optional.of(testEntityType));

        // Mock entity data retrieval
        Map<String, Object> userData = Map.of("status", "active");
        when(entityTypeService.retrieveEntityData(eq("User"), anyString())).thenReturn(userData);

        // Mock rule execution results
        RuleExecutionResult ruleResult = new RuleExecutionResult(1L, "Test Rule", true);
        when(ruleExecutionService.executeRuleWithDefinition(eq(testRule), any(ExecutionContext.class)))
            .thenReturn(ruleResult);

        // When
        EntityFilterResult result = entityFilterService.filterEntitiesBatch(testRequest);

        // Then
        assertNotNull(result);
        assertEquals(3L, result.getTotalProcessed());
        assertEquals(3L, result.getTotalMatched());
        assertEquals(0L, result.getTotalFailed());
        assertNotNull(result.getMetrics());
        assertTrue(result.getMetrics().getBatchesProcessed() >= 1);
    }

    @Test
    void testGetEntityCount_Success() {
        // Given
        when(entityTypeRepository.findByTypeNameActive("User"))
            .thenReturn(Optional.of(testEntityType));

        // When
        Long count = entityFilterService.getEntityCount("User");

        // Then
        assertNotNull(count);
        assertEquals(1000L, count); // Placeholder implementation returns 1000
        verify(entityTypeRepository).findByTypeNameActive("User");
    }

    @Test
    void testValidateRuleForEntityType_Success() {
        // Given
        when(entityTypeRepository.findByTypeNameActive("User"))
            .thenReturn(Optional.of(testEntityType));

        // When
        boolean isValid = entityFilterService.validateRuleForEntityType("User", testRule);

        // Then
        assertTrue(isValid); // Placeholder implementation returns true for valid fields
        verify(entityTypeRepository).findByTypeNameActive("User");
    }

    @Test
    void testValidateRuleForEntityType_EntityTypeNotFound() {
        // Given
        when(entityTypeRepository.findByTypeNameActive("NonExistentType"))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuleNotFoundException.class, () -> {
            entityFilterService.validateRuleForEntityType("NonExistentType", testRule);
        });
    }

    @Test
    void testFilterEntities_WithoutEntityIds() {
        // Given - request without specific entity IDs
        testRequest.setEntityIds(null);
        when(entityTypeRepository.findByTypeNameActive("User"))
            .thenReturn(Optional.of(testEntityType));

        // Mock entity data retrieval for generated entities
        Map<String, Object> userData = Map.of("status", "active");
        when(entityTypeService.retrieveEntityData(eq("User"), anyString())).thenReturn(userData);

        // Mock rule execution results for generated entities
        RuleExecutionResult ruleResult = new RuleExecutionResult(1L, "Test Rule", true);
        when(ruleExecutionService.executeRuleWithDefinition(eq(testRule), any(ExecutionContext.class)))
            .thenReturn(ruleResult);

        // When
        EntityFilterResult result = entityFilterService.filterEntities(testRequest);

        // Then
        assertNotNull(result);
        // The service should query all entity IDs and apply pagination
        // In the placeholder implementation, it generates 1000 entities and takes first 20
        assertEquals(20L, result.getTotalProcessed()); // Page size
        assertNotNull(result.getPagination());
        assertEquals(1000L, result.getPagination().getTotalElements()); // Total available
    }

    @Test
    void testFilterEntities_WithTraces() {
        // Given
        testRequest.setIncludeTrace(true);
        when(entityTypeRepository.findByTypeNameActive("User"))
            .thenReturn(Optional.of(testEntityType));

        Map<String, Object> userData = Map.of("status", "active");
        when(entityTypeService.retrieveEntityData(eq("User"), anyString())).thenReturn(userData);

        RuleExecutionResult ruleResult = new RuleExecutionResult(1L, "Test Rule", true);
        RuleExecutionResult.ExecutionTrace trace = new RuleExecutionResult.ExecutionTrace("condition1", "matched", true, "active", "active");
        ruleResult.addTrace(trace);

        when(ruleExecutionService.executeRuleWithDefinition(eq(testRule), any(ExecutionContext.class)))
            .thenReturn(ruleResult);

        // When
        EntityFilterResult result = entityFilterService.filterEntities(testRequest);

        // Then
        assertNotNull(result);
        assertTrue(result.getEntities().size() > 0);
        EntityFilterResult.FilteredEntity firstEntity = result.getEntities().get(0);
        assertNotNull(firstEntity.getTrace());
        assertTrue(firstEntity.getTrace().containsKey("trace_0"));
    }
}