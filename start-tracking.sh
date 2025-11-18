#!/bin/bash

echo "🚌 UrbanFlow Real-Time Bus Tracking - Quick Start"
echo "=================================================="
echo ""

# Check if backend is running
if ! curl -s http://localhost:8091/api/buses > /dev/null 2>&1; then
    echo "⚠️  Backend is not running on port 8091"
    echo "   Please start it first:"
    echo "   cd backend-services/geolocation-service && mvn spring-boot:run"
    echo ""
    exit 1
fi

echo "✓ Backend is running"
echo ""

# Check if Python is available
if command -v python3 &> /dev/null; then
    echo "Starting bus location simulator..."
    echo "Press Ctrl+C to stop"
    echo ""
    cd backend-services/geolocation-service
    python3 test-bus-updates.py
else
    echo "⚠️  Python3 not found. Please install Python or use curl manually."
    echo ""
    echo "Manual test command:"
    echo "curl -X POST http://localhost:8091/api/telemetry \\"
    echo "  -H 'Content-Type: application/json' \\"
    echo "  -d '{\"busId\":\"BUS-001\",\"lineId\":\"LINE-1\",\"lat\":33.5731,\"lon\":-7.5898,\"speed\":15.5,\"heading\":45,\"ts\":'$(date +%s000)'}'"
fi
