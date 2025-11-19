const host = window.location.hostname;
const API_HOST = host === "localhost" || host === "127.0.0.1" ? "http://localhost:8085" : "http://payment-service:8085";
const BASE = (process.env.REACT_APP_PAYMENT_API_URL || API_HOST) + "/api";

const getAuthHeaders = () => {
  const user = JSON.parse(localStorage.getItem('user') || '{}');
  const headers = {};
  const token = user && (user.token || user.accessToken || user.authToken);
  if (token) headers['Authorization'] = `Bearer ${token}`;
  return headers;
};

const paymentService = {
  // Simulate paying for a ticket/order
  async payTicket(ticketId, payload) {
    const res = await fetch(`${BASE}/payments/${encodeURIComponent(ticketId)}/pay`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
      body: JSON.stringify(payload),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Payment failed');
    }
    return res.json();
  },

  // Create a pending payment session from the frontend (ticket intent)
  async createSession(payload) {
    const res = await fetch(`${BASE}/payments/sessions`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
      body: JSON.stringify(payload),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Create session failed');
    }
    return res.json();
  }

  ,
  async getSession(sessionId) {
    const res = await fetch(`${BASE}/payments/sessions/${encodeURIComponent(sessionId)}`, {
      method: 'GET',
      headers: { ...getAuthHeaders() }
    });
    if (!res.ok) {
      const txt = await res.text().catch(() => '');
      throw new Error(txt || 'Failed to fetch session');
    }
    return res.json();
  }
};

export default paymentService;
