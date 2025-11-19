# External API Integration Guide

## Overview

The geolocation service supports multiple external transit APIs for real-time bus tracking. It can also generate mock data for testing.

## Supported API Formats

The service automatically detects and parses these formats:

1. **Simple JSON Array** (Recommended for custom APIs):
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

2. **Transitland Format**:
```json
{
  "vehicle_positions": [
    {
      "id": "BUS-001",
      "route_id": "LINE-1",
      "latitude": 33.5731,
      "longitude": -7.5898
    }
  ]
}
```

3. **GTFS-RT Format**:
```json
{
  "entity": [
    {
      "id": "1",
      "vehicle": {
        "vehicle": {"id": "BUS-001"},
        "trip": {"routeId": "LINE-1"},
        "position": {
          "latitude": 33.5731,
          "longitude": -7.5898,
          "bearing": 45,
          "speed": 7.08
        },
        "timestamp": 1704067200
      }
    }
  ]
}
```

## Configuration

Edit `application.properties`:

### Option 1: Use Real External API

```properties
# Your API endpoint
external.api.url=https://your-api.com/vehicle-positions
external.api.poll-interval-ms=5000
external.api.api-key=YOUR_API_KEY_HERE
external.api.enable-mock=false
```

### Option 2: Use Mock Data (For Testing)

```properties
external.api.url=
external.api.enable-mock=true
external.api.poll-interval-ms=5000
```

## Free Transit APIs You Can Use

### 1. Transitland API (Free Tier Available)
- **URL Format**: `https://transit.land/api/v2/rest/vehicle_positions?feed_onestop_id=FEED_ID`
- **Documentation**: https://www.transit.land/documentation
- **Get API Key**: Sign up at https://www.transit.land/
- **Example**:
```properties
external.api.url=https://transit.land/api/v2/rest/vehicle_positions?feed_onestop_id=F-9q8y-sfmta
external.api.api-key=YOUR_TRANSITLAND_API_KEY
```

### 2. OpenTripPlanner (Self-hosted)
- **URL Format**: `http://localhost:8080/otp/routers/default/vehiclePositions`
- **Documentation**: http://docs.opentripplanner.org/
- **Note**: Requires local installation

### 3. City-Specific APIs
Many cities provide their own transit APIs:
- **NYC MTA**: https://api.mta.info/
- **Chicago CTA**: https://www.transitchicago.com/developers/
- **San Francisco Muni**: https://www.sfmta.com/getting-around/transit/how-tos/transit-data

### 4. Custom API
Create your own API endpoint that returns bus positions in one of the supported formats.

## Field Name Mapping

The parser automatically maps these field names:

| Our Field | Accepted Field Names |
|-----------|---------------------|
| busId | `busId`, `id`, `vehicle_id`, `vehicleId` |
| lineId | `lineId`, `route_id`, `routeId`, `line` |
| lat | `lat`, `latitude` |
| lon | `lon`, `lng`, `longitude` |
| speed | `speed`, `velocity` |
| heading | `heading`, `bearing`, `direction` |
| ts | `ts`, `timestamp`, `time` |

## Testing

### Test with Mock Data

1. Set `external.api.enable-mock=true` in `application.properties`
2. Start the service: `mvn spring-boot:run`
3. Mock buses will automatically appear and move on the map

### Test with Real API

1. Configure your API URL and key in `application.properties`
2. Start the service: `mvn spring-boot:run`
3. Check logs for: `"Fetched X bus updates from external API"`
4. Buses should appear on the frontend map

### Manual Test Endpoint

Send test data directly:
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

## Troubleshooting

### No buses showing
- Check if `external.api.enable-mock=true` is set
- Check backend logs for API errors
- Verify API URL is accessible
- Check CORS settings if calling from browser

### API connection errors
- Verify API URL is correct
- Check if API key is required and valid
- Test API endpoint with curl/browser
- Check network connectivity

### Parsing errors
- Verify API response format matches one of the supported formats
- Check logs for specific parsing errors
- Ensure required fields (busId, lat, lon) are present


