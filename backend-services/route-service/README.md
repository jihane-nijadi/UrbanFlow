# Route Service 🚌

A production-ready microservice for managing bus routes and schedules in the UrbanFlow urban transportation system.

## 🎯 Overview

The Route Service is responsible for:
- Managing bus routes and schedules
- Providing route planning functionality
- Supporting stop search and autocomplete
- Integrating with Ticket Service for bookings
- Optionally integrating with Geolocation Service for real-time bus tracking

## 🏗️ Architecture

### Technology Stack
- **Java 17+**
- **Spring Boot 3.2.0**
- **PostgreSQL** (Database)
- **Spring Data JPA** (ORM)
- **Maven** (Build tool)
- **SpringDoc OpenAPI** (API documentation)
- **Lombok** (Boilerplate reduction)

### Design Patterns
- ✅ Layered Architecture (Controller → Service → Repository)
- ✅ Service Layer Pattern
- ✅ DTO Pattern
- ✅ Repository Pattern
- ✅ Mapper Pattern
- ✅ Exception Handling Pattern
- ✅ Builder Pattern
- ✅ Dependency Injection
- ✅ Facade Pattern
- ✅ Graceful Degradation

## 📁 Project Structure

```
route-service/
├── src/main/java/com/bus/route/
│   ├── RouteServiceApplication.java     # Main application
│   ├── config/
│   │   ├── DataInitializer.java         # Sample data loader
│   │   └── RestTemplateConfig.java      # HTTP client config
│   ├── controller/
│   │   └── RouteController.java         # REST endpoints
│   ├── service/
│   │   └── RouteService.java            # Business logic
│   ├── repository/
│   │   ├── RouteRepository.java         # Route data access
│   │   └── ScheduleRepository.java      # Schedule data access
│   ├── entity/
│   │   ├── Route.java                   # Route entity
│   │   └── Schedule.java                # Schedule entity
│   ├── dto/
│   │   ├── RouteDTO.java                # Route response
│   │   ├── ScheduleDTO.java             # Schedule response
│   │   ├── RoutePlanRequest.java        # Planning request
│   │   └── RoutePlanResponse.java       # Planning response
│   ├── mapper/
│   │   └── RouteMapper.java             # Entity ↔ DTO mapper
│   └── exception/
│       ├── ResourceNotFoundException.java
│       └── GlobalExceptionHandler.java  # Centralized errors
├── src/main/resources/
│   └── application.yaml                 # Configuration
├── ROUTE_SERVICE_ARCHITECTURE.md        # Architecture docs
├── REFACTORING_SUMMARY.md               # Refactoring details
├── pom.xml                              # Maven dependencies
└── README.md                            # This file
```

## 🚀 Quick Start

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+

### 1. Database Setup
```sql
CREATE DATABASE route_db;
```

### 2. Configuration
Update `src/main/resources/application.yaml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/route_db
    username: your_username
    password: your_password
```

### 3. Build
```bash
mvn clean package
```

### 4. Run
```bash
# Option 1: Using Maven
./mvnw spring-boot:run

# Option 2: Using JAR
java -jar target/route-service-1.0.0.jar
```

### 5. Verify
```bash
# Check health
curl http://localhost:8081/api/routes

# View Swagger UI
open http://localhost:8081/swagger-ui.html
```

## 📡 API Endpoints

### Route Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/routes` | Get all routes with schedules |
| GET | `/api/routes/{id}` | Get route by ID |
| GET | `/api/routes/route-id/{routeId}` | Get route by routeId (e.g., R10) |
| GET | `/api/routes/{id}/available` | Check if route exists |

### Schedule Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/routes/schedules/{id}` | Get schedule by ID |
| GET | `/api/routes/route-id/{routeId}/schedules` | Get schedules for route |
| GET | `/api/routes/schedules/{id}/available` | Check if schedule exists |

### Stop Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/routes/stops` | Get all unique stops |
| GET | `/api/routes/stops/search?query=x` | Search stops (autocomplete) |

