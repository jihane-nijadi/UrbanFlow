import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import routeService from '../api/routeService';
import ticketService from '../api/ticketService';
import paymentService from '../api/paymentService';
import geolocationService from '../api/geolocationService';
import '../styles/App.css';
import { fakeResults } from '../data/fakeResults';

export default function TicketPurchase() {
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [departureDate, setDepartureDate] = useState('');
  const [quantity, setQuantity] = useState(1);
  const [stops, setStops] = useState([]);
  
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [results, setResults] = useState([]);
  const [selectedRoute, setSelectedRoute] = useState(null);
  const navigate = useNavigate();

  const getAuthHeaders = () => {
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    const headers = {};
    const token = user && (user.token || user.accessToken || user.authToken);
    if (token) headers['Authorization'] = `Bearer ${token}`;
    return headers;
  };

  const doSearch = async () => {
    setMessage('');
    setResults([]);
    setLoading(true);
    try {
      const filters = { departureStopId: from, arrivalStopId: to };
      const res = await routeService.searchRoutes(filters);
      if (res && Array.isArray(res.availableBuses)) {
        // helper to convert possible LocalTime object or string into a HH:MM[:SS] string
        const formatScheduleTime = (val) => {
          if (!val && val !== 0) return '';
          if (typeof val === 'string') return val;
          if (typeof val === 'object' && val !== null) {
            const hh = String(val.hour != null ? val.hour : (val.H || 0)).padStart(2,'0');
            const mm = String(val.minute != null ? val.minute : (val.M || 0)).padStart(2,'0');
            const ssRaw = (val.second != null ? val.second : (val.S != null ? val.S : 0));
            const ss = String(ssRaw).padStart(2,'0');
            return `${hh}:${mm}:${ss}`;
          }
          return String(val);
        };

        const normalized = res.availableBuses.map((s, idx) => {
          // Prefer scheduledDepartureTime if present (added by geolocation mock), fall back to others
          const rawDep = s.scheduledDepartureTime || s.estimatedDepartureTime || s.departureTime || s.departure || '';
          const depStr = formatScheduleTime(rawDep);
          // compute arrival as departure + 30 minutes (display-only) when we have a parsed departure
          const depDate = parseToDate(depStr) || parseToDate(new Date().toISOString().slice(0,10) + 'T' + depStr);
          let arrStr = '';
          if (depDate) {
            const arrDate = new Date(depDate.getTime() + 30 * 60 * 1000);
            arrStr = `${pad(arrDate.getHours())}:${pad(arrDate.getMinutes())}`;
          } else {
            // fallback to any estimated arrival time present
            arrStr = formatScheduleTime(s.scheduledArrivalTime || s.estimatedArrivalTime || s.arrivalTime || s.arrival || '');
          }
          // Ensure unique id per scheduled instance so multiple buses on same line appear.
          const instanceId = s.busId || `${s.lineId || 'LINE'}|${depStr || 'unspecified'}|${idx}`;
          return {
            id: instanceId,
            bus: s.lineId || s.busId || 'Bus',
            from: (s.departureStop && (s.departureStop.name || s.departureStop.stopId)) || (res.departureStop && (res.departureStop.name || res.departureStop.stopId)) || '',
            to: (s.arrivalStop && (s.arrivalStop.name || s.arrivalStop.stopId)) || (res.arrivalStop && (res.arrivalStop.name || res.arrivalStop.stopId)) || '',
            departure: depStr || new Date().toISOString(),
            arrival: arrStr || formatScheduleTime(s.estimatedArrivalTime ? s.estimatedArrivalTime : s.arrivalTime) || new Date().toISOString(),
            price: s.price || 0,
            seats: s.availableSeats || s.seats || 50,
            raw: s
          };
        });

        // Use normalized results as provided by the backend (no artificial staggering).
        const deduped = dedupeByDepArr(normalized);
        const withMocks = addMissingSchedules(deduped);
        setResults(filterOutSpecificTime(withMocks));
      } else if (Array.isArray(res)) {
        // If the route-service returned a plain array, normalize times (handle object LocalTime shapes)
        const formatScheduleTime = (val) => {
          if (!val && val !== 0) return '';
          if (typeof val === 'string') return val;
          if (typeof val === 'object' && val !== null) {
            const hh = String(val.hour != null ? val.hour : (val.H || 0)).padStart(2,'0');
            const mm = String(val.minute != null ? val.minute : (val.M || 0)).padStart(2,'0');
            const ssRaw = (val.second != null ? val.second : (val.S != null ? val.S : 0));
            const ss = String(ssRaw).padStart(2,'0');
            return `${hh}:${mm}:${ss}`;
          }
          return String(val);
        };

        // Normalize all entries and keep them (allow multiple schedules per destination)
        const normalizedArray = res.map((s, idx) => {
          const depRaw = s.scheduledDepartureTime || s.departure || s.estimatedDepartureTime || s.departureTime || '';
          const dep = formatScheduleTime(depRaw);
          let arr = formatScheduleTime(s.scheduledArrivalTime || s.arrival || s.estimatedArrivalTime || s.arrivalTime || '');
          const depDate = parseToDate(dep) || parseToDate(new Date().toISOString().slice(0,10) + 'T' + dep);
          if (depDate) {
            const arrDate = new Date(depDate.getTime() + 30 * 60 * 1000);
            arr = `${pad(arrDate.getHours())}:${pad(arrDate.getMinutes())}`;
          }
          const instanceId = s.busId || `${s.lineId || 'LINE'}|${dep || 'unspecified'}|${idx}`;
          return { ...s, id: instanceId, departure: dep, arrival: arr };
        });

        // Use normalized array results directly but ensure desired schedule times exist per line.
        const dedupedArr = dedupeByDepArr(normalizedArray);
        const withMocksArr = addMissingSchedules(dedupedArr);
        setResults(filterOutSpecificTime(withMocksArr));
      } else {
        const fromName = (stops.find(x => x.stopId === from) || {}).name || '';
        const toName = (stops.find(x => x.stopId === to) || {}).name || '';
        setResults(fakeResults.filter(r => (!from || r.from.toLowerCase().includes(fromName.toLowerCase())) && (!to || r.to.toLowerCase().includes(toName.toLowerCase()))));
      }
    } catch (e) {
      console.debug('search failed, using fake results', e);
      const fromName = (stops.find(x => x.stopId === from) || {}).name || '';
      const toName = (stops.find(x => x.stopId === to) || {}).name || '';
      setResults(fakeResults.filter(r => (!from || r.from.toLowerCase().includes(fromName.toLowerCase())) && (!to || r.to.toLowerCase().includes(toName.toLowerCase()))));
    } finally { setLoading(false); }
  };



  useEffect(() => {
    let mounted = true;
    const loadStops = async () => {
      try {
        const s = await geolocationService.getAllStops();
        if (mounted && Array.isArray(s)) setStops(s);
      } catch (err) {
        console.debug('Failed to load stops from geolocation service', err);
      }
    };
    loadStops();
    // Check for a prefill payload (when user clicked 'Changer' on an existing ticket)
    try {
      const raw = localStorage.getItem('uf_prefill_ticket');
      if (raw) {
        const pf = JSON.parse(raw);
        if (pf) {
          if (pf.departureStop) setFrom(pf.departureStop);
          if (pf.arrivalStop) setTo(pf.arrivalStop);
          if (pf.departureTime && typeof pf.departureTime === 'string') {
            // try to extract date part
            const m = pf.departureTime.match(/^(\d{4}-\d{2}-\d{2})/);
            if (m) setDepartureDate(m[1]);
          }
          if (pf.quantity) setQuantity(pf.quantity);
        }
        localStorage.removeItem('uf_prefill_ticket');
      }
    } catch (e) {
      // ignore
    }
    return () => { mounted = false; };
  }, []);

  const doPurchase = async (route) => {
    setMessage('');
    setLoading(true);
    try {
      const user = JSON.parse(localStorage.getItem('user') || '{}');
      const routeId = route.id || (route.raw && route.raw.lineId) || null;
      const scheduleId = route.raw && (route.raw.scheduleId || route.raw.schedule_id) || null;

      const buildIsoFromDateAndTime = (dateStr, timeStr) => {
        if (!dateStr || !timeStr) return null;
        const t = String(timeStr).trim();
        const timeMatch = t.match(/^(\d{2}:\d{2}(?::\d{2})?)/);
        if (!timeMatch) return null;
        const timePart = timeMatch[1];
        return `${dateStr}T${timePart}`;
      };

      let departureTimeIso = null;
      if (typeof route.departure === 'string' && /^\d{4}-\d{2}-\d{2}T/.test(route.departure)) {
        departureTimeIso = route.departure;
      } else if (departureDate && typeof route.departure === 'string') {
        departureTimeIso = buildIsoFromDateAndTime(departureDate, route.departure);
      } else if (departureDate && route.raw && (route.raw.estimatedDepartureTime || route.raw.departureTime)) {
        departureTimeIso = buildIsoFromDateAndTime(departureDate, route.raw.estimatedDepartureTime || route.raw.departureTime);
      }

      const payload = {
        routeId,
        scheduleId,
        departureStop: route.from,
        arrivalStop: route.to,
        departureTime: departureTimeIso || route.departure,
        quantity,
        price: (7 * (quantity || 1)),
        passengerEmail: user.email || 'demo@example.com'
      };

      // include bus info so payment sessions/tickets can display the chosen bus number
      try {
        payload.bus = route.bus || formatDisplayId(route) || (route.raw && (route.raw.bus || route.raw.busId)) || null;
      } catch (e) { payload.bus = null; }

      // Create a payment session in payment-service instead of creating the ticket immediately
      // payment-service will create the actual ticket only after payment is completed
      // Create a payment session in payment-service. Do NOT fall back to creating the ticket directly.
      const res = await paymentService.createSession(payload);
      console.debug('purchase response', res);
      // After session creation, navigate to payment dashboard
      const createdId = res && (res.sessionId || res.id || res.ticketId || res.confirmation || res["id"]);
      setSelectedRoute(route.id);
      // If backend returned a direct payment URL, open the payment flow inside the frontend dashboard
      if (res && (res.paymentUrl || res.sessionId)) {
        try {
          // navigate to passenger dashboard and open Payments tab with session id and amount
          // Prefer explicit sessionId returned by the payment-service. Never follow a paymentUrl returned by backend.
          const sessionId = res.sessionId || res.id || null;
          const amt = (res.amount || res.price || payload.price || (7 * (quantity || 1))).toString();
          if (!sessionId) throw new Error('No session id returned from payment service');
          // Open the standalone Payment Dashboard in a new tab so the user only sees the payment form
          const url = `${window.location.origin}/payment?session=${encodeURIComponent(sessionId)}&amount=${encodeURIComponent(amt)}`;
          // open payments dashboard in a new browser tab
          window.open(url, '_blank');
          return;
        } catch (pe) {
          setMessage('Achat créé mais impossible d' + "\"" + 'ouvrir le paiement: ' + (pe.message || pe.toString()));
          return;
        }
      }

      if (createdId) {
        const amt = (res.amount || res.price || payload.price || (7 * (quantity || 1))).toString();
        const sessionParam = encodeURIComponent(createdId);
        const url = `${window.location.origin}/passenger?session=${sessionParam}&amount=${encodeURIComponent(amt)}`;
        window.open(url, '_blank');
        return;
      }
      setMessage("Échec lors de la création de la session de paiement — réessayez plus tard");
    } catch (err) {
      setMessage("Échec de l'achat : " + (err.message || err.toString()));
    } finally {
      setLoading(false);
    }
  };

  // Parse a value (ISO datetime or time-only) into a Date object using the selected departureDate when necessary
  const parseToDate = (value) => {
    if (!value) return null;
    try {
      if (typeof value === 'string') {
        // ISO datetime
        if (/^\d{4}-\d{2}-\d{2}T/.test(value)) {
          const d = new Date(value);
          if (!isNaN(d.getTime())) return d;
        }
        // time-only like HH:MM or HH:MM:SS
        const tm = value.match(/^(\d{2}:\d{2}(?::\d{2})?)$/);
        if (tm) {
          const baseDate = departureDate && departureDate.length === 10 ? departureDate : (new Date()).toISOString().slice(0,10);
          const iso = `${baseDate}T${tm[1]}`;
          const d2 = new Date(iso);
          if (!isNaN(d2.getTime())) return d2;
        }
      }
      // If value is an object e.g. LocalTime serialized as {hour:8, minute:30, second:0}
      if (typeof value === 'object' && value !== null) {
        const hasHour = Object.prototype.hasOwnProperty.call(value, 'hour');
        const hasMinute = Object.prototype.hasOwnProperty.call(value, 'minute');
        if (hasHour && hasMinute) {
          const h = Number(value.hour || 0);
          const m = Number(value.minute || 0);
          const s = Number(value.second || value.nano ? Math.floor((value.nano || 0) / 1e9) : 0);
          const baseDate = departureDate && departureDate.length === 10 ? departureDate : (new Date()).toISOString().slice(0,10);
          const iso = `${baseDate}T${String(h).padStart(2,'0')}:${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')}`;
          const d3 = new Date(iso);
          if (!isNaN(d3.getTime())) return d3;
        }
      }
      // try native Date parsing
      const d = new Date(value);
      if (!isNaN(d.getTime())) return d;
    } catch (e) {
      // ignore
    }
    return null;
  };

  // Return HH:MM string for display and de-dup keys
  const pad = (n) => String(n).padStart(2, '0');
  const formatHourMinute = (value) => {
    if (!value && value !== 0) return '';
    // If it's already a time-like string HH:MM or HH:MM:SS
    if (typeof value === 'string') {
      const tm = value.match(/^(\d{2}:\d{2})/);
      if (tm) return tm[1];
    }
    const d = parseToDate(value);
    if (d) {
      return `${pad(d.getHours())}:${pad(d.getMinutes())}`;
    }
    return String(value).slice(0,5);
  };

  const formatDateTime = (dt) => formatHourMinute(dt);

  // Remove duplicate schedules that have the exact same departure+arrival times
  const dedupeByDepArr = (list) => {
    const seen = new Set();
    const out = [];
    for (const item of list) {
      // Key only on from, to, departure and arrival (collapse exact time duplicates)
      const key = `${item.from || ''}|${item.to || ''}|${formatHourMinute(item.departure)}|${formatHourMinute(item.arrival)}`;
      if (!seen.has(key)) {
        seen.add(key);
        out.push(item);
      }
    }
    return out;
  };

  // Format the small id shown on the card. Strip any generated suffix like "|09:00|mock"
  const formatDisplayId = (item) => {
    const raw = item && (item.id || item.bus || '');
    if (!raw) return '';
    return String(raw).split('|')[0];
  };

  // Detect synthetic/mock entries by looking for a |mock suffix in id or raw.busId
  const isMockItem = (item) => {
    if (!item) return false;
    const id = String(item.id || '');
    if (id.toLowerCase().includes('|mock')) return true;
    const rawBusId = item.raw && (item.raw.busId || item.raw.id || '');
    if (String(rawBusId).toLowerCase().includes('|mock')) return true;
    if (item.raw && item.raw.mock === true) return true;
    return false;
  };

  // Generate a Casablanca-like bus number for synthetic schedules.
  // Deterministic, produces numbers in range 1..199 based on lineKey and time.
  const generateCasablancaBusNumber = (lineKey, time, seq = 0) => {
    const key = String(lineKey || '') + '|' + String(time || '') + '|' + String(seq || 0);
    let hash = 0;
    for (let i = 0; i < key.length; i++) {
      hash = ((hash << 5) - hash) + key.charCodeAt(i);
      hash |= 0;
    }
    const base = Math.abs(hash) % 150 + 1; // 1..150
    const hourMatch = String(time || '').match(/^(\d{2}):/);
    const hour = hourMatch ? Number(hourMatch[1]) : 0;
    const num = ((base + hour + seq) % 199) + 1; // ensure 1..199
    return `Bus ${num}`;
  };

  // Allocate a unique bus number not already used in this result set.
  const allocateUniqueBusNumber = (lineKey, time, usedNumbers, seqStart = 0) => {
    let seq = seqStart;
    for (let attempts = 0; attempts < 500; attempts++) {
      const label = generateCasablancaBusNumber(lineKey, time, seq);
      const m = label.match(/(\d+)$/);
      const n = m ? Number(m[1]) : null;
      if (n == null || !usedNumbers.has(n)) {
        if (n != null) usedNumbers.add(n);
        return label;
      }
      seq += 1;
    }
    // fallback: find first free number 1..199
    for (let v = 1; v <= 199; v++) {
      if (!usedNumbers.has(v)) { usedNumbers.add(v); return `Bus ${v}`; }
    }
    return generateCasablancaBusNumber(lineKey, time, seqStart);
  };

  // Remove any specific unwanted schedule entries (e.g. depart 09:00 arrive 09:30)
  const filterOutSpecificTime = (list) => {
    if (!Array.isArray(list)) return list;
    return list.filter(item => {
      try {
        const dep = formatHourMinute(item.departure);
        const arr = formatHourMinute(item.arrival);
        // filter out exact match 09:00 -> 09:30
        if (dep === '09:00' && arr === '09:30') return false;
        // filter out any entries whose duration cannot be computed
        const dur = computeDuration(item.departure, item.arrival);
        if (dur === 'N/A') return false;
      } catch (e) {
        // ignore and keep item
      }
      return true;
    });
  };

  // Ensure each line has the desired scheduled times by adding mock schedule entries
  const addMissingSchedules = (list) => {
    // ensure we include a 09:30 departure (arrives ~10:00) and other useful slots
    const desiredTimes = ['09:30','10:30','12:00','14:00','16:00'];
    const byLine = new Map();
    for (const it of list) {
      const lineKey = (it.raw && (it.raw.lineId || it.raw.busId)) || it.bus || 'LINE';
      if (!byLine.has(lineKey)) byLine.set(lineKey, []);
      byLine.get(lineKey).push(it);
    }

    const out = [...list];
    byLine.forEach((items, lineKey) => {
      if (!items || items.length === 0) return;
      const existing = new Set(items.map(i => formatHourMinute(i.departure)));
      const template = items[0];
      // collect used numeric bus numbers from existing items to avoid collisions
      const usedNumbers = new Set();
      for (const it of items) {
        try {
          const m = String(it.bus).match(/(\d+)$/);
          if (m) usedNumbers.add(Number(m[1]));
        } catch (e) {}
      }
      desiredTimes.forEach((time, timeIdx) => {
        if (!existing.has(time)) {
          const depDate = parseToDate(time) || parseToDate(new Date().toISOString().slice(0,10) + 'T' + time);
          let arr = '';
          if (depDate) {
            const arrDate = new Date(depDate.getTime() + 30 * 60 * 1000);
            arr = `${pad(arrDate.getHours())}:${pad(arrDate.getMinutes())}`;
          }
          const newId = `${template.id || template.bus || lineKey}|${time}|mock`;
          // allocate a unique Casablanca-like bus number for the synthetic instance
          const busLabel = allocateUniqueBusNumber(lineKey, time, usedNumbers, timeIdx);
          const newItem = {
            id: newId,
            bus: busLabel,
            from: template.from || '',
            to: template.to || '',
            departure: time,
            arrival: arr,
            price: template.price || 7,
            seats: template.seats || 50,
            raw: { lineId: lineKey, busId: newId, scheduledDepartureTime: time }
          };
          out.push(newItem);
        }
      });
    });
    return out;
  };

  // Compute duration between departure and arrival and return human readable string
  const computeDuration = (depValue, arrValue) => {
    try {
      const sd = parseToDate(depValue);
      const ed = parseToDate(arrValue);
      if (!sd || !ed) return 'N/A';

      let diffMs = ed - sd;
      // If arrival is before departure, assume arrival is on next day (overnight)
      if (diffMs < 0) diffMs += 24 * 60 * 60 * 1000;

      const totalMinutes = Math.round(diffMs / 60000);
      const hours = Math.floor(totalMinutes / 60);
      const minutes = totalMinutes % 60;
      if (hours > 0) return `${hours}h ${minutes}m`;
      return `${minutes}m`;
    } catch (e) {
      return 'N/A';
    }
  };

  return (
    <div className="purchase-card" style={{ maxWidth: 1100, minHeight: '64vh' }}>
      <div className="purchase-header">
        <h3 style={{ margin: 0, color: '#111827' }}>Acheter des tickets</h3>
      </div>

      <div className="search-grid" style={{ marginTop: 12 }}>
        <div className="input-group">
          <label>Départ</label>
          <select className="form-select" value={from} onChange={(e)=>setFrom(e.target.value)}>
            <option value="">-- Départ --</option>
            {stops.length ? stops.map(s => <option key={s.stopId} value={s.stopId}>{s.name || s.stopId}</option>) : (
              // fallback to names from fake results
              fakeResults.map((r, idx) => <option key={idx} value={r.from}>{r.from}</option>)
            )}
          </select>
        </div>
        <div className="input-group">
          <label>Arrivée</label>
          <select className="form-select" value={to} onChange={(e)=>setTo(e.target.value)}>
            <option value="">-- Arrivée --</option>
            {stops.length ? stops.map(s => <option key={s.stopId} value={s.stopId}>{s.name || s.stopId}</option>) : (
              fakeResults.map((r, idx) => <option key={idx} value={r.to}>{r.to}</option>)
            )}
          </select>
        </div>
        <div className="input-group">
          <label>Date départ</label>
          <input className="form-input" type="date" value={departureDate} onChange={(e)=>setDepartureDate(e.target.value)} />
        </div>
        
        <div className="input-group">
          <label>Quantité</label>
          <input className="form-input" type="number" min={1} value={quantity} onChange={(e)=>setQuantity(parseInt(e.target.value)||1)} />
        </div>
      </div>

      <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 12 }}>
        <div style={{ display: 'flex', gap: 8 }}>
          <button aria-label="Rechercher les trajets" className="primary-button" onClick={doSearch} disabled={loading}>{loading ? 'Recherche...' : 'Rechercher'}</button>
          <button aria-label="Effacer les filtres" className="logout-button" onClick={()=>{ setFrom(''); setTo(''); setDepartureDate(''); setResults([]); }}>{'Effacer'}</button>
        </div>
        {results.length ? (
          <div style={{ color: '#6b7280', alignSelf: 'center' }}>{`${results.length} résultats`}</div>
        ) : null}
      </div>

      <div style={{ marginTop: 16 }}>
        <div className="results-grid">
          {results.map(r => (
            <div key={r.id} className="result-card">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div>
                  <div style={{ fontWeight: 800, fontSize: '1rem', display: 'flex', alignItems: 'center', gap: 8 }}>
                    <div>{r.bus}</div>
                    <span style={{ fontSize: 12, color: '#6b7280', marginLeft: 8 }}>{formatDisplayId(r)}</span>
                  </div>
                  <div style={{ color: '#374151', marginTop: 6 }}>{r.from} → {r.to}</div>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontWeight: 800, fontSize: '1rem', color: '#111827' }}>
                    {`${(7 * (quantity || 1)).toFixed(2)} DH`}
                    <div style={{ fontSize: 12, color: '#9ca3af' }}>({`7 DH / ticket x ${quantity || 1}`})</div>
                  </div>
                  <div style={{ color: '#6b7280', fontSize: 12 }}>{r.seats} places</div>
                </div>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 10, alignItems: 'center' }}>
                <div style={{ color: '#6b7280', fontSize: 13 }}>
                  Départ : {formatDateTime(r.departure)} · Arrivée : {formatDateTime(r.arrival)}
                  <div style={{ fontSize: 12, color: '#9ca3af', marginTop: 6 }}>
                    Durée : {computeDuration(r.departure, r.arrival)}
                  </div>
                </div>
                <div style={{ display: 'flex', gap: 8 }}>
                    <button aria-label={`Acheter billet pour ${r.bus} ${formatDisplayId(r)}`} className="primary-button" onClick={() => doPurchase(r)} disabled={loading || selectedRoute === r.id}>{selectedRoute === r.id ? 'Acheté' : 'Acheter'}</button>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {message && <div className="info-box">{message}</div>}
    </div>
  );
}
