# Rules Engine Backend Service

A Spring Boot Java web service that provides comprehensive rule management, execution, and field configuration capabilities.

## Features

- **Rule Management**: Create, organize, and maintain business rules with folder-based organization
- **Field Configuration**: Define available fields, their data sources, and mapping configurations
- **Rule Execution**: Evaluate rules against data contexts with dynamic field resolution
- **JWT Authentication**: Secure API endpoints with JWT-based authentication
- **Health Monitoring**: Built-in health checks and monitoring endpoints
- **Database Integration**: JPA/Hibernate with H2 (development) and PostgreSQL (production) support

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

### Running the Application

1. Clone the repository and navigate to the rules-engine directory
2. Run the application:
   ```bash
   mvn spring-boot:run
   ```
3. The application will start on `http://localhost:8080`

### Default Users

The application creates default users on startup:
- **Admin**: username=`admin`, password=`admin123`
- **User**: username=`user`, password=`user123`

### API Endpoints

#### Authentication
- `POST /api/auth/login` - Authenticate and get JWT token

#### Health Checks
- `GET /api/health` - Application health status
- `GET /api/health/ready` - Readiness probe
- `GET /api/health/live` - Liveness probe

#### Monitoring
- `GET /api/actuator/health` - Detailed health information
- `GET /api/actuator/metrics` - Application metrics
- `GET /api/actuator/prometheus` - Prometheus metrics

### Testing

Run the test suite:
```bash
mvn test
```

### Configuration

#### Development (H2 Database)
The application uses H2 in-memory database by default. Access the H2 console at:
`http://localhost:8080/h2-console`

#### Production (PostgreSQL)
Set the following environment variables:
- `DATABASE_URL`: PostgreSQL connection URL
- `DATABASE_USERNAME`: Database username
- `DATABASE_PASSWORD`: Database password
- `JWT_SECRET`: JWT signing secret

Run with production profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### Security

- JWT tokens expire after 24 hours by default
- CORS is configured for localhost development
- All API endpoints (except auth and health) require authentication
- Role-based access control with USER and ADMIN roles

### Database Schema

The application automatically creates the following tables:
- `users` - User authentication and authorization
- Additional tables will be created as features are implemented

### Monitoring and Observability

- Health checks available at `/api/health/*`
- Metrics exported to Prometheus format
- Structured logging with correlation IDs
- Database connection monitoring

## Development

### Project Structure
```
src/
├── main/java/com/rulesengine/
│   ├── config/          # Configuration classes
│   ├── controller/      # REST controllers
│   ├── dto/            # Data transfer objects
│   ├── entity/         # JPA entities
│   ├── exception/      # Exception handling
│   ├── repository/     # Data repositories
│   ├── security/       # Security components
│   └── service/        # Business logic services
└── test/               # Test classes
```

### Adding New Features

1. Create entity classes in `entity/` package
2. Add repository interfaces in `repository/` package
3. Implement business logic in `service/` package
4. Create REST controllers in `controller/` package
5. Add DTOs in `dto/` package for request/response objects
6. Write tests for all components

This completes the basic project setup and core infrastructure for the Rules Engine.