### Route Planning
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/routes/plan?from=X&to=Y` | Plan route (query params) |
| POST | `/api/routes/plan` | Plan route (request body) |

## 📝 API Examples

### Get All Routes
```bash
curl http://localhost:8081/api/routes
```

Response:
```json
[
  {
    "id": 1,
    "routeId": "R10",
    "routeName": "Maarif - Ain Sebaa",
    "schedules": [
      {
        "id": 1,
        "routeId": "R10",
        "departureStop": "Maarif",
        "arrivalStop": "Zerktouni",
        "departureTime": "08:00",
        "arrivalTime": "08:10"
      }
    ]
  }
]
```

### Get Route by ID
```bash
curl http://localhost:8081/api/routes/1
```

### Check Route Availability
```bash
curl http://localhost:8081/api/routes/1/available
```

Response: `true` or `false`

### Get Schedule by ID
```bash
curl http://localhost:8081/api/routes/schedules/1
```

### Plan Route
```bash
curl "http://localhost:8081/api/routes/plan?from=Maarif&to=Casa%20Port"
```

Response:
```json
{
  "routeId": "R10",
  "routeName": "Maarif - Ain Sebaa",
  "departureStop": "Maarif",
  "arrivalStop": "Casa Port",
  "departureTime": "08:00",
  "arrivalTime": "08:25",
  "estimatedDurationMinutes": 25,
  "busesAvailable": []
}
```

### Search Stops
```bash
curl "http://localhost:8081/api/routes/stops/search?query=maa"
```

Response:
```json
["Maarif"]
```

## 🗄️ Database Schema

### Routes Table
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, Auto-increment |
| route_id | VARCHAR(255) | UNIQUE, NOT NULL |
| route_name | VARCHAR(255) | NOT NULL |

### Schedules Table
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, Auto-increment |
| route_id | VARCHAR(255) | NOT NULL |
| departure_stop | VARCHAR(255) | NOT NULL |
| arrival_stop | VARCHAR(255) | NOT NULL |
| departure_time | VARCHAR(255) | NOT NULL |
| arrival_time | VARCHAR(255) | NOT NULL |
| route_db_id | BIGINT | FK → routes(id) |

### Relationships
- One Route has many Schedules (1:N)
- One Schedule belongs to one Route (N:1)

## 🧪 Sample Data

The service initializes with sample data:

### Routes
- **R10**: Maarif - Ain Sebaa
- **R20**: Derb Sultan - Place Mohammed V
- **R30**: Casablanca Central - Airport

### Schedules
Each route has multiple schedules for different times and directions.

Example for R10:
- Maarif → Zerktouni (08:00 - 08:10)
- Zerktouni → Casa Port (08:10 - 08:25)
- Casa Port → Ain Sebaa (08:25 - 08:45)
- Full route: Maarif → Ain Sebaa (08:00 - 08:45)
- Return routes available

## 🔗 Integration with Ticket Service

### How Ticket Service Uses Route Service

1. **Route Validation**
```java
// Check if route exists
boolean exists = restTemplate.getForObject(
    "http://localhost:8081/api/routes/1/available", 
    Boolean.class
);
```

2. **Fetch Route Details**
```java
// Get route information
RouteDTO route = restTemplate.getForObject(
    "http://localhost:8081/api/routes/1", 
    RouteDTO.class
);
```

3. **Schedule Validation**
```java
// Check if schedule exists
boolean exists = restTemplate.getForObject(
    "http://localhost:8081/api/routes/schedules/1/available", 
    Boolean.class
);
```

4. **Fetch Schedule Details**
```java
// Get schedule information
ScheduleDTO schedule = restTemplate.getForObject(
    "http://localhost:8081/api/routes/schedules/1", 
    ScheduleDTO.class
);
```

### Perfect Compatibility
The Route Service provides **exactly** the endpoints and response formats that Ticket Service expects.

## 📊 Error Handling

### Standard Error Response
```json
{
  "timestamp": "2025-11-17T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Route not found with id: 123"
}
```

### HTTP Status Codes
- **200** - Success
- **400** - Bad Request (validation failed)
- **404** - Not Found (resource doesn't exist)
- **500** - Internal Server Error
- **503** - Service Unavailable (external service down)

## 📖 API Documentation

### Swagger UI
Interactive API documentation with try-it-out functionality:
```
http://localhost:8081/swagger-ui.html
```

### OpenAPI JSON
Machine-readable API specification:
```
http://localhost:8081/v3/api-docs
```

## 🛠️ Configuration

### Application Properties
```yaml
server:
  port: 8081

spring:
  application:
    name: route-service
  datasource:
    url: jdbc:postgresql://localhost:5432/route_db
    username: postgres
    password: your_password
  jpa:
    hibernate:
      ddl-auto: update  # Auto-create schema
    show-sql: true      # Log SQL queries
```

### Environment Variables
```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=route_db
export DB_USERNAME=postgres
export DB_PASSWORD=your_password
```

## 🧪 Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify
```

### Manual API Tests
```bash
# Test all endpoints
./test-api.sh
```

## 📦 Deployment

### Docker (Future)
```dockerfile
FROM openjdk:17-slim
COPY target/route-service-1.0.0.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Docker Compose
```yaml
services:
  route-service:
    build: .
    ports:
      - "8081:8081"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/route_db
    depends_on:
      - db
  db:
    image: postgres:15
    environment:
      - POSTGRES_DB=route_db
      - POSTGRES_PASSWORD=password
```

## 🔍 Monitoring

### Logging
- Request/response logging at INFO level
- External service calls at DEBUG level
- Errors at ERROR level

### Future: Spring Actuator
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

Endpoints:
- `/actuator/health` - Service health
- `/actuator/info` - Service info
- `/actuator/metrics` - Performance metrics

## 🚧 Future Enhancements

- [ ] Multi-leg journey planning (with transfers)
- [ ] Real-time schedule updates via WebSocket
- [ ] Caching for frequently accessed routes
- [ ] Service discovery (Eureka)
- [ ] Circuit breaker (Resilience4j)
- [ ] Event-driven updates (RabbitMQ)
- [ ] Rate limiting
- [ ] OAuth2/JWT authentication

## 📚 Documentation

- **[Architecture Guide](ROUTE_SERVICE_ARCHITECTURE.md)** - Complete architecture documentation
- **[Refactoring Summary](REFACTORING_SUMMARY.md)** - Details of what was changed
- **[Swagger UI](http://localhost:8081/swagger-ui.html)** - Interactive API docs

## 🤝 Contributing

1. Follow the existing code structure
2. Add unit tests for new features
3. Update documentation
4. Follow Spring Boot best practices
5. Use Lombok annotations

## 📄 License

Part of the UrbanFlow project.

## 👥 Support

For questions or issues:
- Check the architecture documentation
- Review Swagger UI for API details
- Check logs for error details

---

**Built with ❤️ using Spring Boot and best practices** 🚀
