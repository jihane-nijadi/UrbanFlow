// Determine backend base URL depending on environment:
// - Local dev (running `npm start`): call Spring Boot directly on localhost:8083
// - In Docker/Kubernetes/production: use relative `/api` so Nginx in the frontend
//   container can proxy to the correct backend service.
const host = window.location.hostname;

let API_BASE = "/api";

if (process.env.REACT_APP_API_URL) {
  // Explicit override via env var
  API_BASE = process.env.REACT_APP_API_URL.replace(/\/$/, "") + "/api";
} else if (host === "localhost" || host === "127.0.0.1") {
  // Local development: hit user-service directly
  API_BASE = "http://localhost:8083/api";
}

const API_URL = API_BASE;

const authService = {
  register: async (data) => {
    const res = await fetch(`${API_URL}/auth/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data),
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || "Registration failed");
    }

    const user = await res.json();
    localStorage.setItem("user", JSON.stringify(user));
    return user;
  },

  login: async (data) => {
    const res = await fetch(`${API_URL}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data),
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || "Invalid email or password");
    }

    const user = await res.json();
    localStorage.setItem("user", JSON.stringify(user));
    return user;
  },

  getCurrentUser: () => {
    return JSON.parse(localStorage.getItem("user"));
  },

  logout: () => {
    localStorage.removeItem("user");
  },
};

export default authService;
