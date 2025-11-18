# Route Service Architecture

## Overview
The Route Service is a microservice responsible for managing bus routes and schedules in the UrbanFlow system. It provides RESTful APIs for route planning, schedule management, and inter-service communication with the Ticket Service.

## Technology Stack
- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17+
- **Database**: PostgreSQL
- **ORM**: Spring Data JPA / Hibernate
- **Documentation**: SpringDoc OpenAPI (Swagger)
- **Build Tool**: Maven

## Architecture & Design Patterns

### 1. Layered Architecture
```
┌─────────────────────────────────────────┐
│         Controller Layer                │  ← REST endpoints, request validation
│  (RouteController)                      │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│         Service Layer                   │  ← Business logic, orchestration
│  (RouteService)                         │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│         Repository Layer                │  ← Data access
│  (RouteRepository, ScheduleRepository)  │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│         Database (PostgreSQL)           │
└─────────────────────────────────────────┘
```

### 2. Design Patterns Implemented

#### a) **Service Layer Pattern**
- Separates business logic from controller
- `RouteService` handles all business operations
- Controllers are thin, focusing only on HTTP concerns

#### b) **Data Transfer Object (DTO) Pattern**
- `RouteDTO`, `ScheduleDTO`: Response objects
- `RoutePlanRequest`, `RoutePlanResponse`: Request/Response for planning
- Benefits:
  - Decouples internal entities from API contracts
  - Controls what data is exposed
  - Enables versioning without changing entities

#### c) **Repository Pattern**
- Spring Data JPA repositories
- `RouteRepository`, `ScheduleRepository`
- Abstract database operations
- Type-safe queries

#### d) **Mapper Pattern**
- `RouteMapper`: Converts entities to DTOs
- Centralized conversion logic
- Easy to test and maintain

#### e) **Exception Handling Pattern**
- `GlobalExceptionHandler`: Centralized error handling
- Custom exception: `ResourceNotFoundException`
- Consistent error responses across all endpoints

#### f) **Builder Pattern**
- Lombok `@Builder` on entities and DTOs
- Fluent, readable object construction
- Immutability support

#### g) **Dependency Injection**
- Constructor injection via `@RequiredArgsConstructor`
- Promotes testability
- Clear dependencies

## Domain Model

### Entity Relationship
```
┌──────────────────┐         1:N         ┌──────────────────┐
│      Route       │◄───────────────────►│    Schedule      │
├──────────────────┤                     ├──────────────────┤
│ id (PK)          │                     │ id (PK)          │
│ routeId (UK)     │                     │ routeId          │
│ routeName        │                     │ departureStop    │
└──────────────────┘                     │ arrivalStop      │
                                         │ departureTime    │
                                         │ arrivalTime      │
                                         │ route_db_id (FK) │
                                         └──────────────────┘
```

### Route Entity
- Represents a bus route (e.g., "R10: Maarif - Ain Sebaa")
- Has many schedules
- Unique `routeId` constraint

### Schedule Entity
- Represents a specific route segment with times
- Belongs to one route
- Stores denormalized `routeId` for quick access
- Bidirectional relationship with Route

## API Endpoints

### Route Management
```
GET    /api/routes                      - Get all routes with schedules
GET    /api/routes/{id}                 - Get route by ID (Ticket Service)
GET    /api/routes/route-id/{routeId}   - Get route by routeId (e.g., R10)
GET    /api/routes/{id}/available       - Check if route exists (Ticket Service)
```

### Schedule Management
```
GET    /api/routes/schedules/{id}                - Get schedule by ID (Ticket Service)
GET    /api/routes/route-id/{routeId}/schedules  - Get schedules for a route
GET    /api/routes/schedules/{id}/available      - Check if schedule exists (Ticket Service)
```

### Stop Management
```
GET    /api/routes/stops                 - Get all unique stops
GET    /api/routes/stops/search?query=x  - Search stops (autocomplete)
```

### Route Planning
```
GET    /api/routes/plan?from=X&to=Y     - Plan route (query params)
POST   /api/routes/plan                 - Plan route (request body)
```

## Integration with Ticket Service

### Communication Pattern
- **Synchronous REST API calls**
- Ticket Service acts as client
- Route Service acts as server
- Uses `RestTemplate` for HTTP communication

### Key Integration Points

#### 1. Route Validation
```java
// Ticket Service calls:
GET /api/routes/{id}
GET /api/routes/{id}/available
```

#### 2. Schedule Validation
```java
// Ticket Service calls:
GET /api/routes/schedules/{id}
GET /api/routes/schedules/{id}/available
```

#### 3. Data Fetching
```java
// Ticket Service fetches route and schedule details during ticket creation
RouteDTO route = restTemplate.getForObject("http://localhost:8081/api/routes/{id}", RouteDTO.class);
ScheduleDTO schedule = restTemplate.getForObject("http://localhost:8081/api/routes/schedules/{id}", ScheduleDTO.class);
```

### Error Handling
- Returns 404 if route/schedule not found
- Returns 400 for validation errors
- Returns 503 if external service unavailable
- Consistent JSON error responses

## Business Logic

### Route Planning Algorithm
1. User provides: departure stop, arrival stop
2. System searches all schedules for direct match
3. If found: Returns route details + available buses
4. If not found: Throws `ResourceNotFoundException`

### Duration Calculation
- Parses `departureTime` and `arrivalTime` (HH:mm format)
- Calculates difference in minutes
- Handles edge cases (next-day arrivals)

