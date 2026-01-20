package com.rulesengine.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rulesengine.dto.EntityFilterRequest;
import com.rulesengine.dto.EntityFilterResult;
import com.rulesengine.model.RuleDefinition;
import com.rulesengine.model.RuleItem;
import com.rulesengine.service.EntityFilterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class EntityControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EntityFilterService entityFilterService;

    @InjectMocks
    private EntityController entityController;

    private ObjectMapper objectMapper;
    private EntityFilterRequest testRequest;
    private EntityFilterResult testResult;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(entityController).build();
        objectMapper = new ObjectMapper();

        // Create test rule definition
        RuleItem condition = new RuleItem("status", "equals", "active");
        RuleDefinition rule = new RuleDefinition("and", Arrays.asList(condition));

        // Create test request
        testRequest = new EntityFilterRequest();
        testRequest.setEntityType("User");
        testRequest.setEntityIds(Arrays.asList("user1", "user2", "user3"));
        testRequest.setRule(rule);
        testRequest.setPage(0);
        testRequest.setSize(20);
        testRequest.setIncludeEntityData(true);

        // Create test result
        testResult = new EntityFilterResult();
        testResult.setTotalProcessed(3L);
        testResult.setTotalMatched(2L);
        testResult.setTotalFailed(0L);
        testResult.setExecutionTimeMs(150L);
        testResult.setExecutedAt(LocalDateTime.now());

        List<EntityFilterResult.FilteredEntity> entities = Arrays.asList(
            new EntityFilterResult.FilteredEntity("user1", true),
            new EntityFilterResult.FilteredEntity("user2", true),
            new EntityFilterResult.FilteredEntity("user3", false)
        );
        testResult.setEntities(entities);

        EntityFilterResult.PaginationInfo pagination = new EntityFilterResult.PaginationInfo(0, 20, 3L);
        testResult.setPagination(pagination);
    }

    @Test
    void testFilterEntities_Success() throws Exception {
        // Given
        when(entityFilterService.filterEntities(any(EntityFilterRequest.class)))
            .thenReturn(testResult);

        // When & Then
        mockMvc.perform(post("/api/entities/filter")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProcessed").value(3))
                .andExpect(jsonPath("$.totalMatched").value(2))
                .andExpect(jsonPath("$.totalFailed").value(0))
                .andExpect(jsonPath("$.entities").isArray())
                .andExpect(jsonPath("$.entities.length()").value(3))
                .andExpect(jsonPath("$.entities[0].entityId").value("user1"))
                .andExpect(jsonPath("$.entities[0].matched").value(true))
                .andExpect(jsonPath("$.pagination.page").value(0))
                .andExpect(jsonPath("$.pagination.size").value(20))
                .andExpect(jsonPath("$.pagination.totalElements").value(3));
    }

    @Test
    void testFilterEntitiesBatch_Success() throws Exception {
        // Given
        testRequest.setBatchSize(100);
        EntityFilterResult.FilteringMetrics metrics = new EntityFilterResult.FilteringMetrics();
        metrics.setBatchesProcessed(1);
        metrics.setDataRetrievalTimeMs(50L);
        metrics.setRuleEvaluationTimeMs(30L);
        testResult.setMetrics(metrics);

        when(entityFilterService.filterEntitiesBatch(any(EntityFilterRequest.class)))
            .thenReturn(testResult);

        // When & Then
        mockMvc.perform(post("/api/entities/filter/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProcessed").value(3))
                .andExpect(jsonPath("$.totalMatched").value(2))
                .andExpect(jsonPath("$.metrics.batchesProcessed").value(1))
                .andExpect(jsonPath("$.metrics.dataRetrievalTimeMs").value(50))
                .andExpect(jsonPath("$.metrics.ruleEvaluationTimeMs").value(30));
    }

    @Test
    void testGetEntityCount_Success() throws Exception {
        // Given
        when(entityFilterService.getEntityCount("User")).thenReturn(1000L);

        // When & Then
        mockMvc.perform(get("/api/entities/User/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("1000"));
    }

    @Test
    void testValidateRuleForEntityType_Success() throws Exception {
        // Given
        when(entityFilterService.validateRuleForEntityType(any(String.class), any(RuleDefinition.class)))
            .thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/entities/validate-rule")
                .param("entityType", "User")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void testFilterEntities_InvalidRequest() throws Exception {
        // Given - invalid request without entity type
        EntityFilterRequest invalidRequest = new EntityFilterRequest();
        invalidRequest.setRule(testRequest.getRule());

        // When & Then
        mockMvc.perform(post("/api/entities/filter")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testFilterEntities_InvalidRule() throws Exception {
        // Given - request without rule
        EntityFilterRequest invalidRequest = new EntityFilterRequest();
        invalidRequest.setEntityType("User");

        // When & Then
        mockMvc.perform(post("/api/entities/filter")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}