import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ticketService from '../api/ticketService';
import paymentService from '../api/paymentService';

export default function MyTickets() {
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [processingId, setProcessingId] = useState(null);

  const user = JSON.parse(localStorage.getItem('user') || '{}');
  // payment-service stores sessions under the user's email in our setup; prefer email
  const userId = user && (user.email || user.id || user.userId || user._id);

  useEffect(() => {
    let mounted = true;
    setLoading(true);
    setError(null);
    // Prefer fetching paid tickets via payment-service so we show tickets that have completed payments
    (async () => {
      try {
        // get completed payment sessions for this user from payment-service
        const sessions = await paymentService.getCompletedSessionsForUser(userId);
        if (!mounted) return;
        const arr = Array.isArray(sessions) ? sessions : (sessions.sessions || []);
        // For each session that has a ticketId, fetch the ticket details from ticket-service
        const ticketPromises = arr.filter(s => s.ticketId).map(s => ticketService.getTicket(s.ticketId).catch(() => null));
        const ticketResults = await Promise.all(ticketPromises);
        const validTickets = ticketResults.filter(Boolean);
        // Build a display list that includes tickets and completed sessions without a ticket (payment-only)
        const paymentsWithoutTicket = arr.filter(s => !s.ticketId && (s.status || '').toUpperCase() === 'COMPLETED');
        const paymentPlaceholders = paymentsWithoutTicket.map(s => {
          let meta = {};
          try { meta = s.metadata ? JSON.parse(s.metadata) : {}; } catch (e) { meta = {}; }
          return ({
            _paymentSession: s,
            id: `ps-${s.id}`,
            title: meta.routeId ? `Paiement ${meta.routeId}` : `Paiement ${s.id}`,
            status: s.status || 'COMPLETED',
            purchaseDate: s.createdAt || s.created_at || null,
            price: s.amount || s.price || 0,
            departureStop: meta.departureStop || '',
            arrivalStop: meta.arrivalStop || '',
            departureTime: meta.departureTime || '',
            arrivalTime: meta.arrivalTime || ''
          });
        });
        const merged = [...validTickets, ...paymentPlaceholders];
        // Prefer showing ticket objects first, then payment placeholders
        setTickets(merged);
        setLoading(false);
      } catch (err) {
        if (!mounted) return;
        // fallback to old ticket-service listing if payment-service unavailable
        try {
          const data = await ticketService.getMyTickets(userId);
          setTickets(Array.isArray(data) ? data : (data.tickets || []));
        } catch (e) {
          setError((err && err.message) || 'Impossible de récupérer vos tickets');
        }
        setLoading(false);
      }
    })();
    return () => { mounted = false; };
  }, [userId]);

  const doCancel = async (ticketId) => {
    if (!window.confirm("Confirmer l'annulation du ticket ?")) return;
    setProcessingId(ticketId);
    try {
      await ticketService.cancelTicket(ticketId);
      setTickets(prev => prev.map(t => t.id === ticketId || t.ticketId === ticketId ? { ...t, status: 'CANCELLED' } : t));
      alert("Ticket annulé avec succès.");
    } catch (e) {
      console.error(e);
      alert(e.message || "Échec de l'annulation");
    } finally {
      setProcessingId(null);
    }
  };

  const navigate = useNavigate();

  const doChange = (ticket) => {
    // Store ticket as prefill payload and navigate to purchase tab
    try {
      const prefill = {
        routeId: ticket.routeId || ticket.routeCode || ticket.routeName,
        departureStop: ticket.departureStop,
        arrivalStop: ticket.arrivalStop,
        departureTime: ticket.travelDate || ticket.departureTime || ticket.travelDateTime,
        price: ticket.price || ticket.amount || 7,
        quantity: ticket.quantity || 1
      };
      localStorage.setItem('uf_prefill_ticket', JSON.stringify(prefill));
      // Open passenger dashboard and activate purchase tab
      navigate('/passenger');
      // Use storage event to signal the other tab to open the correct tab if needed
      try { window.localStorage.setItem('uf_open_acheter', Date.now().toString()); } catch (e) {}
    } catch (e) {
      console.error('Failed to set prefill', e);
      alert('Impossible d\u2019initier le changement, contactez le support.');
    }
  };

  const formatDate = (iso) => {
    try {
      if (!iso) return '-';
      const d = new Date(iso);
      return d.toLocaleString();
    } catch (e) { return iso; }
  };

  return (
    <div className="card" style={{ maxWidth: 1100, margin: '0 auto' }}>
      <h3 style={{ textAlign: 'center', color: '#ffffff', marginBottom: 6 }}>Mes tickets</h3>

      {loading && <div style={{ marginTop: 18 }}>Chargement de vos tickets...</div>}
      {error && <div style={{ marginTop: 18, color: '#9b1c1c' }}>{error}</div>}

      {!loading && !error && tickets && tickets.length === 0 && (
        <div style={{ marginTop: 18, color: '#6b7280' }}>Vous n'avez aucun ticket actif pour le moment.</div>
      )}

      {!loading && tickets && tickets.length > 0 && (
        <div style={{ marginTop: 18 }}>
          <div style={{ display: 'flex', gap: 12, alignItems: 'flex-start' }}>
            {/* Purchased */}
            <div style={{ flex: 1 }}>
              <h4 style={{ color: '#ffffff' }}>Tickets achetés</h4>
              <div className="results-grid">
                {tickets.filter(t => {
                  const s = (t.status || t.ticketStatus || '').toUpperCase();
                  return ['ACTIVE','PAID','COMPLETED','SUCCESS'].includes(s);
                }).map((t) => {
                  const id = t.id || t.ticketId || t.ticket_id;
                  const isPaymentPlaceholder = Boolean(t._paymentSession);
                  let meta = {};
                  if (isPaymentPlaceholder) {
                    try { meta = t._paymentSession && t._paymentSession.metadata ? (typeof t._paymentSession.metadata === 'string' ? JSON.parse(t._paymentSession.metadata) : t._paymentSession.metadata) : {}; } catch (e) { meta = {}; }
                  }

                  const title = t.routeName || t.routeCode || (t.ticketType && t.ticketType.name) || t.bus || meta.routeId || meta.bus || (t.type) || `Ticket ${id ? String(id).slice(0,8) : ''}`;
                  const busNumber = t.bus || meta.bus || (t.raw && t.raw.bus) || '';
                  const from = t.departureStop || t.from || meta.departureStop || '';
                  const to = t.arrivalStop || t.to || meta.arrivalStop || '';
                  const departure = t.departureTime || t.travelDate || t.departure || meta.departureTime || '';
                  const arrival = t.arrivalTime || t.arrival || meta.arrivalTime || '';
                  const price = t.price || t.amount || t.fare || (meta.amount || meta.price) || 7;
                  const status = (t.status || t.ticketStatus || (t._paymentSession && t._paymentSession.status) || '').toUpperCase();
                  const purchaseDate = t.purchaseDate || t.createdAt || t.purchase_date || t.created_at || (t._paymentSession && (t._paymentSession.createdAt || t._paymentSession.created_at)) || null;

                  return (
                    <div className="result-card" key={id}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                        <div>
                          <div style={{ fontWeight: 800, fontSize: '1rem', display: 'flex', alignItems: 'center', gap: 8 }}>
                            <div>{title}</div>
                            {busNumber ? <div style={{ fontSize: 12, color: '#6b7280', marginLeft: 8 }}>{busNumber}</div> : null}
                            <span style={{ fontSize: 12, color: '#6b7280', marginLeft: 8 }}>{id ? String(id).slice(0,8) : ''}</span>
                          </div>
                          <div style={{ color: '#374151', marginTop: 6 }}>{from} → {to}</div>
                          <div style={{ marginTop: 6, fontSize: 13, color: '#6b7280' }}>ID: {id}</div>
                        </div>
                        <div style={{ textAlign: 'right' }}>
                          <div style={{ fontWeight: 800, fontSize: '1rem', color: '#111827' }}>
                            {`${Number(price).toFixed(2)} DH`}
                            <div style={{ fontSize: 12, color: '#9ca3af' }}>{`(${Number(price).toFixed(2)} DH)`}</div>
                          </div>
                          <div style={{ color: '#6b7280', fontSize: 12 }}>{t.seats || ''}</div>
                        </div>
                      </div>

                      <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 10, alignItems: 'center' }}>
                        <div style={{ color: '#6b7280', fontSize: 13 }}>
                          Départ : {departure ? formatDate(departure) : '-'} · Arrivée : {arrival ? formatDate(arrival) : '-'}
                          <div style={{ marginTop: 6 }}>Acheté le: {purchaseDate ? formatDate(purchaseDate) : '-'}</div>
                        </div>
                        <div style={{ display: 'flex', gap: 8 }}>
                          {status === 'ACTIVE' && (
                            <>
                              <button aria-label={`Annuler ticket ${id}`} className="buy-button" onClick={() => doCancel(id)} disabled={processingId === id}>{processingId === id ? 'Annulation…' : 'Annuler'}</button>
                              <button className="primary-button" style={{ background: '#f59e0b' }} onClick={() => doChange(t)}>Changer</button>
                            </>
                          )}
                        </div>
                      </div>

                      <div style={{ marginTop: 12, display: 'flex', gap: 8 }}>
                        <a href={`${window.location.origin}/api/tickets/${id}/qrcode`} target="_blank" rel="noreferrer" download>
                          <button className="primary-button" style={{ padding: '0.55rem 0.9rem', background: '#6b7280' }}>Télécharger</button>
                        </a>
                        {/* support contact removed as requested */}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Cancelled */}
            <div style={{ flex: 1 }}>
              <h4 style={{ color: '#ffffff' }}>Tickets annulés</h4>
              <div className="results-grid">
                {tickets.filter(t => {
                  const s = (t.status || t.ticketStatus || '').toUpperCase();
                  return s === 'CANCELLED' || s === 'VOID' || s === 'REFUNDED';
                }).map((t) => (
                  <div className="result-card" key={t.id || t.ticketId || Math.random()}>
                    <div style={{ fontWeight: 800 }}>{t.routeName || t.routeCode || `Ticket ${t.id || t.ticketId || ''}`}</div>
                    <div style={{ marginTop: 6 }} className="meta">Annulé le: {t.updatedAt || t.cancelledAt || t.updatedAt}</div>
                    <div style={{ marginTop: 12 }}><span className="status-badge" style={{ backgroundColor: '#f87171' }}>CANCELLED</span></div>
                  </div>
                ))}
              </div>
            </div>

            {/* Exchanged */}
            <div style={{ flex: 1 }}>
              <h4 style={{ color: '#ffffff' }}>Tickets échangés</h4>
              <div className="results-grid">
                {tickets.filter(t => {
                  const s = (t.status || t.ticketStatus || '').toUpperCase();
                  return s === 'EXCHANGED' || t.exchanged === true || t.status === 'EXCHANGE_PENDING';
                }).map((t) => (
                  <div className="result-card" key={t.id || t.ticketId || Math.random()}>
                    <div style={{ fontWeight: 800 }}>{t.routeName || t.routeCode || `Ticket ${t.id || t.ticketId || ''}`}</div>
                    <div style={{ marginTop: 6 }} className="meta">Échangé le: {t.updatedAt || t.exchangedAt || ''}</div>
                    <div style={{ marginTop: 12 }}><span className="status-badge" style={{ backgroundColor: '#fbbf24' }}>EXCHANGED</span></div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
