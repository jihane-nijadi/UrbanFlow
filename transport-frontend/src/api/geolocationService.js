const host = window.location.hostname;
const API_HOST = host === "localhost" || host === "127.0.0.1" ? "http://localhost:8091" : "http://geolocation-service:8091";
const BASE = (process.env.REACT_APP_GEOLOCATION_API_URL || API_HOST) + "/api";

const geolocationService = {
  async getAllBuses() {
    const res = await fetch(`${BASE}/buses`);
    if (!res.ok) throw new Error('Failed to fetch buses');
    return res.json();
  },

  async getAllStops() {
    const res = await fetch(`${BASE}/stops`);
    if (!res.ok) throw new Error('Failed to fetch stops');
    return res.json();
  },

  async getBusById(busId) {
    const res = await fetch(`${BASE}/buses/${encodeURIComponent(busId)}`);
    if (!res.ok) throw new Error('Failed to fetch bus');
    return res.json();
  },

  // If you want SSE, connect to `${BASE.replace('/api','')}/api/realtime/stream` from components
};

export default geolocationService;
