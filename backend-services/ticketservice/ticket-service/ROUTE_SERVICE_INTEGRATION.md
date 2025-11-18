# Route Service Integration Guide

## Overview
The Ticket Service has been refactored to properly implement microservices architecture by integrating with the Route Service. Instead of duplicating route information, the Ticket Service now fetches route and schedule data from the Route Service via REST API calls.

## Architecture Changes

### Before (Monolithic Data Model)
- Ticket entity stored complete route information
- Fields: `origin`, `destination`, `departureTime (LocalDateTime)`, `arrivalTime (LocalDateTime)`
- **Problem**: Data duplication, tight coupling, potential inconsistency

### After (Microservices Pattern)
- Ticket entity stores references and fetched data
- Fields: `scheduleId`, `routeId`, `routeName`, `departureStop`, `arrivalStop`, `departureTime (String)`, `arrivalTime (String)`, `travelDate (LocalDate)`
- **Benefits**: Single source of truth, loose coupling, better data consistency

## Modified Files

### 1. New DTOs
- **RouteDTO.java**: Represents Route entity from Route Service
  ```java
  - id, routeId, routeName, schedules
  ```

- **ScheduleDTO.java**: Represents Schedule entity from Route Service
  ```java
  - id, routeId, departureStop, arrivalStop, departureTime, arrivalTime
  ```

### 2. New Service Client
- **RouteServiceClient.java**: HTTP client for Route Service communication
  - `getRouteById(Long routeId)`: Fetch route details
  - `getScheduleById(Long scheduleId)`: Fetch schedule details
  - `isRouteAvailable(Long routeId)`: Check route existence
  - `isScheduleAvailable(Long scheduleId)`: Check schedule existence

### 3. Configuration
- **RestTemplateConfig.java**: Provides `RestTemplate` bean for HTTP calls
- **application.properties**: Added `route.service.url` property
  ```properties
  route.service.url=${ROUTE_SERVICE_URL:http://localhost:8081}
  ```

### 4. Updated Entities

#### Ticket.java
**Removed Fields:**
- `origin` (String)
- `destination` (String)
- `departureTime` (LocalDateTime)
- `arrivalTime` (LocalDateTime)

**Added Fields:**
- `scheduleId` (Long) - Reference to Route Service schedule
- `routeName` (String) - Cached from Route Service
- `departureStop` (String) - Cached from Route Service
- `arrivalStop` (String) - Cached from Route Service
- `departureTime` (String) - Time only (e.g., "09:00")
- `arrivalTime` (String) - Time only (e.g., "12:30")
- `travelDate` (LocalDate) - Date of travel

**Rationale:**
- Store schedule reference for foreign key relationship
- Cache route/schedule data for performance (avoid fetching on every read)
- Separate date and time for flexible querying
- Keep `routeId` for backward compatibility and queries

### 5. Updated DTOs

#### TicketRequest.java
**Simplified to:**
```java
- userId (String)
- routeId (Long)
- scheduleId (Long)
- travelDate (LocalDate)
- price (BigDecimal)
- seatNumber (String)
```

**Benefits:**
- Client only needs to provide IDs and travel date
- Route/schedule details fetched automatically from Route Service
- Reduced request payload size

#### TicketResponse.java
**Updated to include:**
- All new Ticket fields (scheduleId, routeName, stops, times, travelDate)
- Maintains backward compatibility for frontend

### 6. Updated Service Layer

#### TicketService.java
**Modified `createTicket()` method:**
1. Validate route exists via Route Service
2. Validate schedule exists via Route Service
3. Validate schedule belongs to the route
4. Check seat availability by `scheduleId` and `travelDate`
5. Fetch route details from Route Service
6. Fetch schedule details from Route Service
7. Build ticket with fetched data
8. Save and publish event

**Key Changes:**
- Added `RouteServiceClient` dependency
- Fetch route/schedule data before creating ticket
- Validate relationships between route and schedule
- Check seat conflicts using `scheduleId` instead of route-level checking

