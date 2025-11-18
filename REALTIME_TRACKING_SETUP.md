# Real-Time Bus Tracking - Complete Setup Guide

## ✅ What's Been Implemented

### Backend (Geolocation Service)
- ✅ **External API Integration** - Supports multiple transit API formats
- ✅ **Mock Data Generator** - Generates realistic bus movements for testing
- ✅ **Real-time Broadcasting** - Server-Sent Events (SSE) for live updates
- ✅ **Flexible Parser** - Automatically detects and parses different API formats

### Frontend (React)
- ✅ **Interactive Map** - Leaflet map with OpenStreetMap tiles
- ✅ **Real-time Updates** - Connects to SSE endpoint for live bus positions
- ✅ **Bus Markers** - Custom rotating bus icons showing direction
- ✅ **Bus Information** - Click markers to see bus details

## 🚀 Quick Start (Using Mock Data)

### Step 1: Start Backend
```bash
cd backend-services/geolocation-service
mvn spring-boot:run
```

The service will:
- Start on port 8091
- Generate mock bus data automatically (5 buses moving in Casablanca area)
- Broadcast updates every 5 seconds

### Step 2: Start Frontend
```bash
cd transport-frontend
npm start
```

### Step 3: View Tracking
1. Open browser to http://localhost:3000
2. Log in as a passenger (or skip auth if configured)
3. Click "Bus disponibles" in the sidebar
4. See 5 buses moving on the map in real-time!

## 🔌 Using Real External API

### Option 1: Transitland API (Free)

1. **Sign up** at https://www.transit.land/ (free tier available)
2. **Get your API key**
3. **Find a feed ID** for your city (e.g., San Francisco: `F-9q8y-sfmta`)
4. **Update `application.properties`**:
```properties
external.api.url=https://transit.land/api/v2/rest/vehicle_positions?feed_onestop_id=F-9q8y-sfmta
external.api.api-key=YOUR_API_KEY_HERE
external.api.enable-mock=false
external.api.poll-interval-ms=5000
```

### Option 2: Custom API

Create an API endpoint that returns JSON in this format:
```json
[
  {
    "busId": "BUS-001",
    "lineId": "LINE-1",
    "lat": 33.5731,
    "lon": -7.5898,
    "speed": 25.5,
    "heading": 45,
    "ts": 1704067200000
  }
]
```

Then configure:
```properties
external.api.url=https://your-api.com/buses
external.api.api-key=YOUR_KEY_IF_NEEDED
external.api.enable-mock=false
```

### Option 3: City-Specific APIs

Many cities provide transit APIs. Examples:
- **NYC MTA**: https://api.mta.info/
- **Chicago CTA**: https://www.transitchicago.com/developers/
- **San Francisco**: https://www.sfmta.com/getting-around/transit/how-tos/transit-data

## 📊 How It Works

```
External API / Mock Generator
         ↓
   ExternalApiPoller (every 5 seconds)
         ↓
   ApiResponseParser (converts to BusUpdate)
         ↓
   InMemoryBusStore (stores bus states)
         ↓
   RealtimeBroadcaster (SSE)
         ↓
   Frontend (React + Leaflet)
         ↓
   Map with Moving Buses 🚌
```

## 🧪 Testing

### Test Mock Data
```bash
# Backend is already configured with mock data enabled
cd backend-services/geolocation-service
mvn spring-boot:run
```

### Test with Manual Data
```bash
curl -X POST http://localhost:8091/api/telemetry \
  -H "Content-Type: application/json" \
  -d '{
    "busId": "TEST-001",
    "lineId": "LINE-1",
    "lat": 33.5731,
    "lon": -7.5898,
    "speed": 20.0,
    "heading": 90,
    "ts": '$(date +%s)000'
  }'
```

### Check Active Buses
```bash
curl http://localhost:8091/api/buses
```

## 📝 Configuration Options

In `application.properties`:

| Property | Description | Default |
|----------|-------------|---------|
| `external.api.url` | External API endpoint URL | (empty) |
| `external.api.poll-interval-ms` | Polling interval in milliseconds | 5000 |
| `external.api.api-key` | API key for authentication | (empty) |
| `external.api.enable-mock` | Enable mock data generator | true |

## 🎯 Current Status

✅ **Mock data is ENABLED by default** - Buses will appear automatically!
✅ **Frontend is ready** - Map component integrated in PassengerDashboard
✅ **Real-time updates working** - SSE broadcasting configured
✅ **Multiple API formats supported** - Automatic format detection

## 🔍 Verify It's Working

1. **Backend logs** should show:
   ```
   Processed X bus updates
   ```

2. **Frontend console** should show:
   - No connection errors
   - SSE connection established

3. **Map** should show:
   - 5 bus markers (if using mock data)
   - Buses moving/updating positions
   - Click markers to see bus info

## 🐛 Troubleshooting

**No buses on map?**
- Check backend is running on port 8091
- Verify `external.api.enable-mock=true` in application.properties
- Check browser console for errors

**API not working?**
- Check API URL is accessible
- Verify API key is correct
- Check backend logs for errors
- Enable mock data as fallback

**Map not loading?**
- Check internet connection (map tiles from CDN)
- Verify Leaflet CSS is imported
- Check browser console

## 📚 Next Steps

1. **Find a real transit API** for your city/region
2. **Configure the API URL** in application.properties
3. **Disable mock data** when real API is working
4. **Customize map center** to your city coordinates
5. **Add more features** (routes, ETAs, filters, etc.)


