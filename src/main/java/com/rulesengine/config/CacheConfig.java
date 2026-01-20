package com.rulesengine.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for caching field values and other frequently accessed data
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    /**
     * Configure cache manager for field values and other cached data
     */
    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        
        // Pre-configure cache names
        cacheManager.setCacheNames(java.util.Arrays.asList(
            "fieldValues",           // Field values resolved from data services
            "distinctFieldValues",   // Distinct field values for dropdowns
            "fieldConfigs",          // Field configuration entities
            "builderFieldConfigs",   // Field configurations optimized for Rule Builder
            "dataServiceResponses",  // Raw responses from data services
            "resolutionPlans"        // Field resolution plans
        ));
        
        // Allow dynamic cache creation
        cacheManager.setAllowNullValues(false);
        
        return cacheManager;
    }
}