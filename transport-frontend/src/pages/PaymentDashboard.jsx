import { useState, useEffect } from 'react';
import { useParams, useSearchParams, useNavigate } from 'react-router-dom';
import ticketService from '../api/ticketService';
import paymentService from '../api/paymentService';

export default function PaymentDashboard() {
  const { ticketId } = useParams();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [ticket, setTicket] = useState(null);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [rawSession, setRawSession] = useState(null);
  const [lastPayResponse, setLastPayResponse] = useState(null);
  const [cardName, setCardName] = useState('');
  const [cardNumber, setCardNumber] = useState('');
  const [expiry, setExpiry] = useState('');
  const [cvc, setCvc] = useState('');

  const amount = searchParams.get('amount') || (ticket && ticket.price) || '';

  // If a session param is present, use it to fetch session details
  const sessionId = searchParams.get('session');

  useEffect(() => {
    let mounted = true;
    const loadSession = async () => {
      try {
        if (sessionId) {
          const s = await paymentService.getSession(sessionId);
          if (mounted) {
            // session.metadata contains the original ticket payload as JSON string
            const meta = s.metadata ? JSON.parse(s.metadata) : null;
            if (meta) {
              setTicket({
                id: sessionId,
                routeName: meta.routeId || meta.routeName || '',
                departureStop: meta.departureStop || '',
                arrivalStop: meta.arrivalStop || '',
                travelDate: meta.departureTime || meta.travelDate || ''
              });
            }
          }
        }
      } catch (e) {
        console.debug('Failed to load session', e);
      }
    };
    loadSession();
    return () => { mounted = false; };
  }, [sessionId]);

  useEffect(() => {
    let mounted = true;
    const load = async () => {
      try {
        if (ticketId) {
          const res = await ticketService.getTicket(ticketId);
          if (mounted) setTicket(res);
        }
      } catch (e) {
        console.debug('Failed to load ticket', e);
      }
    };
    load();
    return () => { mounted = false; };
  }, [ticketId]);

  const doPay = async (e) => {
    e.preventDefault();
    setMessage('');
    setLoading(true);
    try {
      // Simulate Stripe payment via payment-service
      const payload = {
        amount: Number(amount) || (ticket && ticket.price) || 0,
        paymentMethod: 'card',
        card: { cardName, cardNumber: '****' + (cardNumber.slice(-4) || ''), expiry, cvc: '***' }
      };
      const targetId = ticketId || sessionId;
      const res = await paymentService.payTicket(targetId, payload);
        // Payment response handled, navigate back to passenger
      setMessage('Paiement réussi — transaction: ' + (res.transactionId || res.id || 'ok'));
      // Give backend a moment to process events; then redirect to passenger tickets
      setTimeout(() => navigate('/passenger'), 1200);
    } catch (err) {
      setMessage('Échec du paiement : ' + (err.message || err.toString()));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="card" style={{ maxWidth: 760 }}>
      <h3>Paiement du ticket</h3>
      <div style={{ marginTop: 12 }}>
        {ticket ? (
          <div>
            {!sessionId && ticket.id ? <div><strong>Ticket #</strong> {ticket.id}</div> : null}
            <div><strong>Trajet</strong> {ticket.routeName} — {ticket.departureStop} → {ticket.arrivalStop}</div>
            <div><strong>Date</strong> {ticket.travelDate}</div>
            <div style={{ marginTop: 8 }}><strong>Montant</strong> {amount} DH</div>
          </div>
        ) : <div style={{ color: '#6b7280' }}>Chargement du ticket...</div>}

        <form onSubmit={doPay} style={{ marginTop: 14 }}>
          <div className="input-group">
            <label>Nom sur la carte</label>
            <input className="form-input" value={cardName} onChange={(e)=>setCardName(e.target.value)} placeholder="Nom complet" />
          </div>
          <div className="input-group">
            <label>Numéro de carte</label>
            <input className="form-input" value={cardNumber} onChange={(e)=>setCardNumber(e.target.value)} placeholder="4242 4242 4242 4242" />
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <div className="input-group" style={{ flex: 1 }}>
              <label>Expiry</label>
              <input className="form-input" value={expiry} onChange={(e)=>setExpiry(e.target.value)} placeholder="MM/YY" />
            </div>
            <div className="input-group" style={{ width: 120 }}>
              <label>CVC</label>
              <input className="form-input" value={cvc} onChange={(e)=>setCvc(e.target.value)} placeholder="123" />
            </div>
          </div>

          <div style={{ marginTop: 12, display: 'flex', gap: 8 }}>
            <button className="primary-button" type="submit" disabled={loading}>{loading ? 'Paiement...' : `Payer ${amount} DH`}</button>
            <button type="button" className="logout-button" onClick={()=>navigate('/passenger')}>Annuler</button>
          </div>
        </form>

        {message && <div className="info-box" style={{ marginTop: 12 }}>{message}</div>}
        {/* Debug panel to show raw session and last pay response for troubleshooting */}
        <div style={{ marginTop: 12 }}>
          {/* Debug panels removed to keep payment UI minimal */}
        </div>
      </div>
    </div>
  );
}
