package com.rulesengine.fixtures;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rulesengine.dto.*;
import com.rulesengine.entity.*;
import com.rulesengine.model.RuleDefinition;
import com.rulesengine.model.RuleItem;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Comprehensive test data fixtures for Rules Engine testing.
 * Provides realistic test data for all major entities and DTOs.
 */
@Component
public class TestDataFixtures {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ========== Rule Fixtures ==========

    public CreateRuleRequest createSimpleRuleRequest() {
        CreateRuleRequest request = new CreateRuleRequest();
        request.setName("Customer Age Validation");
        request.setDescription("Validates customer age is above 18");
        request.setRuleDefinitionJson(createSimpleRuleDefinitionJson());
        request.setFolderId(1L);
        return request;
    }

    public CreateRuleRequest createComplexRuleRequest() {
        CreateRuleRequest request = new CreateRuleRequest();
        request.setName("Premium Customer Eligibility");
        request.setDescription("Complex rule for determining premium customer eligibility");
        request.setRuleDefinitionJson(createComplexRuleDefinitionJson());
        request.setFolderId(2L);
        return request;
    }

    public RuleEntity createRuleEntity() {
        RuleEntity rule = new RuleEntity();
        rule.setId(1L);
        rule.setName("Test Rule");
        rule.setDescription("Test rule description");
        rule.setRuleDefinitionJson(createSimpleRuleDefinitionJson());
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());
        rule.setCreatedBy("test-user");
        rule.setVersion(1);
        rule.setIsActive(true);
        return rule;
    }

    public String createSimpleRuleDefinitionJson() {
        try {
            RuleDefinition rule = new RuleDefinition();
            rule.setCombinator("and");
            
            RuleItem item = new RuleItem();
            item.setField("customerAge");
            item.setOperator(">");
            item.setValue("18");
            
            rule.setRules(Arrays.asList(item));
            
            return objectMapper.writeValueAsString(rule);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to create rule definition JSON", e);
        }
    }

    public String createComplexRuleDefinitionJson() {
        try {
            RuleDefinition mainRule = new RuleDefinition();
            mainRule.setCombinator("and");
            
            // Age condition
            RuleItem ageItem = new RuleItem();
            ageItem.setField("customerAge");
            ageItem.setOperator(">=");
            ageItem.setValue("25");
            
            // Income condition group
            RuleItem incomeGroup = new RuleItem();
            incomeGroup.setCombinator("or");
            
            RuleItem salaryItem = new RuleItem();
            salaryItem.setField("annualSalary");
            salaryItem.setOperator(">");
            salaryItem.setValue("50000");
            
            RuleItem creditScoreItem = new RuleItem();
            creditScoreItem.setField("creditScore");
            creditScoreItem.setOperator(">=");
            creditScoreItem.setValue("700");
            
            incomeGroup.setRules(Arrays.asList(salaryItem, creditScoreItem));
            
            mainRule.setRules(Arrays.asList(ageItem, incomeGroup));
            
            return objectMapper.writeValueAsString(mainRule);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to create complex rule definition JSON", e);
        }
    }

    // ========== Field Configuration Fixtures ==========

    public CreateFieldConfigRequest createStringFieldConfigRequest() {
        CreateFieldConfigRequest request = new CreateFieldConfigRequest();
        request.setFieldName("customerType");
        request.setFieldType("STRING");
        request.setDescription("Customer classification type");
        request.setIsRequired(true);
        request.setDefaultValue("STANDARD");
        return request;
    }

    public CreateFieldConfigRequest createGraphQLFieldConfigRequest() {
        CreateFieldConfigRequest request = new CreateFieldConfigRequest();
        request.setFieldName("customerScore");
        request.setFieldType("NUMBER");
        request.setDescription("Customer credit score from external GraphQL service");
        request.setDataServiceConfigJson(createGraphQLDataServiceConfigJson());
        request.setMapperExpression("customer.creditScore");
        request.setIsRequired(true);
        return request;
    }

    public CreateFieldConfigRequest createCalculatedFieldConfigRequest() {
        CreateFieldConfigRequest request = new CreateFieldConfigRequest();
        request.setFieldName("totalAmount");
        request.setFieldType("NUMBER");
        request.setDescription("Total amount including tax");
        request.setIsCalculated(true);
        request.setCalculatorConfigJson(createCalculatorConfigJson());
        request.setDependenciesJson("[\"baseAmount\", \"taxRate\"]");
        request.setIsRequired(true);
        return request;
    }

    public FieldConfigEntity createFieldConfigEntity() {
        FieldConfigEntity fieldConfig = new FieldConfigEntity();
        fieldConfig.setId(1L);
        fieldConfig.setFieldName("testField");
        fieldConfig.setFieldType("STRING");
        fieldConfig.setDescription("Test field description");
        fieldConfig.setIsRequired(false);
        fieldConfig.setIsCalculated(false);
        fieldConfig.setVersion(1);
        fieldConfig.setCreatedAt(LocalDateTime.now());
        return fieldConfig;
    }

    public String createGraphQLDataServiceConfigJson() {
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("type", "GRAPHQL");
            config.put("endpoint", "https://api.example.com/graphql");
            config.put("query", "query GetCustomer($id: ID!) { customer(id: $id) { creditScore } }");
            
            Map<String, Object> authConfig = new HashMap<>();
            authConfig.put("type", "API_KEY");
            authConfig.put("apiKey", "test-api-key");
            config.put("authConfig", authConfig);
            
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to create GraphQL config JSON", e);
        }
    }

    public String createRestDataServiceConfigJson() {
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("type", "REST");
            config.put("endpoint", "https://api.example.com/customers/{id}");
            config.put("method", "GET");
            
            Map<String, Object> authConfig = new HashMap<>();
            authConfig.put("type", "BEARER_TOKEN");
            authConfig.put("token", "test-bearer-token");
            config.put("authConfig", authConfig);
            
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to create REST config JSON", e);
        }
    }

    public String createCalculatorConfigJson() {
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("type", "EXPRESSION");
            config.put("expression", "baseAmount * (1 + taxRate)");
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to create calculator config JSON", e);
        }
    }

    // ========== Execution Context Fixtures ==========

    public ExecutionContext createSimpleExecutionContext() {
        ExecutionContext context = new ExecutionContext();
        context.setEntityId("customer-123");
        context.setEntityType("CUSTOMER");
        
        Map<String, Object> fieldValues = new HashMap<>();
        fieldValues.put("customerAge", 25);
        fieldValues.put("customerType", "PREMIUM");
        fieldValues.put("annualSalary", 75000);
        fieldValues.put("creditScore", 750);
        context.setFieldValues(fieldValues);
        
        return context;
    }

    public ExecutionContext createComplexExecutionContext() {
        ExecutionContext context = new ExecutionContext();
        context.setEntityId("customer-456");
        context.setEntityType("CUSTOMER");
        
        Map<String, Object> fieldValues = new HashMap<>();
        fieldValues.put("customerAge", 30);
        fieldValues.put("customerType", "PREMIUM");
        fieldValues.put("annualSalary", 95000);
        fieldValues.put("creditScore", 800);
        fieldValues.put("accountBalance", 25000.50);
        fieldValues.put("hasLoan", false);
        fieldValues.put("registrationDate", "2020-01-15");
        fieldValues.put("lastLoginDate", "2023-12-01T10:30:00");
        context.setFieldValues(fieldValues);
        
        return context;
    }

    public BatchExecutionRequest createBatchExecutionRequest() {
        BatchExecutionRequest request = new BatchExecutionRequest();
        request.setRuleIds(List.of(1L, 2L, 3L));
        request.setContext(createSimpleExecutionContext());
        return request;
    }

    // ========== Entity Fixtures ==========

    public FolderEntity createFolderEntity() {
        FolderEntity folder = new FolderEntity();
        folder.setId(1L);
        folder.setName("Test Folder");
        folder.setDescription("Test folder description");
        folder.setPath("/test-folder");
        folder.setCreatedAt(LocalDateTime.now());
        return folder;
    }

    public EntityTypeEntity createEntityTypeEntity() {
        EntityTypeEntity entityType = new EntityTypeEntity();
        entityType.setId(1L);
        entityType.setTypeName("CUSTOMER");
        entityType.setDescription("Customer entity type");
        entityType.setDataServiceConfigJson(createRestDataServiceConfigJson());
        entityType.setFieldMappingsJson(createFieldMappingsJson());
        entityType.setCreatedAt(LocalDateTime.now());
        entityType.setUpdatedAt(LocalDateTime.now());
        return entityType;
    }

    public String createFieldMappingsJson() {
        try {
            Map<String, String> mappings = new HashMap<>();
            mappings.put("customerId", "id");
            mappings.put("customerName", "name");
            mappings.put("customerAge", "age");
            mappings.put("customerType", "type");
            return objectMapper.writeValueAsString(mappings);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to create field mappings JSON", e);
        }
    }

    // ========== Response Fixtures ==========

    public RuleExecutionResult createSuccessfulExecutionResult() {
        RuleExecutionResult result = new RuleExecutionResult();
        result.setRuleId(1L);
        result.setRuleName("Test Rule");
        result.setResult(true);
        result.setExecutionDurationMs(150L);
        result.addTrace("rule", "Evaluating rule: Test Rule", true);
        result.addTrace("condition", "Condition: customerAge > 18", true);
        result.addTrace("field", "Field value: customerAge = 25", true);
        result.addTrace("condition", "Condition result: true", true);
        result.addTrace("rule", "Rule result: true", true);
        return result;
    }

    public RuleExecutionResult createFailedExecutionResult() {
        RuleExecutionResult result = new RuleExecutionResult();
        result.setRuleId(2L);
        result.setRuleName("Failed Rule");
        result.setResult(false);
        result.setExecutionDurationMs(75L);
        result.setErrorMessage("Field 'customerAge' not found in context");
        return result;
    }

    // ========== Mock External Service Responses ==========

    public String createMockGraphQLResponse() {
        try {
            Map<String, Object> response = new HashMap<>();
            Map<String, Object> data = new HashMap<>();
            Map<String, Object> customer = new HashMap<>();
            customer.put("creditScore", 750);
            data.put("customer", customer);
            response.put("data", data);
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to create mock GraphQL response", e);
        }
    }

    public String createMockRestResponse() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("id", "customer-123");
            response.put("name", "John Doe");
            response.put("age", 30);
            response.put("type", "PREMIUM");
            response.put("creditScore", 750);
            response.put("accountBalance", 25000.50);
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to create mock REST response", e);
        }
    }

    // ========== Error Response Fixtures ==========

    public String createValidationErrorResponse() {
        try {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Validation failed");
            error.put("message", "Field name must be unique");
            error.put("timestamp", LocalDateTime.now().toString());
            error.put("path", "/api/field-configs");
            return objectMapper.writeValueAsString(error);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to create validation error response", e);
        }
    }

    public String createNotFoundErrorResponse() {
        try {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Not Found");
            error.put("message", "Rule with ID 999 not found");
            error.put("timestamp", LocalDateTime.now().toString());
            error.put("path", "/api/rules/999");
            return objectMapper.writeValueAsString(error);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to create not found error response", e);
        }
    }

    // ========== Utility Methods ==========

    public List<RuleEntity> createMultipleRuleEntities(int count) {
        List<RuleEntity> rules = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            RuleEntity rule = createRuleEntity();
            rule.setId((long) i);
            rule.setName("Test Rule " + i);
            rule.setDescription("Test rule description " + i);
            rules.add(rule);
        }
        return rules;
    }

    public List<FieldConfigEntity> createMultipleFieldConfigEntities(int count) {
        List<FieldConfigEntity> fieldConfigs = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            FieldConfigEntity fieldConfig = createFieldConfigEntity();
            fieldConfig.setId((long) i);
            fieldConfig.setFieldName("testField" + i);
            fieldConfig.setDescription("Test field description " + i);
            fieldConfigs.add(fieldConfig);
        }
        return fieldConfigs;
    }

    public Map<String, Object> createLargeFieldValueContext() {
        Map<String, Object> fieldValues = new HashMap<>();
        fieldValues.put("customerId", "customer-789");
        fieldValues.put("customerName", "Jane Smith");
        fieldValues.put("customerAge", 35);
        fieldValues.put("customerType", "PREMIUM");
        fieldValues.put("annualSalary", 120000);
        fieldValues.put("creditScore", 820);
        fieldValues.put("accountBalance", 50000.75);
        fieldValues.put("hasLoan", true);
        fieldValues.put("loanAmount", 15000.00);
        fieldValues.put("registrationDate", "2019-03-20");
        fieldValues.put("lastLoginDate", "2023-12-01T15:45:30");
        fieldValues.put("preferredLanguage", "en");
        fieldValues.put("marketingOptIn", true);
        fieldValues.put("riskCategory", "LOW");
        fieldValues.put("accountStatus", "ACTIVE");
        return fieldValues;
    }
}