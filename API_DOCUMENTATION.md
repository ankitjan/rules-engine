# Rules Engine API Documentation

This document describes the comprehensive API documentation and testing infrastructure implemented for the Rules Engine.

## Overview

The Rules Engine provides comprehensive API documentation using OpenAPI/Swagger with interactive testing capabilities, extensive test fixtures, and contract tests for external service integrations.

## Features Implemented

### 1. OpenAPI/Swagger Documentation

- **Complete API Documentation**: All REST endpoints are documented with OpenAPI 3.0 specifications
- **Interactive Swagger UI**: Available at `/swagger-ui.html` for testing APIs directly from the browser
- **Comprehensive Examples**: Request/response examples for all endpoints with multiple scenarios
- **Security Documentation**: JWT authentication scheme properly documented
- **Error Response Documentation**: Standardized error responses with examples

#### Accessing API Documentation

- **OpenAPI Spec**: `GET /api-docs` - Returns the complete OpenAPI specification in JSON format
- **Swagger UI**: `GET /swagger-ui.html` - Interactive API documentation interface
- **API Groups**: Documentation is organized into logical groups:
  - Rule Management APIs (`/api/rules/**`)
  - Field Configuration APIs (`/api/field-configs/**`, `/api/field-values/**`)
  - Entity Management APIs (`/api/entities/**`, `/api/entity-types/**`)
  - Administration APIs (`/api/export/**`, `/api/import/**`, `/api/analytics/**`)

### 2. Test Data Fixtures

Comprehensive test data fixtures are provided in `TestDataFixtures.java`:

- **Rule Fixtures**: Simple and complex rule definitions with nested conditions
- **Field Configuration Fixtures**: String fields, GraphQL fields, calculated fields
- **Execution Context Fixtures**: Various data contexts for rule testing
- **Entity Fixtures**: Folders, entity types, and related data
- **Mock Service Responses**: GraphQL and REST API response examples
- **Error Response Fixtures**: Standardized error response examples

### 3. Contract Tests for External Services

Contract tests in `ExternalServiceContractTest.java` validate:

- **GraphQL Client Integration**: Authentication, query execution, error handling
- **REST Client Integration**: HTTP methods, authentication, response parsing
- **Connection Validation**: Service connectivity and health checks
- **Performance Testing**: Response times and concurrent request handling
- **Error Recovery**: Retry logic and failure scenarios
- **Authentication Methods**: API keys, Bearer tokens, OAuth

### 4. Integration Tests

Comprehensive integration tests in `ApiDocumentationIntegrationTest.java`:

- **All API Endpoints**: Complete coverage of REST endpoints
- **Authentication & Authorization**: JWT token validation and role-based access
- **Error Handling**: Validation errors, not found, unauthorized responses
- **Content Negotiation**: JSON content types and accept headers
- **CORS Support**: Cross-origin request handling
- **Performance Validation**: Response time requirements

### 5. Documentation Generation

Automated documentation generation in `ApiDocumentationGenerator.java`:

- **Markdown Documentation**: Complete API guide with examples
- **OpenAPI Specification Export**: JSON specification file generation
- **Postman Collection**: Ready-to-use Postman collection for API testing

## Configuration

### OpenAPI Configuration

The OpenAPI documentation is configured in `OpenApiConfig.java`:

```yaml
springdoc:
  api-docs:
    path: /api-docs
    enabled: true
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
    try-it-out-enabled: true
    operations-sorter: method
    tags-sorter: alpha
```

### Security Integration

JWT authentication is properly integrated with OpenAPI:

- Bearer token authentication scheme
- Security requirements on protected endpoints
- Example JWT tokens in documentation

## Usage Examples

### 1. Creating a Rule

```bash
curl -X POST http://localhost:8080/api/rules \
  -H "Authorization: Bearer <jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Customer Age Validation",
    "description": "Validates customer age is above 18",
    "ruleDefinitionJson": "{\"combinator\":\"and\",\"rules\":[{\"field\":\"customerAge\",\"operator\":\">\",\"value\":\"18\"}]}",
    "folderId": 1
  }'
```

### 2. Executing a Rule

```bash
curl -X POST http://localhost:8080/api/rules/1/execute \
  -H "Authorization: Bearer <jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "entityId": "customer-123",
    "entityType": "CUSTOMER",
    "fieldValues": {
      "customerAge": 25,
      "customerType": "PREMIUM"
    }
  }'
```

### 3. Creating a Field Configuration

```bash
curl -X POST http://localhost:8080/api/field-configs \
  -H "Authorization: Bearer <jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "fieldName": "customerScore",
    "fieldType": "NUMBER",
    "description": "Customer credit score from external service",
    "dataServiceConfigJson": "{\"type\":\"GRAPHQL\",\"endpoint\":\"https://api.example.com/graphql\"}",
    "mapperExpression": "customer.creditScore",
    "isRequired": true
  }'
```

## Testing

### Running API Documentation Tests

```bash
# Run all API documentation tests
mvn test -Dtest=ApiDocumentationIntegrationTest

# Run contract tests
mvn test -Dtest=ExternalServiceContractTest

# Generate documentation
mvn test -Dtest=ApiDocumentationGenerator
```

### Generated Documentation

After running the documentation generator, the following files are created in `target/generated-docs/`:

- `api-documentation.md` - Complete API documentation with examples
- `openapi-spec.json` - OpenAPI specification file
- `postman-collection.json` - Postman collection for API testing

## Best Practices

### 1. API Documentation

- All endpoints have comprehensive OpenAPI annotations
- Request/response examples for common scenarios
- Error responses are documented with appropriate HTTP status codes
- Security requirements are clearly specified

### 2. Testing

- Contract tests validate external service integrations
- Integration tests cover all API endpoints
- Test fixtures provide realistic data for testing
- Performance requirements are validated

### 3. Error Handling

- Standardized error response format
- Appropriate HTTP status codes
- Detailed error messages for debugging
- Validation error details included

## Maintenance

### Adding New Endpoints

1. Add OpenAPI annotations to controller methods
2. Create test fixtures for new data types
3. Add integration tests for new endpoints
4. Update contract tests if external services are involved
5. Regenerate documentation

### Updating Documentation

1. Update OpenAPI annotations as needed
2. Add new examples to test fixtures
3. Run documentation generator to update files
4. Verify Swagger UI displays correctly

## Security Considerations

- JWT tokens are required for all protected endpoints
- Role-based access control is enforced
- API keys and sensitive data are not exposed in documentation
- Rate limiting is implemented to prevent abuse

## Performance

- API documentation is cached for improved performance
- Contract tests validate response times
- Connection pooling is used for external service calls
- Timeout and retry mechanisms are properly configured