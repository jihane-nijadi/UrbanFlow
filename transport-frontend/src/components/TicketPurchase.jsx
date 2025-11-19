import { useState, useEffect } from 'react';
import routeService from '../api/routeService';
import ticketService from '../api/ticketService';
import geolocationService from '../api/geolocationService';
import '../styles/App.css';

// keep fakeResults as fallback if services are unavailable

const fakeResults = [
  { id: 'r1', bus: 'Express 12', from: 'Central Station', to: 'Airport', departure: '2025-11-19T08:30:00', arrival: '2025-11-19T09:10:00', price: 4.5, seats: 12 },
  { id: 'r2', bus: 'Ligne 5', from: 'Main Street', to: 'Université', departure: '2025-11-19T09:00:00', arrival: '2025-11-19T09:40:00', price: 2.0, seats: 5 },
  { id: 'r3', bus: 'InterCity', from: 'East Terminal', to: 'City B', departure: '2025-11-19T14:00:00', arrival: '2025-11-19T15:45:00', price: 6.25, seats: 20 }
];

export default function TicketPurchase() {
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [departureDate, setDepartureDate] = useState('');
  const [time, setTime] = useState('');
  const [quantity, setQuantity] = useState(1);
  const [stops, setStops] = useState([]);
  
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [results, setResults] = useState([]);
  const [selectedRoute, setSelectedRoute] = useState(null);

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
        setResults(withMocks);
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
        setResults(withMocksArr);
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
        passengerEmail: user.email || 'demo@example.com'
      };

      const res = await ticketService.purchaseTicket(payload);
      setMessage("Achat réussi — réf : " + (res.confirmation || res.id || 'ok'));
      setSelectedRoute(route.id);
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

  // Ensure each line has the desired scheduled times by adding mock schedule entries
  const addMissingSchedules = (list) => {
    const desiredTimes = ['09:00','10:30','12:00','14:00','16:00'];
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
      desiredTimes.forEach(time => {
        if (!existing.has(time)) {
          const depDate = parseToDate(time) || parseToDate(new Date().toISOString().slice(0,10) + 'T' + time);
          let arr = '';
          if (depDate) {
            const arrDate = new Date(depDate.getTime() + 30 * 60 * 1000);
            arr = `${pad(arrDate.getHours())}:${pad(arrDate.getMinutes())}`;
          }
          const newId = `${template.id || template.bus || lineKey}|${time}|mock`;
          const newItem = {
            id: newId,
            bus: template.bus || lineKey,
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
          <label>Heure</label>
          <input className="form-input" type="time" value={time} onChange={(e)=>setTime(e.target.value)} />
        </div>
        <div className="input-group">
          <label>Quantité</label>
          <input className="form-input" type="number" min={1} value={quantity} onChange={(e)=>setQuantity(parseInt(e.target.value)||1)} />
        </div>
      </div>

      <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 12 }}>
        <div style={{ display: 'flex', gap: 8 }}>
          <button aria-label="Rechercher les trajets" className="primary-button" onClick={doSearch} disabled={loading}>{loading ? 'Recherche...' : 'Rechercher'}</button>
          <button aria-label="Effacer les filtres" className="logout-button" onClick={()=>{ setFrom(''); setTo(''); setDepartureDate(''); setTime(''); setResults([]); }}>{'Effacer'}</button>
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
                  <div style={{ fontWeight: 800, fontSize: '1rem' }}>{r.bus} <span style={{ fontSize: 12, color: '#6b7280', marginLeft: 8 }}>{formatDisplayId(r)}</span></div>
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
