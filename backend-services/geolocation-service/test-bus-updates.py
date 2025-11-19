#!/usr/bin/env python3
"""
Test script to send bus location updates to the geolocation service.
Usage: python3 test-bus-updates.py
"""

import requests
import time
import json
from datetime import datetime

API_URL = "http://localhost:8091/api/telemetry"

# Example coordinates (Casablanca, Morocco area)
LAT_BASE = 33.5731
LON_BASE = -7.5898


def send_update(bus_id, line_id, lat, lon, speed, heading):
    """Send a bus location update to the API."""
    payload = {
        "busId": bus_id,
        "lineId": line_id,
        "lat": lat,
        "lon": lon,
        "speed": speed,
        "heading": heading,
        "ts": int(time.time() * 1000)  # Current time in milliseconds
    }
    
    try:
        response = requests.post(API_URL, json=payload)
        if response.status_code == 202:
            print(f"✓ Sent update for {bus_id} at ({lat:.6f}, {lon:.6f})")
        else:
            print(f"✗ Failed to send update for {bus_id}: {response.status_code}")
    except Exception as e:
        print(f"✗ Error sending update for {bus_id}: {e}")


def main():
    print("Starting bus location simulator...")
    print("Press Ctrl+C to stop")
    print("-" * 50)
    
    # Initialize bus positions
    bus1_lat = LAT_BASE
    bus1_lon = LON_BASE
    bus2_lat = LAT_BASE + 0.01
    bus2_lon = LON_BASE + 0.01
    bus3_lat = LAT_BASE - 0.01
    bus3_lon = LON_BASE - 0.01
    
    counter = 0
    
    try:
        while True:
            # Bus 1 - moving north
            bus1_lat += 0.0001
            send_update("BUS-001", "LINE-1", bus1_lat, bus1_lon, 15.5, 0)
            
            # Bus 2 - moving east
            bus2_lon += 0.0001
            send_update("BUS-002", "LINE-2", bus2_lat, bus2_lon, 20.0, 90)
            
            # Bus 3 - moving south
            bus3_lat -= 0.0001
            send_update("BUS-003", "LINE-1", bus3_lat, bus3_lon, 12.3, 180)
            
            counter += 1
            print(f"Batch #{counter} completed\n")
            
            time.sleep(3)  # Wait 3 seconds before next update
            
    except KeyboardInterrupt:
        print("\n\nStopped by user. Total batches sent: {}".format(counter))


if __name__ == "__main__":
    main()