### 7. Updated Repository

#### TicketRepository.java
**Added Methods:**
- `findByScheduleId(Long scheduleId)`: Find all tickets for a schedule

**Modified Methods:**
- `existsBySeatNumberAndScheduleIdAndTravelDate(...)`: Check seat conflicts per schedule and date
- `findUpcomingTicketsByUserId(...)`: Use `travelDate` instead of `departureTime`

### 8. Updated Events

#### TicketCreatedEvent.java
**Added Fields:**
- `scheduleId`, `routeName`, `departureStop`, `arrivalStop`, `travelDate`
- Changed `departureTime`/`arrivalTime` from LocalDateTime to String

#### TicketCancelledEvent.java
**Added Fields:**
- `scheduleId`, `routeName`, `departureStop`, `arrivalStop`, `travelDate`
- Changed `departureTime` from LocalDateTime to String

#### TicketUsedEvent.java
**Added Fields:**
- `scheduleId`, `routeName`, `departureStop`, `arrivalStop`, `travelDate`

**Note:** All event classes now use `@Builder.Default` for `eventType` field to fix Lombok warning

### 9. Updated Mapper

#### TicketMapper.java
- **Removed**: `toEntity()` method (no longer needed - tickets built in service with fetched data)
- **Updated**: `toResponse()` method to include all new fields

## Integration Flow

### Ticket Creation Flow
```
1. Client sends TicketRequest (userId, routeId, scheduleId, travelDate, price, seatNumber)
   ↓
2. TicketService validates route exists via RouteServiceClient
   ↓
3. TicketService validates schedule exists via RouteServiceClient
   ↓
4. TicketService validates schedule belongs to route
   ↓
5. TicketService checks seat availability (scheduleId + travelDate + seatNumber)
   ↓
6. TicketService fetches RouteDTO from Route Service
   ↓
7. TicketService fetches ScheduleDTO from Route Service
   ↓
8. TicketService builds Ticket entity with fetched data:
   - routeId, scheduleId from request
   - routeName from RouteDTO
   - departureStop, arrivalStop, departureTime, arrivalTime from ScheduleDTO
   - travelDate from request
   ↓
9. TicketService saves Ticket to database
   ↓
10. TicketService publishes TicketCreatedEvent to RabbitMQ
   ↓
11. Response with TicketResponse DTO
```

## Configuration

### Environment Variables
```bash
# Route Service URL (default: http://localhost:8081)
ROUTE_SERVICE_URL=http://localhost:8081

# Database Configuration
DB_HOST=localhost
DB_PORT=5433

# RabbitMQ Configuration
RABBITMQ_HOST=localhost
```

### application.properties
```properties
route.service.url=${ROUTE_SERVICE_URL:http://localhost:8081}
```

## Error Handling

### RouteServiceClient
- Returns `null` if route/schedule not found
- Logs errors but doesn't throw exceptions
- Allows service layer to handle validation

### TicketService
- Throws `ResourceNotFoundException` if route doesn't exist
- Throws `ResourceNotFoundException` if schedule doesn't exist
- Throws `IllegalStateException` if schedule doesn't belong to route
- Throws `IllegalStateException` if seat already booked

## Benefits of This Architecture

### 1. Single Source of Truth
- Route Service owns all route/schedule data
- No data duplication across services
- Changes to routes propagate automatically

### 2. Loose Coupling
- Services communicate via REST API
- Can deploy and scale independently
- Can evolve schemas without breaking other services

### 3. Better Performance
- Route data cached in tickets (no fetch on every read)
- Only fetch during ticket creation
- Reduces inter-service calls

### 4. Flexibility
- Can switch Route Service implementation
- Can add caching layer (Redis) for frequently accessed routes
- Can implement circuit breakers for resilience

