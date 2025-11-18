// Determine backend base URL depending on environment:
// - Local dev (running `npm start`): frontend served from localhost, backend likely at localhost:8083
// - Docker compose: frontend container will resolve `user-service` hostname on the same Docker network
const host = window.location.hostname;
const API_HOST = host === "localhost" || host === "127.0.0.1" ? "http://localhost:8083" : "http://user-service:8083";
const API_URL = (process.env.REACT_APP_API_URL || API_HOST) + "/api";

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
