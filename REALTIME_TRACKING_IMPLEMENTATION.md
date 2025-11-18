# Real-Time Bus Tracking Implementation

## Overview

This document describes the implementation of real-time bus tracking functionality for the UrbanFlow transport management system.

## Architecture

### Backend (Spring Boot)
- **Service**: `geolocation-service` (Port 8091)
- **Real-time Communication**: Server-Sent Events (SSE)
- **Data Storage**: In-memory store (can be extended to database)
- **External Integration**: Polls external APIs for bus positions

### Frontend (React)
- **Component**: `BusTracker` - Interactive map with real-time updates
- **Map Library**: React-Leaflet with OpenStreetMap tiles
- **Integration**: Integrated into PassengerDashboard

## Features Implemented

### Backend Features
1. **REST API Endpoints**:
   - `GET /api/buses` - Get all active buses
   - `GET /api/buses/{busId}` - Get specific bus details
   - `GET /api/buses/{busId}/positions` - Get bus position history
   - `POST /api/telemetry` - Ingest bus location updates
   - `GET /api/realtime/stream` - SSE endpoint for real-time updates

2. **Real-time Broadcasting**:
   - Server-Sent Events (SSE) for push updates
   - Automatic client connection management
   - Broadcasts updates to all connected clients

3. **CORS Configuration**:
   - Configured to allow frontend connections
   - Supports SSE connections

### Frontend Features
1. **Interactive Map**:
   - OpenStreetMap integration
   - Custom bus markers with rotation based on heading
   - Click markers to see bus details
   - Auto-fit map to show all active buses

2. **Real-time Updates**:
   - Connects to SSE endpoint
   - Automatically updates bus positions
   - Shows active bus count with live indicator

3. **Bus Information**:
   - Bus ID and Line ID
   - Current speed (km/h)
   - Heading/direction
   - Last update timestamp
   - List view of all active buses

## File Structure

### Backend Files
```
backend-services/geolocation-service/
├── src/main/java/com/uf/geolocationservice/
│   ├── controller/
│   │   └── BusController.java          # REST endpoints
│   ├── model/
│   │   ├── BusState.java               # Bus state model
│   │   └── BusUpdate.java              # Bus update DTO
│   ├── service/
│   │   ├── InMemoryBusStore.java       # Bus data storage
│   │   ├── RealtimeBroadcaster.java    # SSE broadcasting
│   │   └── ExternalApiPoller.java      # External API polling
│   └── config/
│       ├── AppConfig.java              # CORS & scheduling config
│       └── SecurityConfig.java         # Security settings
└── test-bus-updates.py                 # Test script
```

### Frontend Files
```
transport-frontend/
├── src/
│   ├── api/
│   │   └── geolocationService.js       # API service for backend
│   ├── components/
│   │   └── BusTracker.jsx              # Main tracking component
│   └── pages/
│       └── PassengerDashboard.jsx      # Updated with BusTracker
└── package.json                        # Added react-leaflet, leaflet
```

## Usage

### Starting the Services

1. **Start Backend**:
   ```bash
   cd backend-services/geolocation-service
   mvn spring-boot:run
   ```

2. **Start Frontend**:
   ```bash
   cd transport-frontend
   npm start
   ```

3. **Send Test Data**:
   ```bash
   # Using Python script (recommended)
   cd backend-services/geolocation-service
   python3 test-bus-updates.py
   
   # Or using curl
   curl -X POST http://localhost:8091/api/telemetry \
     -H "Content-Type: application/json" \
     -d '{
       "busId": "BUS-001",
       "lineId": "LINE-1",
       "lat": 33.5731,
       "lon": -7.5898,
       "speed": 15.5,
       "heading": 45,
       "ts": 1704067200000
     }'
   ```

### Accessing the Feature

1. Log in to the frontend as a passenger
2. Navigate to the "Bus disponibles" tab
3. View the map with real-time bus positions
4. Click on bus markers to see details

## Data Flow

1. **Bus Updates**:
   - External API or telemetry endpoint sends bus location updates
   - Updates are stored in `InMemoryBusStore`
   - Updates are broadcasted via SSE to all connected clients

2. **Frontend Updates**:
   - Frontend connects to SSE endpoint on component mount
   - Receives real-time updates and updates map markers
   - Automatically handles reconnection on errors

## Configuration

### Backend Configuration
Edit `application.properties`:
```properties
server.port=8091
external.api.url=https://your-api-url
external.api.poll-interval-ms=3000
external.api.api-key=YOUR_API_KEY
```

### Frontend Configuration
Edit `src/api/geolocationService.js` to change API URL:
```javascript
const GEOLOCATION_API_URL = "http://localhost:8091/api";
```

## Future Enhancements

1. **Database Persistence**: Replace in-memory store with PostgreSQL
2. **Route Visualization**: Show bus routes on the map
3. **Predictions**: ETA calculations based on current position
4. **Filters**: Filter buses by line, route, or area
5. **History Playback**: Replay bus movements over time
6. **Notifications**: Alert users when their bus is nearby
7. **Mobile Optimization**: Better mobile experience

## Troubleshooting

### Buses not showing on map
- Verify backend is running on port 8091
- Check browser console for errors
- Ensure at least one bus update has been sent

### SSE connection issues
- Check CORS configuration
- Verify network connectivity
- Check browser console for connection errors

### Map not loading
- Ensure internet connection (map tiles from CDN)
- Check Leaflet CSS is imported
- Verify react-leaflet is installed

## Testing

See `backend-services/geolocation-service/TESTING.md` for detailed testing instructions.