### 5. Proper Microservices Pattern
- Each service has clear responsibility
- Route Service: Manages routes and schedules
- Ticket Service: Manages ticket bookings

## Testing Checklist

### Unit Tests
- [ ] RouteServiceClient - mock RestTemplate
- [ ] TicketService.createTicket() - mock RouteServiceClient
- [ ] Ticket validation logic
- [ ] Event publishing with new structure

### Integration Tests
- [ ] End-to-end ticket creation with real Route Service
- [ ] Handle Route Service unavailability
- [ ] Seat conflict detection with scheduleId
- [ ] Event publishing to RabbitMQ

### Manual Tests
- [ ] Create ticket with valid route and schedule
- [ ] Create ticket with invalid routeId (should fail)
- [ ] Create ticket with invalid scheduleId (should fail)
- [ ] Create ticket with schedule not belonging to route (should fail)
- [ ] Book same seat on same schedule and date (should fail)
- [ ] Book same seat on different date (should succeed)
- [ ] Verify RabbitMQ events contain new fields

## Migration Notes

### Database Schema Changes
- Run when `spring.jpa.hibernate.ddl-auto=update`:
  - Adds new columns: `schedule_id`, `route_name`, `departure_stop`, `arrival_stop`, `travel_date`
  - Modifies columns: `departure_time`, `arrival_time` (LocalDateTime → String)
  - Removes columns: `origin`, `destination`

### Data Migration (if needed)
```sql
-- Example migration script (adjust as needed)
-- 1. Add new columns
ALTER TABLE tickets ADD COLUMN schedule_id BIGINT;
ALTER TABLE tickets ADD COLUMN route_name VARCHAR(255);
ALTER TABLE tickets ADD COLUMN departure_stop VARCHAR(255);
ALTER TABLE tickets ADD COLUMN arrival_stop VARCHAR(255);
ALTER TABLE tickets ADD COLUMN travel_date DATE;

-- 2. Migrate data (depends on your data structure)
-- This is just an example - adjust based on your needs
UPDATE tickets SET 
    travel_date = DATE(departure_time),
    departure_stop = origin,
    arrival_stop = destination;

-- 3. Drop old columns (after verifying data)
ALTER TABLE tickets DROP COLUMN origin;
ALTER TABLE tickets DROP COLUMN destination;
```

## Future Enhancements

### 1. Caching
```java
@Cacheable("routes")
public RouteDTO getRouteById(Long routeId) {
    // Cached route fetching
}
```

### 2. Circuit Breaker (Resilience4j)
```java
@CircuitBreaker(name = "routeService", fallbackMethod = "getRouteFallback")
public RouteDTO getRouteById(Long routeId) {
    // Resilient route fetching
}
```

### 3. Asynchronous Communication
- Use RabbitMQ events instead of REST calls for non-critical data
- Route Service publishes RouteUpdatedEvent
- Ticket Service updates cached route names

### 4. Service Discovery (Eureka)
- Dynamic service URL resolution
- Load balancing across multiple Route Service instances

### 5. API Gateway
- Centralized routing
- Authentication/authorization
- Rate limiting

## Troubleshooting

### Issue: "Connection refused" when calling Route Service
**Solution:** Ensure Route Service is running on `http://localhost:8081`

### Issue: Ticket creation fails with "Route not found"
**Solution:** Verify routeId exists in Route Service database

### Issue: Seat booking validation not working
**Solution:** Check `scheduleId` and `travelDate` are correct in request

### Issue: Old departure_time column still exists
**Solution:** Set `spring.jpa.hibernate.ddl-auto=update` and restart service

## Summary

The Ticket Service now properly implements microservices architecture by:
- **Fetching** route/schedule data from Route Service (not duplicating)
- **Caching** frequently accessed data in ticket entity
- **Validating** relationships between routes and schedules
- **Publishing** enriched events with complete ticket information

This architecture ensures data consistency, loose coupling, and proper service boundaries while maintaining performance through strategic caching.
