import React, { useEffect, useState } from 'react';
// Import the mock data from TicketPurchase for suggestions
import { useNavigate } from 'react-router-dom';
import GeolocationMockSuggestions from '../components/GeolocationMockSuggestions';
import ticketService from '../api/ticketService';
import jsPDF from 'jspdf';
import paymentService from '../api/paymentService';

export default function MyTickets() {
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [processingId, setProcessingId] = useState(null);
  const [rawDebug, setRawDebug] = useState({ sessions: null, tickets: null });
  const [showRawDebug, setShowRawDebug] = useState(false);
  const [frontendDeleted, setFrontendDeleted] = useState(false);

  const user = JSON.parse(localStorage.getItem('user') || '{}');
  // payment-service stores sessions under the user's email in our setup; prefer email
  const userId = user && (user.email || user.id || user.userId || user._id);

  useEffect(() => {
    let mounted = true;
    // reset frontend-deleted marker when reloading tickets from server
    setFrontendDeleted(false);
    setLoading(true);
    setError(null);
    // Prefer fetching paid tickets via payment-service so we show tickets that have completed payments
    (async () => {
      try {
        // Try several identifiers when asking payment-service for completed sessions
        const idsToTry = [];
        if (user) {
          // push likely identity fields in order of preference
          if (user.email) idsToTry.push(user.email);
          if (user.id) idsToTry.push(user.id);
          if (user.userId) idsToTry.push(user.userId);
          if (user._id) idsToTry.push(user._id);
          if (user.username) idsToTry.push(user.username);
        }
        // include the previously computed userId too (may be same as above)
        if (userId) idsToTry.push(userId);

        let sessions = null;
        let usedId = null;
        for (const idCandidate of idsToTry) {
          if (!idCandidate) continue;
          try {
            const res = await paymentService.getCompletedSessionsForUser(idCandidate);
            // Accept when we get an array or an object with sessions
            if (res) {
              sessions = res;
              usedId = idCandidate;
              break;
            }
          } catch (e) {
            // try next candidate
            console.debug('getCompletedSessions failed for', idCandidate, e && e.message);
          }
        }
        // final attempt: try without id (some deployments support /payments/user//completed)
        if (!sessions) {
          try { sessions = await paymentService.getCompletedSessionsForUser(userId || ''); } catch (e) { sessions = null; }
        }
        // set debug raw sessions for inspection
        setRawDebug(prev => ({ ...prev, sessions }));
        if (!mounted) return;
        const arr = Array.isArray(sessions) ? sessions : (sessions && (sessions.sessions || sessions.data) ? (sessions.sessions || sessions.data) : []);
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

        // Clean up merged list: remove mock entries, dedupe by departure+arrival+title, and ensure unique bus numbers
        const usedKeys = new Set();
        const usedBusNumbers = new Set();
        let generatedBusCounter = 1;
        const cleaned = [];

        for (const item of merged) {
          const idStr = item && item.id ? String(item.id) : '';
          // remove synthetic/mock items (ids containing '|mock' or 'mock')
          if (idStr.toLowerCase().includes('|mock') || idStr.toLowerCase().includes('mock')) continue;

          // extract metadata safely
          let meta = {};
          if (item._paymentSession) {
            try { meta = item._paymentSession && item._paymentSession.metadata ? (typeof item._paymentSession.metadata === 'string' ? JSON.parse(item._paymentSession.metadata) : item._paymentSession.metadata) : {}; } catch (e) { meta = {}; }
          }

          // initial title and bus values (may be cleaned)
          const rawTitle = item.routeName || item.routeCode || (item.ticketType && item.ticketType.name) || item.bus || meta.routeId || meta.bus || (item.type) || `Ticket ${idStr ? idStr.slice(0,8) : ''}`;
          let title = sanitizeLabel(rawTitle, meta, 'title') || rawTitle;
          // If title is just a bus placeholder like BUS-001, remove it and fallback to a better label
          if (title && /bus-?001/i.test(title)) {
            title = (meta && (meta.routeName || meta.routeId)) || (rawTitle && rawTitle.replace(/bus-?001/ig, '').trim()) || '';
            if (!title) title = 'Route inconnue';
          }
          let busNumberRaw = item.bus || meta.bus || (item.raw && item.raw.bus) || '';
          // sanitize bus number via sanitizer (will clear mock-like labels)
          busNumberRaw = sanitizeLabel(busNumberRaw, meta, 'bus') || '';
          // If the bus value is the placeholder/test value BUS-001, treat it as missing
          if (busNumberRaw && /bus-?001/i.test(busNumberRaw)) {
            busNumberRaw = '';
          }
          // compute travelDate and times
          const travelDateIso = item.travelDate || (item._paymentSession && (item._paymentSession.travelDate || item._paymentSession.travel_date)) || null;
          const departureTimeStr = (item._paymentSession ? (meta.departureTime || item.departureTime) : (item.departureTime || meta.departureTime)) || '';
          let arrivalTimeStr = (item._paymentSession ? (meta.arrivalTime || item.arrivalTime) : (item.arrivalTime || meta.arrivalTime)) || '';
          arrivalTimeStr = computeArrivalTime(departureTimeStr, arrivalTimeStr, travelDateIso) || arrivalTimeStr;
          const departureDisplay = travelDateIso ? formatDateTimeFromParts(travelDateIso, departureTimeStr) : (departureTimeStr ? departureTimeStr : (item.departure || meta.departure || '-'));
          const arrivalDisplay = travelDateIso ? formatDateTimeFromParts(travelDateIso, arrivalTimeStr) : (arrivalTimeStr ? arrivalTimeStr : (item.arrival || meta.arrival || '-'));

          // dedupe key: use title + departure + arrival
          const dedupeKey = `${title}||${departureDisplay}||${arrivalDisplay}`;
          if (usedKeys.has(dedupeKey)) continue;
          usedKeys.add(dedupeKey);

          // ensure unique bus numbers
          let busNumber = busNumberRaw || '';
          if (!busNumber) {
            // Default bus number per request: always show "BUS-69" when not provided
            busNumber = 'BUS-69';
          }

          // attach computed display values so render uses consistent values
          const withComputed = { ...item, _computed: { title, busNumber, departureDisplay, arrivalDisplay } };
          cleaned.push(withComputed);
        }

        setTickets(cleaned);
        setLoading(false);
      } catch (err) {
        if (!mounted) return;
        // fallback to old ticket-service listing if payment-service unavailable
        try {
          // try without userId first (uses /tickets/my if token present)
          let data = null;
          try { data = await ticketService.getMyTickets(); } catch (e) { data = null; }
          if (!data) data = await ticketService.getMyTickets(userId);
          const ticketArray = Array.isArray(data) ? data : (data && (data.tickets || data.data) ? (data.tickets || data.data) : []);
          setTickets(ticketArray);
          setRawDebug(prev => ({ ...prev, tickets: ticketArray }));
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

  const generateTicketPdf = (ticket) => {
    try {
      const id = ticket.id || ticket.ticketId || '';
      const title = (ticket._computed && ticket._computed.title) || ticket.routeName || ticket.routeCode || ticket.bus || '';
      const bus = (ticket._computed && ticket._computed.busNumber) || ticket.bus || (ticket.raw && ticket.raw.bus) || '';
      const from = ticket.departureStop || ticket.from || (ticket._paymentSession && (ticket._paymentSession.metadata && JSON.parse(ticket._paymentSession.metadata).departureStop)) || '';
      const to = ticket.arrivalStop || ticket.to || (ticket._paymentSession && (ticket._paymentSession.metadata && JSON.parse(ticket._paymentSession.metadata).arrivalStop)) || '';
      const departure = (ticket._computed && ticket._computed.departureDisplay) || ticket.departureTime || ticket.departure || '';
      const arrival = (ticket._computed && ticket._computed.arrivalDisplay) || ticket.arrivalTime || ticket.arrival || '';
      const price = ticket.price || ticket.amount || 7;
      const purchaseDate = ticket.purchaseDate || ticket.createdAt || (ticket._paymentSession && (ticket._paymentSession.createdAt || ticket._paymentSession.created_at)) || '';

      // Create a monochrome PDF using jsPDF
      const doc = new jsPDF({ unit: 'pt', format: 'a4' });
      const left = 40;
      let y = 60;
      doc.setFont('helvetica');
      doc.setFontSize(18);
      doc.setTextColor(0, 0, 0);
      doc.text(bus || title || 'Ticket', left, y);
      y += 28;
      doc.setFontSize(12);
      doc.text(`${from} → ${to}`, left, y);
      y += 20;
      doc.setFontSize(14);
      doc.text(`${Number(price).toFixed(2)} DH`, left, y);
      y += 18;
      doc.setFontSize(10);
      doc.text(`(${Number(price).toFixed(2)} DH)`, left, y);
      y += 20;
      doc.text(`Départ : ${formatDate(departure)} · Arrivée : ${formatDate(arrival)}`, left, y);
      y += 20;
      if (purchaseDate) {
        doc.text(`Acheté le: ${formatDate(purchaseDate)}`, left, y);
        y += 18;
      }
      doc.setFontSize(9);
      doc.text(`Ticket ID: ${id}`, left, y);

      // Save the PDF file directly
      const fileName = `ticket-${id || Date.now()}.pdf`;
      doc.save(fileName);
    } catch (e) {
      console.error('Failed to generate ticket PDF', e);
      alert('Impossible de générer le PDF du ticket');
    }
  };
        const amount = (res && (res.amount || res.price)) || payload.price || 7;
        if (!sessionId) {
          alert('Impossible de créer la session de paiement — réessayez plus tard.');
          return;
        }
        // Navigate to the PaymentDashboard which listens to ?session= query
        navigate(`/payment?session=${encodeURIComponent(sessionId)}&amount=${encodeURIComponent(String(amount))}`);
      } catch (e) {
        console.error('Failed to confirm change', e);
        alert('Échec de la tentative de changement');
      } finally {
        setProcessingId(null);
      }
    })();
  };

  const formatDate = (iso) => {
    try {
      if (!iso) return '-';
      const d = new Date(iso);
      if (!isNaN(d.getTime())) return d.toLocaleString();
      // if value looks like HH:MM, return it as-is
      if (typeof iso === 'string' && iso.match(/^\d{2}:\d{2}(:\d{2})?$/)) return iso;
      return String(iso);
    } catch (e) { return iso; }
  };

  // Combine a date part (ISO) and a time string (HH:MM or HH:MM:SS) into a displayable datetime
  const formatDateTimeFromParts = (dateIso, timeStr) => {
    try {
      if (dateIso && (typeof dateIso === 'string')) {
        // extract date part yyyy-mm-dd
        const m = dateIso.match(/^(\d{4}-\d{2}-\d{2})/);
        const datePart = m ? m[1] : null;
        if (datePart && timeStr && typeof timeStr === 'string' && timeStr.match(/^\d{2}:\d{2}(:\d{2})?$/)) {
          const iso = `${datePart}T${timeStr}`;
          const d = new Date(iso);
          if (!isNaN(d.getTime())) return d.toLocaleString();
        }
      }
      // fallback: if timeStr is time-only, return it; else try formatting input directly
      if (timeStr && typeof timeStr === 'string' && timeStr.match(/^\d{2}:\d{2}(:\d{2})?$/)) return timeStr;
      if (dateIso) return formatDate(dateIso);
    } catch (e) {}
    return '-';
  };

  const pad2 = (n) => String(n).padStart(2,'0');

  // Compute an arrival time string (HH:MM or HH:MM:SS) given departure and optional arrival.
  // If arrivalMissing OR arrival equals departure, compute arrival = departure + 30 minutes.
  const computeArrivalTime = (departureTimeStr, arrivalTimeStr, travelDateIso) => {
    try {
      // if arrival provided and different from departure, use it
      if (arrivalTimeStr && arrivalTimeStr !== departureTimeStr) return arrivalTimeStr;

      if (!departureTimeStr) return '';
      // Build a Date object. Prefer combining with travelDateIso if present to avoid timezone issues
      let baseIso = null;
      if (travelDateIso && typeof travelDateIso === 'string') {
        const m = travelDateIso.match(/^(\d{4}-\d{2}-\d{2})/);
        if (m) baseIso = `${m[1]}T${departureTimeStr}`;
      }
      if (!baseIso) baseIso = `1970-01-01T${departureTimeStr}`;
      const d = new Date(baseIso);
      if (isNaN(d.getTime())) return '';
      d.setMinutes(d.getMinutes() + 30);
      // return time as HH:MM or HH:MM:SS if seconds present
      const hh = pad2(d.getHours());
      const mm = pad2(d.getMinutes());
      const ss = pad2(d.getSeconds());
      return ss === '00' ? `${hh}:${mm}` : `${hh}:${mm}:${ss}`;
    } catch (e) {
      return '';
    }
  };

  // Sanitize labels that may contain mock tokens like "001|09:30|mock".
  // Prefer metadata values when available, otherwise remove mock segments and return a clean label.
  const sanitizeLabel = (raw, meta, fallback) => {
    try {
      if (!raw && !fallback) return '';
      let s = raw || '';
      if (typeof s !== 'string') s = String(s || '');
      const lower = s.toLowerCase();
      if ((lower.includes('mock') || s.includes('|')) && meta) {
        // prefer meta.routeName or meta.bus etc depending on fallback
        if (fallback && (fallback === 'title')) {
          return meta.routeName || meta.routeId || meta.title || (s.split('|')[0] || '').trim() || '';
        }
        if (fallback && (fallback === 'bus')) {
          return meta.bus || meta.busNumber || (s.split('|')[0] || '').trim() || '';
        }
      }
      // If still contains '|', take the most meaningful segment (first non-mock token)
      if (s.includes('|')) {
        const parts = s.split('|').map(p => p.trim()).filter(p => p && !/mock/i.test(p));
        if (parts.length) return parts.join(' ');
      }
      // otherwise remove explicit 'mock' token
      return s.replace(/\|?\s*mock\s*/i, '').trim();
    } catch (e) {
      return raw || fallback || '';
    }
  };

  // Generate a printable ticket view and trigger print dialog (user can save as PDF)
  const generateTicketPdf = (ticket) => {
    try {
      const id = ticket.id || ticket.ticketId || '';
      const title = (ticket._computed && ticket._computed.title) || ticket.routeName || ticket.routeCode || ticket.bus || '';
      const bus = (ticket._computed && ticket._computed.busNumber) || ticket.bus || (ticket.raw && ticket.raw.bus) || '';
      const from = ticket.departureStop || ticket.from || (ticket._paymentSession && (ticket._paymentSession.metadata && JSON.parse(ticket._paymentSession.metadata).departureStop)) || '';
      const to = ticket.arrivalStop || ticket.to || (ticket._paymentSession && (ticket._paymentSession.metadata && JSON.parse(ticket._paymentSession.metadata).arrivalStop)) || '';
      const departure = (ticket._computed && ticket._computed.departureDisplay) || ticket.departureTime || ticket.departure || '';
      const arrival = (ticket._computed && ticket._computed.arrivalDisplay) || ticket.arrivalTime || ticket.arrival || '';
      const price = ticket.price || ticket.amount || 7;
      const purchaseDate = ticket.purchaseDate || ticket.createdAt || (ticket._paymentSession && (ticket._paymentSession.createdAt || ticket._paymentSession.created_at)) || '';

      const content = `
        <html>
          <head>
            <title>Ticket ${id}</title>
            <style>
              body { font-family: Arial, sans-serif; margin: 24px; color: #111827 }
              .ticket { border: 1px solid #e5e7eb; padding: 18px; border-radius: 8px; max-width: 720px }
              .bus { font-weight: 900; font-size: 20px }
              .route { color: #374151; margin-top: 8px }
              .price { font-weight: 800; font-size: 18px; margin-top: 8px }
              .meta { color: #6b7280; margin-top: 8px }
            </style>
          </head>
          <body>
            <div class="ticket">
              <div class="bus">${bus}</div>
              <div class="route">${from} → ${to}</div>
              <div class="price">${Number(price).toFixed(2)} DH</div>
              <div class="meta">( ${Number(price).toFixed(2)} DH )</div>
              <div class="meta">Départ : ${formatDate(departure)} · Arrivée : ${formatDate(arrival)}</div>
              ${purchaseDate ? `<div class="meta">Acheté le: ${formatDate(purchaseDate)}</div>` : ''}
              <div style="margin-top:16px;font-size:12px;color:#9ca3af">Ticket ID: ${id}</div>
            </div>
          </body>
        </html>
      `;

      const w = window.open('', '_blank');
      if (!w) {
        alert('Impossible d\u2019ouvrir la fenêtre d\u2019impression — autorisez les popups.');
        return;
      }
      w.document.write(content);
      w.document.close();
      // Wait for content to render then print
      setTimeout(() => {
        try { w.focus(); w.print(); w.close(); } catch (e) { /* ignore */ }
      }, 500);
    } catch (e) {
      console.error('Failed to generate ticket PDF', e);
      alert('Impossible de générer le PDF du ticket');
    }
  };

  return (
    <div className="card" style={{ maxWidth: 1100, margin: '0 auto' }}>
      <h3 style={{ textAlign: 'center', color: '#ffffff', marginBottom: 6 }}>Mes tickets</h3>

      {loading && <div style={{ marginTop: 18 }}>Chargement de vos tickets...</div>}
      {error && <div style={{ marginTop: 18, color: '#9b1c1c' }}>{error}</div>}

      {!loading && !error && tickets && tickets.length === 0 && (
        <div style={{ marginTop: 18, color: '#6b7280' }}>Vous n'avez aucun ticket actif pour le moment.</div>
      )}
      {!loading && !error && tickets && tickets.length === 0 && !frontendDeleted && (
        <div style={{ marginTop: 8 }}>
          <button className="logout-button" onClick={() => setShowRawDebug(s => !s)}>{showRawDebug ? 'Cacher debug' : 'Afficher debug'}</button>
          {showRawDebug && (
            <pre style={{ maxHeight: 300, overflow: 'auto', background: '#0f172a', color: '#e5e7eb', padding: 12, marginTop: 8 }}>
              {JSON.stringify(rawDebug, null, 2)}
            </pre>
          )}
        </div>
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
                  // include PENDING tickets so recently created tickets show up immediately
                  return ['ACTIVE','PAID','COMPLETED','SUCCESS','PENDING'].includes(s);
                // Only show purchased tickets, sort BUS-101 to top
                }).filter(t => {
                  const s = (t.status || t.ticketStatus || '').toUpperCase();
                  return ['ACTIVE','PAID','COMPLETED','SUCCESS','PENDING'].includes(s);
                }).sort((a, b) => {
                  // Sort BUS-101 to top
                  const getBus = t => {
                    let meta = {};
                    if (t._paymentSession) {
                      try { meta = t._paymentSession && t._paymentSession.metadata ? (typeof t._paymentSession.metadata === 'string' ? JSON.parse(t._paymentSession.metadata) : t._paymentSession.metadata) : {}; } catch (e) { meta = {}; }
                    }
                    const rawBus = (t._computed && t._computed.busNumber) || t.bus || meta.bus || (t.raw && t.raw.bus) || '';
                    const busNumber = sanitizeLabel(rawBus, meta, 'bus') || rawBus;
                    return String(busNumber).trim();
                  };
                  if (getBus(a) === '101') return -1;
                  if (getBus(b) === '101') return 1;
                  return 0;
                }).map((t) => {
                  const id = t.id || t.ticketId || t.ticket_id;
                  const isPaymentPlaceholder = Boolean(t._paymentSession);
                  let meta = {};
                  if (isPaymentPlaceholder) {
                    try { meta = t._paymentSession && t._paymentSession.metadata ? (typeof t._paymentSession.metadata === 'string' ? JSON.parse(t._paymentSession.metadata) : t._paymentSession.metadata) : {}; } catch (e) { meta = {}; }
                  }

                  // Prefer computed values (from useEffect preprocessing) when available
                  const rawTitleForRender = (t._computed && t._computed.title) || t.routeName || t.routeCode || (t.ticketType && t.ticketType.name) || t.bus || meta.routeId || meta.bus || (t.type) || `Ticket ${id ? String(id).slice(0,8) : ''}`;
                  const title = sanitizeLabel(rawTitleForRender, meta, 'title') || rawTitleForRender;
                  const rawBusForRender = (t._computed && t._computed.busNumber) || t.bus || meta.bus || (t.raw && t.raw.bus) || '';
                  const busNumber = sanitizeLabel(rawBusForRender, meta, 'bus') || rawBusForRender;
                  const from = t.departureStop || t.from || meta.departureStop || '';
                  const to = t.arrivalStop || t.to || meta.arrivalStop || '';
                  const departureDisplay = (t._computed && t._computed.departureDisplay) || (t.travelDate ? formatDateTimeFromParts(t.travelDate, t.departureTime || '') : (t.departureTime || meta.departureTime || t.departure || '-'));
                  const arrivalDisplay = (t._computed && t._computed.arrivalDisplay) || (t.travelDate ? formatDateTimeFromParts(t.travelDate, t.arrivalTime || '') : (t.arrivalTime || meta.arrivalTime || t.arrival || '-'));
                  const price = t.price || t.amount || t.fare || (meta.amount || meta.price) || 7;
                  const status = (t.status || t.ticketStatus || (t._paymentSession && t._paymentSession.status) || '').toUpperCase();
                  const purchaseDate = t.purchaseDate || t.createdAt || t.purchase_date || t.created_at || (t._paymentSession && (t._paymentSession.createdAt || t._paymentSession.created_at)) || null;

                  return (
                    <div className="result-card" key={id}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                        <div>
                          {busNumber && (
                            <div style={{ fontWeight: 900, fontSize: '1.05rem' }}>{busNumber}</div>
                          )}
                          <div style={{ color: '#374151', marginTop: 6 }}>{from} → {to}</div>
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
                          Départ : {departureDisplay || '-'} · Arrivée : {arrivalDisplay || '-'}
                          <div style={{ marginTop: 6 }}>Acheté le: {purchaseDate ? formatDate(purchaseDate) : '-'}</div>
                        </div>
                          <div className="card-actions" style={{ display: 'flex', gap: 8 }}>
                            {(status === 'ACTIVE' || status === 'PENDING') && (
                              <>
                                <button aria-label={`Supprimer ticket ${id}`} className="buy-button" onClick={() => doDelete(id)} disabled={processingId === id}>{processingId === id ? 'Suppression…' : 'Supprimer'}</button>
                                <button className="primary-button" style={{ background: '#f59e0b' }} onClick={() => doChange(t)}>Changer</button>
                              </>
                            )}
                          </div>
                      </div>

                      <div style={{ marginTop: 12, display: 'flex', gap: 8 }}>
                        <button className="primary-button" style={{ padding: '0.55rem 0.9rem', background: '#6b7280' }} onClick={() => generateTicketPdf(t)}>Télécharger</button>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* NOTE: 'Tickets annulés' column removed per user request */}

            {/* Exchanged column removed: only purchased tickets are shown */}
          </div>
        </div>
      )}
      {/* Change suggestions modal */}
      {showChangeModal && changeModalTicket && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(2,6,23,0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 50 }}>
          <div style={{ background: '#fff', padding: 16, borderRadius: 8, width: '95%', maxWidth: 420 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <h3 style={{ fontSize: 18 }}>Suggestions de changement</h3>
              <button onClick={() => setShowChangeModal(false)} style={{ background: 'transparent', border: 'none', fontSize: 18 }}>✕</button>
            </div>
            <div style={{ marginTop: 8, color: '#374151', fontSize: 14 }}>Choisissez une option de remplacement ; cela pré-remplira le formulaire d'achat pour finaliser l'échange.</div>
            <div style={{ marginTop: 10 }}>
              <GeolocationMockSuggestions max={3} onSelect={confirmChange} />
            </div>
            <div style={{ marginTop: 10, textAlign: 'right' }}>
              <button className="logout-button" onClick={() => setShowChangeModal(false)}>Annuler</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
