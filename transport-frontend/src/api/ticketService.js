const host = window.location.hostname;
const API_HOST = host === "localhost" || host === "127.0.0.1" ? "http://localhost:8082" : "http://ticket-service:8082";
const BASE = (process.env.REACT_APP_TICKET_API_URL || API_HOST) + "/api";

const getAuthHeaders = () => {
  const user = JSON.parse(localStorage.getItem('user') || '{}');
  const headers = {};
  const token = user && (user.token || user.accessToken || user.authToken);
  if (token) headers['Authorization'] = `Bearer ${token}`;
  return headers;
};

const ticketService = {
  async purchaseTicket(payload) {
    const res = await fetch(`${BASE}/tickets/purchase`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
      body: JSON.stringify(payload),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Ticket purchase failed');
    }
    return res.json();
  },

  // Fetch ticket details by id
  async getTicket(ticketId) {
    const res = await fetch(`${BASE}/tickets/${encodeURIComponent(ticketId)}`, {
      headers: { ...getAuthHeaders() }
    });
    if (!res.ok) {
      throw new Error('Failed to fetch ticket');
    }
    return res.json();
  },

  // Fetch tickets for current user. If userId provided, use it; otherwise attempts a /tickets/my endpoint
  async getMyTickets(userId) {
    let url;
    if (userId) url = `${BASE}/tickets/user/${encodeURIComponent(userId)}`;
    else url = `${BASE}/tickets/my`;
    const res = await fetch(url, { headers: { ...getAuthHeaders() } });
    if (!res.ok) {
      // try a fallback that doesn't include /user/ (some deployments use /tickets/user/{id})
      const text = await res.text().catch(() => '');
      throw new Error(text || 'Failed to fetch user tickets');
    }
    return res.json();
  },

  // Cancel a ticket. Tries POST /tickets/{id}/cancel, falls back to DELETE /tickets/{id}
  async cancelTicket(ticketId) {
    const cancelUrl = `${BASE}/tickets/${encodeURIComponent(ticketId)}/cancel`;
    let res = await fetch(cancelUrl, { method: 'POST', headers: { 'Content-Type': 'application/json', ...getAuthHeaders() } }).catch(() => null);
    if (res && res.ok) return res.json();

    // fallback to DELETE (some APIs use delete)
    res = await fetch(`${BASE}/tickets/${encodeURIComponent(ticketId)}`, { method: 'DELETE', headers: { ...getAuthHeaders() } }).catch(() => null);
    if (res && (res.ok || res.status === 204)) return { success: true };

    const errText = res ? await res.text().catch(() => '') : 'Network error';
    throw new Error(errText || 'Failed to cancel ticket');
  }
};

export default ticketService;
