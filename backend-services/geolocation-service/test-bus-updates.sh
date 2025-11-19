#!/bin/bash

# Test script to send bus location updates to the geolocation service
# Usage: ./test-bus-updates.sh

API_URL="http://localhost:8091/api/telemetry"

# Example coordinates (Casablanca, Morocco area)
LAT_BASE=33.5731
LON_BASE=-7.5898

# Function to send a bus update
send_update() {
    local bus_id=$1
    local line_id=$2
    local lat=$3
    local lon=$4
    local speed=$5
    local heading=$6
    
    local timestamp=$(date +%s)000  # Current time in milliseconds
    
    curl -X POST "$API_URL" \
        -H "Content-Type: application/json" \
        -d "{
            \"busId\": \"$bus_id\",
            \"lineId\": \"$line_id\",
            \"lat\": $lat,
            \"lon\": $lon,
            \"speed\": $speed,
            \"heading\": $heading,
            \"ts\": $timestamp
        }"
    
    echo ""
}

echo "Sending test bus updates..."
echo "Press Ctrl+C to stop"
echo ""

# Simulate multiple buses moving
bus1_lat=$LAT_BASE
bus1_lon=$LON_BASE
bus2_lat=$((LAT_BASE + 0.01))
bus2_lon=$((LON_BASE + 0.01))
bus3_lat=$((LAT_BASE - 0.01))
bus3_lon=$((LON_BASE - 0.01))

counter=0
while true; do
    # Bus 1 - moving north
    bus1_lat=$(echo "$bus1_lat + 0.0001" | bc)
    send_update "BUS-001" "LINE-1" "$bus1_lat" "$bus1_lon" "15.5" "0"
    
    # Bus 2 - moving east
    bus2_lon=$(echo "$bus2_lon + 0.0001" | bc)
    send_update "BUS-002" "LINE-2" "$bus2_lat" "$bus2_lon" "20.0" "90"
    
    # Bus 3 - moving south
    bus3_lat=$(echo "$bus3_lat - 0.0001" | bc)
    send_update "BUS-003" "LINE-1" "$bus3_lat" "$bus3_lon" "12.3" "180"
    
    sleep 3
    counter=$((counter + 1))
    echo "Sent update batch #$counter"
done




