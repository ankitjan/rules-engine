package com.rulesengine.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI configuration for the Rules Engine API documentation.
 * Provides comprehensive API documentation with security schemes and examples.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${server.servlet.context-path:/api}")
    private String contextPath;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort + contextPath)
                                .description("Development server"),
                        new Server()
                                .url("https://api.rulesengine.com" + contextPath)
                                .description("Production server")
                ))
                // Security is optional - endpoints can be tested without authentication
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", 
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT token for authentication (optional for documentation access)")
                        )
                );
    }

    private Info apiInfo() {
        return new Info()
                .title("Rules Engine API")
                .description("""
                        A comprehensive Rules Engine backend service that provides:
                        
                        ## Features
                        - **Rule Management**: Create, update, delete, and organize business rules
                        - **Field Configuration**: Define dynamic fields with external data service integration
                        - **Rule Execution**: Execute rules against data contexts with field resolution
                        - **Entity Filtering**: Filter entity collections using business rules
                        - **Calculated Fields**: Support for computed fields with dependency resolution
                        - **Import/Export**: Backup and migrate rules and configurations
                        - **Analytics**: Rule usage and performance monitoring
                        
                        ## Accessing the API Documentation
                        - **Swagger UI**: Available at `/swagger-ui.html` (no authentication required)
                        - **OpenAPI Spec**: Available at `/api-docs` (JSON format)
                        - **Interactive Testing**: Use the "Try it out" feature in Swagger UI
                        
                        ## Authentication
                        This API uses JWT (JSON Web Token) for authentication. To test protected endpoints:
                        
                        1. **Get a JWT Token**: Use the `/auth/login` endpoint with credentials:
                           - Username: `admin`, Password: `admin123` (admin role)
                           - Username: `user`, Password: `user123` (user role)
                        
                        2. **Authorize in Swagger**: Click the "Authorize" button and enter: `Bearer <your-jwt-token>`
                        
                        3. **Test Endpoints**: Use the "Try it out" feature to test authenticated endpoints
                        
                        ## Default Test Credentials
                        - **Admin User**: username=`admin`, password=`admin123`
                        - **Regular User**: username=`user`, password=`user123`
                        
                        ## Error Handling
                        The API returns standard HTTP status codes and structured error responses:
                        - `200` - Success
                        - `201` - Created
                        - `400` - Bad Request (validation errors)
                        - `401` - Unauthorized
                        - `403` - Forbidden
                        - `404` - Not Found
                        - `409` - Conflict (duplicate resources)
                        - `500` - Internal Server Error
                        
                        ## Rate Limiting
                        API requests are rate-limited to prevent abuse. Check response headers for rate limit information.
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("Rules Engine Team")
                        .email("support@rulesengine.com")
                        .url("https://github.com/rulesengine/rules-engine"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }
}