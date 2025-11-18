# Testing Real-Time Bus Tracking

## Prerequisites

1. Start the geolocation service:
   ```bash
   cd backend-services/geolocation-service
   mvn spring-boot:run
   ```

2. Start the frontend:
   ```bash
   cd transport-frontend
   npm start
   ```

## Testing with Sample Data

### Option 1: Using curl (Manual)

Send a single bus update:
```bash
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

### Option 2: Using the Test Script

1. Make the script executable:
   ```bash
   chmod +x test-bus-updates.sh
   ```

2. Run the script (requires `bc` for calculations):
   ```bash
   ./test-bus-updates.sh
   ```

   Or install `bc` if not available:
   ```bash
   # Ubuntu/Debian
   sudo apt-get install bc
   
   # macOS
   brew install bc
   ```

### Option 3: Using JavaScript/Node.js

Create a simple Node.js script to send updates:

```javascript
const fetch = require('node-fetch');

const API_URL = 'http://localhost:8091/api/telemetry';

async function sendUpdate(busId, lineId, lat, lon, speed, heading) {
  const response = await fetch(API_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      busId,
      lineId,
      lat,
      lon,
      speed,
      heading,
      ts: Date.now()
    })
  });
  console.log(`Sent update for ${busId}: ${response.status}`);
}

// Simulate bus movement
let lat = 33.5731;
let lon = -7.5898;

setInterval(() => {
  lat += 0.0001;
  lon += 0.0001;
  sendUpdate('BUS-001', 'LINE-1', lat, lon, 15.5, 45);
}, 3000);
```

## API Endpoints

### Get All Active Buses
```bash
curl http://localhost:8091/api/buses
```

### Get Specific Bus
```bash
curl http://localhost:8091/api/buses/BUS-001
```

### Get Bus Position History
```bash
curl http://localhost:8091/api/buses/BUS-001/positions?limit=20
```

### Subscribe to Real-Time Updates (SSE)
Open in browser or use a tool that supports SSE:
```
http://localhost:8091/api/realtime/stream
```

## Frontend Testing

1. Log in as a passenger
2. Navigate to "Bus disponibles" tab
3. You should see the map with bus markers
4. Send test updates using one of the methods above
5. Watch the buses move on the map in real-time

## Troubleshooting

### No buses showing on map
- Check that the backend is running on port 8091
- Verify CORS is enabled (should be configured in AppConfig.java)
- Check browser console for errors
- Ensure you've sent at least one bus update

### SSE connection not working
- Check browser console for connection errors
- Verify the backend endpoint `/api/realtime/stream` is accessible
- Check network tab in browser dev tools for SSE events

### Map not loading
- Ensure Leaflet CSS is imported
- Check that OpenStreetMap tiles are accessible
- Verify internet connection (map tiles are loaded from external CDN)




