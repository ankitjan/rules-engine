package com.rulesengine.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rulesengine.client.config.DataServiceConfig;
import com.rulesengine.dto.FieldValueResponse;
import com.rulesengine.dto.FieldValueSearchRequest;
import com.rulesengine.dto.PagedFieldValueResponse;
import com.rulesengine.entity.FieldConfigEntity;
import com.rulesengine.exception.RuleNotFoundException;
import com.rulesengine.repository.FieldConfigRepository;
import com.rulesengine.service.DataServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FieldValuesService {

    private static final Logger logger = LoggerFactory.getLogger(FieldValuesService.class);

    private final FieldConfigRepository fieldConfigRepository;
    private final DataServiceClient dataServiceClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public FieldValuesService(FieldConfigRepository fieldConfigRepository,
                            DataServiceClient dataServiceClient,
                            ObjectMapper objectMapper) {
        this.fieldConfigRepository = fieldConfigRepository;
        this.dataServiceClient = dataServiceClient;
        this.objectMapper = objectMapper;
    }

    public PagedFieldValueResponse searchFieldValues(FieldValueSearchRequest request) {
        logger.debug("Searching field values for field: {} with query: {}", 
                    request.getFieldName(), request.getQuery());

        // Get field configuration
        FieldConfigEntity fieldConfig = fieldConfigRepository.findByFieldNameActive(request.getFieldName())
                .orElseThrow(() -> new RuleNotFoundException("Field configuration not found: " + request.getFieldName()));

        // Get all values for the field
        List<FieldValueResponse> allValues = getFieldValuesFromDataService(fieldConfig, request.getContext());

        // Apply search filtering
        List<FieldValueResponse> filteredValues = filterValues(allValues, request);

        // Apply sorting
        List<FieldValueResponse> sortedValues = sortValues(filteredValues, request.getSortBy(), request.getSortDirection());

        // Apply pagination
        int totalElements = sortedValues.size();
        int startIndex = request.getPage() * request.getSize();
        int endIndex = Math.min(startIndex + request.getSize(), totalElements);
        
        List<FieldValueResponse> pageContent = startIndex < totalElements 
            ? sortedValues.subList(startIndex, endIndex) 
            : new ArrayList<>();

        // Calculate pagination metadata
        int totalPages = (int) Math.ceil((double) totalElements / request.getSize());
        boolean isFirst = request.getPage() == 0;
        boolean isLast = request.getPage() >= totalPages - 1;
        boolean hasNext = request.getPage() < totalPages - 1;
        boolean hasPrevious = request.getPage() > 0;

        return new PagedFieldValueResponse(
            pageContent,
            request.getPage(),
            request.getSize(),
            (long) totalElements,
            totalPages,
            isFirst,
            isLast,
            hasNext,
            hasPrevious,
            request.getFieldName(),
            request.getQuery()
        );
    }

    @Cacheable(value = "fieldValues", key = "#fieldName + '_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #includeInactive")
    public List<FieldValueResponse> getFieldValues(String fieldName, Pageable pageable, boolean includeInactive) {
        logger.debug("Getting field values for field: {} with pagination", fieldName);

        // Get field configuration
        FieldConfigEntity fieldConfig = fieldConfigRepository.findByFieldNameActive(fieldName)
                .orElseThrow(() -> new RuleNotFoundException("Field configuration not found: " + fieldName));

        // Get values from data service
        List<FieldValueResponse> allValues = getFieldValuesFromDataService(fieldConfig, null);

        // Filter inactive values if needed
        if (!includeInactive) {
            allValues = allValues.stream()
                    .filter(value -> value.getIsActive() == null || value.getIsActive())
                    .collect(Collectors.toList());
        }

        // Apply sorting
        allValues = applySorting(allValues, pageable.getSort());

        // Apply pagination
        int startIndex = (int) pageable.getOffset();
        int endIndex = Math.min(startIndex + pageable.getPageSize(), allValues.size());
        
        return startIndex < allValues.size() 
            ? allValues.subList(startIndex, endIndex) 
            : new ArrayList<>();
    }

    @Cacheable(value = "distinctFieldValues", key = "#fieldName + '_' + #limit + '_' + #includeInactive")
    public List<FieldValueResponse> getDistinctFieldValues(String fieldName, int limit, boolean includeInactive) {
        logger.debug("Getting distinct field values for field: {} with limit: {}", fieldName, limit);

        // Get field configuration
        FieldConfigEntity fieldConfig = fieldConfigRepository.findByFieldNameActive(fieldName)
                .orElseThrow(() -> new RuleNotFoundException("Field configuration not found: " + fieldName));

        // Get values from data service
        List<FieldValueResponse> allValues = getFieldValuesFromDataService(fieldConfig, null);

        // Filter inactive values if needed
        if (!includeInactive) {
            allValues = allValues.stream()
                    .filter(value -> value.getIsActive() == null || value.getIsActive())
                    .collect(Collectors.toList());
        }

        // Get distinct values based on the actual value
        Map<Object, FieldValueResponse> distinctMap = new LinkedHashMap<>();
        for (FieldValueResponse value : allValues) {
            if (!distinctMap.containsKey(value.getValue())) {
                distinctMap.put(value.getValue(), value);
            }
        }

        // Apply limit
        return distinctMap.values().stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    @CacheEvict(value = {"fieldValues", "distinctFieldValues"}, key = "#fieldName")
    public void refreshFieldValueCache(String fieldName) {
        logger.info("Refreshing field value cache for field: {}", fieldName);
        // Cache will be refreshed on next access
    }

    public void preloadFieldValues(List<String> fieldNames) {
        logger.info("Preloading field values for {} fields", fieldNames.size());
        
        for (String fieldName : fieldNames) {
            try {
                // This will populate the cache
                getDistinctFieldValues(fieldName, 100, false);
                logger.debug("Preloaded field values for field: {}", fieldName);
            } catch (Exception e) {
                logger.warn("Failed to preload field values for field: {}: {}", fieldName, e.getMessage());
            }
        }
    }

    public Object getFieldValueStatistics(String fieldName) {
        logger.debug("Getting field value statistics for field: {}", fieldName);

        // Get field configuration
        FieldConfigEntity fieldConfig = fieldConfigRepository.findByFieldNameActive(fieldName)
                .orElseThrow(() -> new RuleNotFoundException("Field configuration not found: " + fieldName));

        // Get values from data service
        List<FieldValueResponse> allValues = getFieldValuesFromDataService(fieldConfig, null);

        // Calculate statistics
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("fieldName", fieldName);
        statistics.put("totalValues", allValues.size());
        statistics.put("activeValues", allValues.stream().filter(v -> v.getIsActive() == null || v.getIsActive()).count());
        statistics.put("inactiveValues", allValues.stream().filter(v -> v.getIsActive() != null && !v.getIsActive()).count());
        statistics.put("lastUpdated", LocalDateTime.now());

        // Value type distribution
        Map<String, Long> typeDistribution = allValues.stream()
                .collect(Collectors.groupingBy(
                    v -> v.getValueType() != null ? v.getValueType() : "UNKNOWN",
                    Collectors.counting()
                ));
        statistics.put("typeDistribution", typeDistribution);

        // Category distribution if available
        Map<String, Long> categoryDistribution = allValues.stream()
                .filter(v -> v.getCategory() != null)
                .collect(Collectors.groupingBy(
                    FieldValueResponse::getCategory,
                    Collectors.counting()
                ));
        if (!categoryDistribution.isEmpty()) {
            statistics.put("categoryDistribution", categoryDistribution);
        }

        return statistics;
    }

    private List<FieldValueResponse> getFieldValuesFromDataService(FieldConfigEntity fieldConfig, Map<String, Object> context) {
        try {
            // Check if field has data service configuration
            if (fieldConfig.getDataServiceConfigJson() == null || fieldConfig.getDataServiceConfigJson().trim().isEmpty()) {
                // Return mock values for fields without data service
                return generateMockValues(fieldConfig);
            }

            // Parse data service configuration
            DataServiceConfig dataServiceConfig = objectMapper.readValue(
                fieldConfig.getDataServiceConfigJson(), 
                DataServiceConfig.class
            );

            // Query data service
            Object response = dataServiceClient.executeRequest(dataServiceConfig, context);

            // Extract values using mapper expression
            return extractFieldValues(response, fieldConfig);

        } catch (Exception e) {
            logger.warn("Failed to get field values from data service for field: {}: {}", 
                       fieldConfig.getFieldName(), e.getMessage());
            // Return mock values as fallback
            return generateMockValues(fieldConfig);
        }
    }

    private List<FieldValueResponse> extractFieldValues(Object response, FieldConfigEntity fieldConfig) {
        List<FieldValueResponse> values = new ArrayList<>();

        try {
            // If response is a list, extract values from each item
            if (response instanceof List) {
                List<?> responseList = (List<?>) response;
                for (Object item : responseList) {
                    FieldValueResponse value = extractSingleValue(item, fieldConfig);
                    if (value != null) {
                        values.add(value);
                    }
                }
            } else {
                // Single value response
                FieldValueResponse value = extractSingleValue(response, fieldConfig);
                if (value != null) {
                    values.add(value);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to extract field values for field: {}: {}", 
                       fieldConfig.getFieldName(), e.getMessage());
        }

        return values;
    }

    private FieldValueResponse extractSingleValue(Object item, FieldConfigEntity fieldConfig) {
        try {
            Object value = item;
            String label = String.valueOf(value);

            // If there's a mapper expression, use it to extract the value
            if (fieldConfig.getMapperExpression() != null && !fieldConfig.getMapperExpression().trim().isEmpty()) {
                // Simple dot notation support for now
                String[] pathParts = fieldConfig.getMapperExpression().split("\\.");
                Object current = item;
                for (String part : pathParts) {
                    if (current instanceof Map) {
                        current = ((Map<?, ?>) current).get(part);
                    } else {
                        // Use reflection for object properties
                        try {
                            current = current.getClass().getMethod("get" + capitalize(part)).invoke(current);
                        } catch (Exception e) {
                            logger.debug("Failed to extract property {} from object", part);
                            break;
                        }
                    }
                    if (current == null) break;
                }
                value = current;
                label = String.valueOf(value);
            }

            return new FieldValueResponse(
                value,
                label,
                null, // description
                fieldConfig.getFieldType(),
                true, // isActive
                null, // usageCount
                null, // lastUsed
                null, // metadata
                "data_service", // source
                null, // category
                null  // sortOrder
            );

        } catch (Exception e) {
            logger.debug("Failed to extract single value: {}", e.getMessage());
            return null;
        }
    }

    private List<FieldValueResponse> generateMockValues(FieldConfigEntity fieldConfig) {
        List<FieldValueResponse> mockValues = new ArrayList<>();
        String fieldType = fieldConfig.getFieldType();

        switch (fieldType.toUpperCase()) {
            case "STRING":
                mockValues.add(new FieldValueResponse("active", "Active"));
                mockValues.add(new FieldValueResponse("inactive", "Inactive"));
                mockValues.add(new FieldValueResponse("pending", "Pending"));
                break;
            case "NUMBER":
                mockValues.add(new FieldValueResponse(1, "1"));
                mockValues.add(new FieldValueResponse(10, "10"));
                mockValues.add(new FieldValueResponse(100, "100"));
                break;
            case "BOOLEAN":
                mockValues.add(new FieldValueResponse(true, "True"));
                mockValues.add(new FieldValueResponse(false, "False"));
                break;
            case "DATE":
                mockValues.add(new FieldValueResponse("2024-01-01", "2024-01-01"));
                mockValues.add(new FieldValueResponse("2024-12-31", "2024-12-31"));
                break;
            default:
                mockValues.add(new FieldValueResponse("sample_value", "Sample Value"));
        }

        return mockValues;
    }

    private List<FieldValueResponse> filterValues(List<FieldValueResponse> values, FieldValueSearchRequest request) {
        if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            return values;
        }

        String query = request.getQuery().toLowerCase();
        return values.stream()
                .filter(value -> {
                    // Search in value and label
                    boolean matches = false;
                    if (value.getValue() != null) {
                        matches = String.valueOf(value.getValue()).toLowerCase().contains(query);
                    }
                    if (!matches && value.getLabel() != null) {
                        matches = value.getLabel().toLowerCase().contains(query);
                    }
                    if (!matches && value.getDescription() != null) {
                        matches = value.getDescription().toLowerCase().contains(query);
                    }
                    return matches;
                })
                .collect(Collectors.toList());
    }

    private List<FieldValueResponse> sortValues(List<FieldValueResponse> values, String sortBy, String sortDirection) {
        Comparator<FieldValueResponse> comparator;

        switch (sortBy.toLowerCase()) {
            case "label":
                comparator = Comparator.comparing(v -> v.getLabel() != null ? v.getLabel() : "");
                break;
            case "usagecount":
                comparator = Comparator.comparing(v -> v.getUsageCount() != null ? v.getUsageCount() : 0L);
                break;
            case "lastused":
                comparator = Comparator.comparing(v -> v.getLastUsed() != null ? v.getLastUsed() : LocalDateTime.MIN);
                break;
            default: // "value"
                comparator = Comparator.comparing(v -> v.getValue() != null ? String.valueOf(v.getValue()) : "");
        }

        if ("desc".equalsIgnoreCase(sortDirection)) {
            comparator = comparator.reversed();
        }

        return values.stream().sorted(comparator).collect(Collectors.toList());
    }

    private List<FieldValueResponse> applySorting(List<FieldValueResponse> values, org.springframework.data.domain.Sort sort) {
        if (sort.isUnsorted()) {
            return values;
        }

        Comparator<FieldValueResponse> comparator = null;
        for (org.springframework.data.domain.Sort.Order order : sort) {
            Comparator<FieldValueResponse> fieldComparator;
            
            switch (order.getProperty().toLowerCase()) {
                case "label":
                    fieldComparator = Comparator.comparing(v -> v.getLabel() != null ? v.getLabel() : "");
                    break;
                case "usagecount":
                    fieldComparator = Comparator.comparing(v -> v.getUsageCount() != null ? v.getUsageCount() : 0L);
                    break;
                case "lastused":
                    fieldComparator = Comparator.comparing(v -> v.getLastUsed() != null ? v.getLastUsed() : LocalDateTime.MIN);
                    break;
                default: // "value"
                    fieldComparator = Comparator.comparing(v -> v.getValue() != null ? String.valueOf(v.getValue()) : "");
            }

            if (order.isDescending()) {
                fieldComparator = fieldComparator.reversed();
            }

            comparator = comparator == null ? fieldComparator : comparator.thenComparing(fieldComparator);
        }

        return values.stream().sorted(comparator).collect(Collectors.toList());
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}