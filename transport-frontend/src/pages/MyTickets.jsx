import React, { useEffect, useState } from 'react';
import ticketService from '../api/ticketService';

export default function MyTickets() {
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [processingId, setProcessingId] = useState(null);

  const user = JSON.parse(localStorage.getItem('user') || '{}');
  const userId = user && (user.id || user.userId || user._id);

  useEffect(() => {
    let mounted = true;
    setLoading(true);
    setError(null);
    ticketService.getMyTickets(userId)
      .then(data => {
        if (!mounted) return;
        // Expecting array of tickets
        setTickets(Array.isArray(data) ? data : (data.tickets || []));
        setLoading(false);
      })
      .catch(err => {
        if (!mounted) return;
        setError(err.message || 'Impossible de récupérer vos tickets');
        setLoading(false);
      });
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

  const formatDate = (iso) => {
    try {
      if (!iso) return '-';
      const d = new Date(iso);
      return d.toLocaleString();
    } catch (e) { return iso; }
  };

  return (
    <div className="card" style={{ maxWidth: 1100, margin: '0 auto' }}>
      <h3>Mes tickets</h3>
      <p style={{ color: '#6b7280', marginTop: 8 }}>Affiche vos tickets achetés et options (annulation, QR, etc.).</p>

      {loading && <div style={{ marginTop: 18 }}>Chargement de vos tickets...</div>}
      {error && <div style={{ marginTop: 18, color: '#9b1c1c' }}>{error}</div>}

      {!loading && !error && tickets && tickets.length === 0 && (
        <div style={{ marginTop: 18, color: '#6b7280' }}>Vous n'avez aucun ticket actif pour le moment.</div>
      )}

      {!loading && tickets && tickets.length > 0 && (
        <div className="results-grid" style={{ marginTop: 18 }}>
          {tickets.map((t) => {
            const id = t.id || t.ticketId || t.ticket_id;
            const title = (t.ticketType && t.ticketType.name) || (t.type) || `Ticket ${id ? id.slice(0,8) : ''}`;
            const status = (t.status || t.ticketStatus || '').toUpperCase();
            const purchaseDate = t.purchaseDate || t.createdAt || t.purchase_date;
            const expiry = t.expiryDate || t.expiry_date || t.expiryDateTime;

            return (
              <div className="result-card" key={id}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <div>
                    <div style={{ fontSize: 16, fontWeight: 800 }}>{title}</div>
                    <div className="meta" style={{ marginTop: 6 }}>{t.qr_code ? `QR: ${t.qr_code.slice?.(0, 12) || ''}` : ''}</div>
                    <div className="meta" style={{ marginTop: 6 }}>Acheté: {formatDate(purchaseDate)}</div>
                    <div className="meta" style={{ marginTop: 6 }}>Valide jusqu'à: {formatDate(expiry)}</div>
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    <div style={{ marginBottom: 8 }}>
                      <span className="status-badge" style={{ backgroundColor: status === 'ACTIVE' ? '#10b981' : (status === 'CANCELLED' ? '#f87171' : '#6b7280') }}>{status || '—'}</span>
                    </div>
                    <div style={{ marginTop: 12 }}>
                      {status === 'ACTIVE' && (
                        <button
                          className="buy-button"
                          onClick={() => doCancel(id)}
                          disabled={processingId === id}
                          aria-label={`Annuler ticket ${id}`}
                        >{processingId === id ? 'Annulation…' : 'Annuler'}</button>
                      )}
                    </div>
                  </div>
                </div>

                <div style={{ marginTop: 12, display: 'flex', gap: 8 }}>
                  <a href={`${window.location.origin}/api/tickets/${id}/qrcode`} target="_blank" rel="noreferrer">
                    <button className="primary-button" style={{ padding: '0.55rem 0.9rem' }}>Voir QR</button>
                  </a>
                  <a href={`${window.location.origin}/api/tickets/${id}/qrcode`} target="_blank" rel="noreferrer" download>
                    <button className="primary-button" style={{ padding: '0.55rem 0.9rem', background: '#6b7280' }}>Télécharger</button>
                  </a>
                  <a href={`mailto:support@urbanflow.local?subject=Problème ticket ${id}`}>
                    <button className="primary-button" style={{ padding: '0.55rem 0.9rem', background: '#e5e7eb', color: '#111827' }}>Contacter support</button>
                  </a>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
