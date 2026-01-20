package com.rulesengine.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for HTTP clients with connection pooling and timeout management.
 */
@Configuration
public class HttpClientConfig {
    
    @Value("${app.http.client.connection-timeout:30000}")
    private int connectionTimeoutMs;
    
    @Value("${app.http.client.read-timeout:30000}")
    private int readTimeoutMs;
    
    @Value("${app.http.client.max-connections:100}")
    private int maxConnections;
    
    @Value("${app.http.client.max-connections-per-route:20}")
    private int maxConnectionsPerRoute;
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate(httpRequestFactory());
    }
    
    @Bean
    public HttpComponentsClientHttpRequestFactory httpRequestFactory() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setHttpClient(httpClient());
        factory.setConnectTimeout(Duration.ofMillis(connectionTimeoutMs));
        factory.setConnectionRequestTimeout(Duration.ofMillis(connectionTimeoutMs));
        return factory;
    }
    
    @Bean
    public CloseableHttpClient httpClient() {
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(Timeout.ofMilliseconds(connectionTimeoutMs))
            .setResponseTimeout(Timeout.ofMilliseconds(readTimeoutMs))
            .build();
        
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(maxConnections);
        connectionManager.setDefaultMaxPerRoute(maxConnectionsPerRoute);
        
        return HttpClients.custom()
            .setDefaultRequestConfig(requestConfig)
            .setConnectionManager(connectionManager)
            .build();
    }
}