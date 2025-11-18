const ROUTE_API_URL = "http://localhost:8083/api/routes";

const routeService = {
  // Get all bus stops
  getAllStops: async () => {
    const res = await fetch(`${ROUTE_API_URL}/stops`);
    if (!res.ok) {
      throw new Error("Failed to fetch bus stops");
    }
    return res.json();
  },

  // Search for routes between two stops
  searchRoutes: async (departureStopId, arrivalStopId) => {
    const res = await fetch(`${ROUTE_API_URL}/search`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        departureStopId,
        arrivalStopId,
      }),
    });
    if (!res.ok) {
      throw new Error("Failed to search routes");
    }
    return res.json();
  },

  // Get complete route for a specific bus
  getBusRoute: async (busId, departureStopId, arrivalStopId) => {
    const res = await fetch(
      `${ROUTE_API_URL}/bus/${busId}/route?departureStopId=${departureStopId}&arrivalStopId=${arrivalStopId}`
    );
    if (!res.ok) {
      throw new Error("Failed to fetch bus route");
    }
    return res.json();
  },
};

export default routeService;

