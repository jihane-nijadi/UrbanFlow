const GEOLOCATION_API_URL = "http://localhost:8091/api";

const geolocationService = {
  // Get all active buses
  getActiveBuses: async () => {
    const res = await fetch(`${GEOLOCATION_API_URL}/buses`);
    if (!res.ok) {
      throw new Error("Failed to fetch active buses");
    }
    return res.json();
  },

  // Get specific bus by ID
  getBus: async (busId) => {
    const res = await fetch(`${GEOLOCATION_API_URL}/buses/${busId}`);
    if (!res.ok) {
      throw new Error(`Failed to fetch bus ${busId}`);
    }
    return res.json();
  },

  // Get bus position history
  getBusHistory: async (busId, limit = 20) => {
    const res = await fetch(`${GEOLOCATION_API_URL}/buses/${busId}/positions?limit=${limit}`);
    if (!res.ok) {
      throw new Error(`Failed to fetch bus history for ${busId}`);
    }
    return res.json();
  },

  // Subscribe to real-time updates via Server-Sent Events
  subscribeToRealtimeUpdates: (onUpdate) => {
    const eventSource = new EventSource(`${GEOLOCATION_API_URL}/realtime/stream`);

    eventSource.addEventListener("bus_updates", (event) => {
      try {
        const updates = JSON.parse(event.data);
        onUpdate(updates);
      } catch (error) {
        console.error("Error parsing SSE update:", error);
      }
    });

    eventSource.onerror = (error) => {
      console.error("SSE connection error:", error);
      // Optionally reconnect after a delay
    };

    return () => {
      eventSource.close();
    };
  },

  // Get all bus stops
  getAllStops: async () => {
    const res = await fetch(`${GEOLOCATION_API_URL}/stops`);
    if (!res.ok) {
      throw new Error("Failed to fetch bus stops");
    }
    return res.json();
  },

  // Get specific stop by ID
  getStop: async (stopId) => {
    const res = await fetch(`${GEOLOCATION_API_URL}/stops/${stopId}`);
    if (!res.ok) {
      throw new Error(`Failed to fetch stop ${stopId}`);
    }
    return res.json();
  },

  // Get stops by line
  getStopsByLine: async (lineId) => {
    const res = await fetch(`${GEOLOCATION_API_URL}/stops/line/${lineId}`);
    if (!res.ok) {
      throw new Error(`Failed to fetch stops for line ${lineId}`);
    }
    return res.json();
  },
};

export default geolocationService;



