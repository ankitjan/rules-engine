package com.rulesengine.client.config;

import org.springframework.http.HttpMethod;
import java.util.Map;

/**
 * Configuration for REST API data services.
 */
public class RestServiceConfig extends DataServiceConfig {
    
    private HttpMethod method = HttpMethod.GET;
    private Map<String, String> headers;
    private Map<String, String> queryParams;
    private String requestBody;
    
    public RestServiceConfig() {}
    
    public RestServiceConfig(String endpoint, HttpMethod method) {
        this.endpoint = endpoint;
        this.method = method;
    }
    
    @Override
    public String getServiceType() {
        return "REST";
    }
    
    // Getters and setters
    public HttpMethod getMethod() {
        return method;
    }
    
    public void setMethod(HttpMethod method) {
        this.method = method;
    }
    
    public Map<String, String> getHeaders() {
        return headers;
    }
    
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
    
    public Map<String, String> getQueryParams() {
        return queryParams;
    }
    
    public void setQueryParams(Map<String, String> queryParams) {
        this.queryParams = queryParams;
    }
    
    public String getRequestBody() {
        return requestBody;
    }
    
    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }
}