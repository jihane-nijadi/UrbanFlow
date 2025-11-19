import React, { useEffect, useState, useRef } from 'react';
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import geolocationService from '../api/geolocationService';

// Fix default icon paths for CRA
try {
  delete L.Icon.Default.prototype._getIconUrl;
  L.Icon.Default.mergeOptions({
    iconRetinaUrl: require('leaflet/dist/images/marker-icon-2x.png'),
    iconUrl: require('leaflet/dist/images/marker-icon.png'),
    shadowUrl: require('leaflet/dist/images/marker-shadow.png')
  });
} catch (e) {
  // ignore in environments where require is not available
}

export default function MapView({ center = [33.589886, -7.603869], zoom = 12 }) {
  const [buses, setBuses] = useState({}); // keyed by busId
  const eventSourceRef = useRef(null);
  const pollingRef = useRef(null);

  // Helper to upsert bus updates
  const upsertUpdates = (updates) => {
    setBuses(prev => {
      const copy = { ...prev };
      for (const u of updates) {
        if (!u || !u.busId) continue;
        copy[u.busId] = {
          busId: u.busId,
          lineId: u.lineId,
          lat: Number(u.lat),
          lon: Number(u.lon),
          speed: u.speed,
          heading: u.heading,
          ts: u.ts,
          scheduledDepartureTime: u.scheduledDepartureTime
        };
      }
      return copy;
    });
  };

  useEffect(() => {
    let mounted = true;

    // initial load
    (async () => {
      try {
        const list = await geolocationService.getAllBuses();
        if (!mounted) return;
        if (Array.isArray(list)) {
          upsertUpdates(list.map(u => ({
            busId: u.busId || u.id || u.bus_id,
            lineId: u.lineId || u.line_id,
            lat: u.lat,
            lon: u.lon,
            speed: u.speed,
            heading: u.heading,
            ts: u.ts,
            scheduledDepartureTime: u.scheduledDepartureTime || u.scheduled_departure_time
          })));
        }
      } catch (e) {
        console.debug('Failed to load initial buses for map', e);
      }
    })();

    // Setup SSE connection
    const host = window.location.hostname;
    const API_HOST = host === 'localhost' || host === '127.0.0.1' ? 'http://localhost:8091' : 'http://geolocation-service:8091';
    const SSE_URL = (process.env.REACT_APP_GEOLOCATION_API_URL || API_HOST) + '/api/realtime/stream';

    try {
      const es = new EventSource(SSE_URL);
      eventSourceRef.current = es;
      const handler = (evt) => {
        try {
          const payload = JSON.parse(evt.data);
          // payload is expected to be an array of BusUpdate
          if (Array.isArray(payload)) {
            upsertUpdates(payload.map(u => ({
              busId: u.busId,
              lineId: u.lineId,
              lat: u.lat,
              lon: u.lon,
              speed: u.speed,
              heading: u.heading,
              ts: u.ts,
              scheduledDepartureTime: u.scheduledDepartureTime
            })));
          }
        } catch (err) {
          console.debug('Invalid SSE payload', err);
        }
      };
      es.addEventListener('bus_updates', handler);
      es.onerror = (err) => {
        console.debug('SSE error, falling back to polling', err);
        es.close();
        eventSourceRef.current = null;
        // start polling
        if (!pollingRef.current) {
          pollingRef.current = setInterval(async () => {
            try {
              const list = await geolocationService.getAllBuses();
              if (Array.isArray(list)) upsertUpdates(list);
            } catch (e) {
              // ignore
            }
          }, 5000);
        }
      };
    } catch (e) {
      // EventSource not available or blocked, fallback to polling
      pollingRef.current = setInterval(async () => {
        try {
          const list = await geolocationService.getAllBuses();
          if (Array.isArray(list)) upsertUpdates(list);
        } catch (err) {
          // ignore
        }
      }, 5000);
    }

    return () => {
      mounted = false;
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
        eventSourceRef.current = null;
      }
      if (pollingRef.current) {
        clearInterval(pollingRef.current);
        pollingRef.current = null;
      }
    };
  }, []);

  const markers = Object.values(buses || {});

  return (
    <div style={{ height: '70vh', width: '100%' }}>
      <MapContainer center={center} zoom={zoom} style={{ height: '100%', width: '100%' }}>
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        {markers.map(m => (
          <Marker key={m.busId} position={[Number(m.lat) || 0, Number(m.lon) || 0]}>
            <Popup>
              <div style={{ minWidth: 180 }}>
                <div style={{ fontWeight: 700 }}>{m.lineId || 'Bus'}</div>
                <div>ID: {m.busId}</div>
                <div>Coord: {Number(m.lat).toFixed(5)}, {Number(m.lon).toFixed(5)}</div>
                <div>Speed: {m.speed ? `${m.speed} km/h` : 'N/A'}</div>
                <div>Heading: {m.heading ?? 'N/A'}</div>
                <div>Scheduled: {m.scheduledDepartureTime || '—'}</div>
              </div>
            </Popup>
          </Marker>
        ))}
      </MapContainer>
    </div>
  );
}
