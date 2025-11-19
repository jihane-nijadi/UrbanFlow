const host = window.location.hostname;
const API_HOST = host === "localhost" || host === "127.0.0.1" ? "http://localhost:8081" : "http://route-service:8081";
const BASE = (process.env.REACT_APP_ROUTE_API_URL || API_HOST) + "/api";

const routeService = {
  // Search routes: route-service expects a POST /api/routes/search with { departureStopId, arrivalStopId }
  async searchRoutes(filters = {}) {
    const body = {
      departureStopId: filters.departureStopId || filters.from || '',
      arrivalStopId: filters.arrivalStopId || filters.to || ''
    };

    const res = await fetch(`${BASE}/routes/search`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });
    if (!res.ok) {
      const txt = await res.text().catch(() => '');
      throw new Error(txt || 'Failed to search routes');
    }
    return res.json();
  }
};

export default routeService;