### Graceful Degradation
- Geolocation service integration is optional
- Returns empty list if geolocation service unavailable
- Doesn't fail the main request

## Data Initialization

### Sample Data (DataInitializer)
- **R10**: Maarif - Ain Sebaa (multiple schedules, both directions)
- **R20**: Derb Sultan - Place Mohammed V (multiple schedules)
- **R30**: Casablanca Central - Airport (hourly schedules)

### Schedule Structure
- **Segments**: Individual stop-to-stop connections
- **Direct routes**: Full journey without intermediate stops
- **Bidirectional**: Forward and return routes

## Configuration

### application.yaml
```yaml
server:
  port: 8081  # Matches Ticket Service expectation

spring:
  application:
    name: route-service
  datasource:
    url: jdbc:postgresql://localhost:5432/route_db
    username: postgres
    password: Halima1975
  jpa:
    hibernate:
      ddl-auto: update  # Auto-create/update schema
    show-sql: true      # Log SQL queries
```

### RestTemplate Configuration
- Connection timeout: 5 seconds
- Read timeout: 5 seconds
- Prevents hanging requests

## API Documentation

### Swagger UI
Access at: `http://localhost:8081/swagger-ui.html`

Features:
- Interactive API documentation
- Try-it-out functionality
- Request/response examples
- Schema definitions

### OpenAPI JSON
Access at: `http://localhost:8081/v3/api-docs`

## Error Responses

### Standard Error Format
```json
{
  "timestamp": "2025-11-17T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Route not found with id: 123"
}
```

### Validation Error Format
```json
{
  "timestamp": "2025-11-17T10:30:00",
  "status": 400,
  "error": "Validation Failed",
  "errors": {
    "from": "Departure stop is required",
    "to": "Arrival stop is required"
  }
}
```

## Logging

### Log Levels
- **INFO**: Request/response logging
- **DEBUG**: RestTemplate calls
- **WARN**: External service failures
- **ERROR**: Internal errors

### Example Logs
```
INFO  - GET /api/routes/1 - Fetching route by ID
DEBUG - Fetching buses from geolocation service: http://localhost:8084/api/location/route/R10
WARN  - Geolocation service unavailable for route R10: Connection refused
```

## Testing Strategy

### Unit Tests
- Service layer methods
- Mapper conversions
- Business logic (duration calculation, search)

### Integration Tests
- Controller endpoints
- Database operations
- Exception handling

### Contract Tests
- API contracts for Ticket Service integration
- Ensures backward compatibility

## Performance Considerations

### Database Optimization
- Indexed `routeId` columns
- Lazy loading for relationships
- Connection pooling (HikariCP)

### Caching Strategy (Future)
- Cache frequently accessed routes
- Cache stop lists
- Time-based invalidation

### N+1 Query Prevention
- Fetch schedules with routes in single query
- Use DTOs to control what's loaded

## Security Considerations

### CORS Configuration
- Currently allows all origins (`*`)
- **Production**: Restrict to known domains

### Input Validation
- `@Valid` annotation on request bodies
- Bean validation constraints
- SQL injection prevention (parameterized queries)

### Future Enhancements
- OAuth2/JWT authentication
- Rate limiting
- API key management

## Deployment

### Database Setup
```sql
CREATE DATABASE route_db;
-- Schema auto-created by Hibernate
```

### Build & Run
```bash
# Build
mvn clean package

# Run
java -jar target/route-service-1.0.0.jar

# Or with Maven
./mvnw spring-boot:run
```

### Docker Support (Future)
```dockerfile
FROM openjdk:17-slim
COPY target/route-service-1.0.0.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## Monitoring & Health Checks

### Spring Actuator (Future Enhancement)
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

Endpoints:
- `/actuator/health` - Service health
- `/actuator/info` - Service information
- `/actuator/metrics` - Performance metrics

## Future Enhancements

### 1. Multi-leg Journey Planning
- Find routes with transfers
- Graph-based pathfinding algorithm
- Optimize for time or transfers

### 2. Real-time Updates
- WebSocket for live schedule updates
- Integration with real-time tracking
- Delay notifications

### 3. Historical Data
- Archive old schedules
- Performance analytics
- Usage patterns

### 4. Service Discovery
- Eureka integration
- Dynamic service URLs
- Load balancing

### 5. Circuit Breaker
- Resilience4j integration
- Fallback mechanisms
- Bulkhead pattern

### 6. Event-Driven Architecture
- Publish RouteUpdatedEvent to RabbitMQ
- Ticket Service subscribes to updates
- Async communication for non-critical operations

## Best Practices Followed

✅ **Separation of Concerns**: Layered architecture  
✅ **Single Responsibility**: Each class has one job  
✅ **Dependency Inversion**: Depend on abstractions  
✅ **Open/Closed Principle**: Open for extension  
✅ **DRY**: No code duplication  
✅ **KISS**: Simple, understandable code  
✅ **Fail Fast**: Early validation  
✅ **Graceful Degradation**: Optional external services  
✅ **Comprehensive Logging**: Troubleshooting support  
✅ **API Documentation**: Self-documenting APIs  

## Summary

The Route Service is a well-architected microservice that:
- Manages route and schedule data
- Provides clean REST APIs
- Integrates seamlessly with Ticket Service
- Follows industry best practices
- Uses proven design patterns
- Is maintainable and extensible

It serves as the **single source of truth** for all route information in the UrbanFlow ecosystem.
