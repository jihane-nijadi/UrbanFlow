import { useEffect, useState, useRef } from "react";
import { MapContainer, TileLayer, Marker, Popup, useMap, Polyline } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import geolocationService from "../api/geolocationService";
import { Bus, MapPin } from "lucide-react";

// Fix pour les icônes par défaut de Leaflet avec React
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png",
  iconUrl: "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png",
  shadowUrl: "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png",
});

/**
 * Crée une icône de bus moderne et stylée.
 * L'icône est un cercle avec une icône SVG de bus qui suit la direction du bus.
 * 
 * @param {number} heading - Direction du bus en degrés (0-360)
 * @returns {L.DivIcon} Icône Leaflet personnalisée
 */
const createBusIcon = (heading) => {
  // SVG d'une icône de bus moderne
  const busSvg = `
    <svg width="32" height="32" viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg">
      <!-- Cercle de fond avec ombre -->
      <circle cx="16" cy="16" r="15" fill="#3b82f6" stroke="white" stroke-width="2.5"/>
      <!-- Corps du bus -->
      <rect x="8" y="12" width="16" height="8" rx="1" fill="white" opacity="0.95"/>
      <!-- Fenêtres du bus -->
      <rect x="10" y="13" width="3" height="2.5" rx="0.5" fill="#1e40af"/>
      <rect x="14" y="13" width="3" height="2.5" rx="0.5" fill="#1e40af"/>
      <rect x="18" y="13" width="3" height="2.5" rx="0.5" fill="#1e40af"/>
      <!-- Porte du bus -->
      <rect x="21" y="15" width="2" height="5" rx="0.5" fill="#1e40af"/>
      <!-- Roues -->
      <circle cx="11" cy="21" r="1.5" fill="#1e293b"/>
      <circle cx="21" cy="21" r="1.5" fill="#1e293b"/>
      <!-- Flèche de direction (pointe vers le haut par défaut) -->
      <path d="M 16 8 L 12 12 L 20 12 Z" fill="white" opacity="0.9"/>
    </svg>
  `;

  return L.divIcon({
    className: "custom-bus-icon",
    html: `<div style="
      transform: rotate(${heading || 0}deg);
      transition: transform 0.3s ease-out;
      filter: drop-shadow(0 2px 4px rgba(0,0,0,0.3));
    ">${busSvg}</div>`,
    iconSize: [32, 32],
    iconAnchor: [16, 16],
  });
};

/**
 * Crée une icône pour les stations (gare de départ ou d'arrivée).
 * 
 * @param {boolean} isDeparture - true pour gare de départ (vert), false pour arrivée (rouge)
 * @returns {L.DivIcon} Icône Leaflet personnalisée
 */
const createStationIcon = (isDeparture) => {
  return L.divIcon({
    className: "station-icon",
    html: `<div style="
      background-color: ${isDeparture ? '#10b981' : '#ef4444'};
      width: 24px;
      height: 24px;
      border-radius: 50%;
      border: 3px solid white;
      box-shadow: 0 2px 4px rgba(0,0,0,0.3);
      display: flex;
      align-items: center;
      justify-content: center;
    ">
      <div style="
        width: 10px;
        height: 10px;
        background-color: white;
        border-radius: 50%;
      "></div>
    </div>`,
    iconSize: [24, 24],
    iconAnchor: [12, 12],
  });
};

/**
 * Composant pour mettre à jour la vue de la carte lorsque les bus changent.
 * Ajuste automatiquement le zoom pour afficher tous les bus actifs.
 */
function MapUpdater({ buses }) {
  const map = useMap();
  const [initialized, setInitialized] = useState(false);

  useEffect(() => {
    if (buses.length > 0 && !initialized) {
      const bounds = L.latLngBounds(buses.map((bus) => [bus.lat, bus.lon]));
      map.fitBounds(bounds, { padding: [50, 50] });
      setInitialized(true);
    }
  }, [buses, map, initialized]);

  return null;
}

/**
 * Composant principal pour le suivi des bus en temps réel.
 * Affiche une carte interactive avec les bus se déplaçant le long de routes réelles.
 */
export default function BusTracker() {
  const [buses, setBuses] = useState([]);
  const [busStops, setBusStops] = useState([]); // Liste des arrêts
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedBus, setSelectedBus] = useState(null);
  const [selectedStop, setSelectedStop] = useState(null);
  const [busRoutes, setBusRoutes] = useState(new Map()); // busId -> route data
  const [busHistory, setBusHistory] = useState(new Map()); // busId -> position history
  const unsubscribeRef = useRef(null);

  // Centre par défaut (Casablanca, Morocco)
  const defaultCenter = [33.5731, -7.5898];
  const defaultZoom = 12;

  // Routes d'exemple - en production, cela viendrait d'une API de routes
  const sampleRoutes = {
    "BUS-001": {
      departure: { name: "Gare de Départ", lat: 33.5631, lon: -7.5998 },
      arrival: { name: "Gare d'Arrivée", lat: 33.5831, lon: -7.5798 },
      lineId: "LINE-1"
    },
    "BUS-002": {
      departure: { name: "Gare Centre", lat: 33.5731, lon: -7.6098 },
      arrival: { name: "Gare Nord", lat: 33.5931, lon: -7.5698 },
      lineId: "LINE-2"
    },
    "BUS-003": {
      departure: { name: "Gare Sud", lat: 33.5531, lon: -7.5898 },
      arrival: { name: "Gare Est", lat: 33.5731, lon: -7.5498 },
      lineId: "LINE-1"
    },
    "BUS-004": {
      departure: { name: "Gare Ouest", lat: 33.5731, lon: -7.6298 },
      arrival: { name: "Gare Centre", lat: 33.5731, lon: -7.5898 },
      lineId: "LINE-3"
    },
    "BUS-005": {
      departure: { name: "Gare Aéroport", lat: 33.5431, lon: -7.6198 },
      arrival: { name: "Gare Ville", lat: 33.5731, lon: -7.5898 },
      lineId: "LINE-2"
    }
  };

  useEffect(() => {
    /**
     * Charge les données initiales des bus.
     * Récupère la liste des bus actifs, leurs routes et leur historique de positions.
     */
    const loadInitialData = async () => {
      try {
        setLoading(true);
        
        // Charger les bus et les arrêts en parallèle
        const [activeBuses, stops] = await Promise.all([
          geolocationService.getActiveBuses(),
          geolocationService.getAllStops()
        ]);
        
        setBuses(activeBuses);
        setBusStops(stops);
        
        // Charger les données de route et l'historique de position pour chaque bus
        const routesMap = new Map();
        const historyMap = new Map();
        
        for (const bus of activeBuses) {
          // Assigner la route si disponible
          if (sampleRoutes[bus.busId]) {
            routesMap.set(bus.busId, sampleRoutes[bus.busId]);
          }
          
          // Charger l'historique de position pour dessiner le chemin parcouru
          try {
            const history = await geolocationService.getBusHistory(bus.busId, 50);
            if (history && history.length > 0) {
              historyMap.set(bus.busId, history.map(pos => [pos.lat, pos.lon]));
            }
          } catch (err) {
            console.warn(`Could not load history for bus ${bus.busId}:`, err);
          }
        }
        
        setBusRoutes(routesMap);
        setBusHistory(historyMap);
        setError(null);
      } catch (err) {
        console.error("Error loading data:", err);
        setError("Failed to load bus data. Please try again later.");
      } finally {
        setLoading(false);
      }
    };

    loadInitialData();

    /**
     * S'abonne aux mises à jour en temps réel via Server-Sent Events (SSE).
     * Met à jour automatiquement les positions des bus lorsqu'elles changent.
     */
    const unsubscribe = geolocationService.subscribeToRealtimeUpdates((updates) => {
      setBuses((prevBuses) => {
        const busMap = new Map(prevBuses.map((bus) => [bus.busId, bus]));
        
        updates.forEach((update) => {
          const existingBus = busMap.get(update.busId);
          if (existingBus) {
            // Mettre à jour le bus existant
            busMap.set(update.busId, {
              ...existingBus,
              lat: update.lat,
              lon: update.lon,
              speed: update.speed,
              heading: update.heading,
              lastUpdatedAt: new Date(update.ts).toISOString(),
            });
          } else {
            // Ajouter un nouveau bus
            busMap.set(update.busId, {
              busId: update.busId,
              lineId: update.lineId,
              lat: update.lat,
              lon: update.lon,
              speed: update.speed,
              heading: update.heading,
              lastUpdatedAt: new Date(update.ts).toISOString(),
            });
            
            // Assigner la route pour le nouveau bus
            if (sampleRoutes[update.busId]) {
              setBusRoutes(prev => new Map(prev).set(update.busId, sampleRoutes[update.busId]));
            }
          }
          
          // Mettre à jour l'historique de position
          setBusHistory(prev => {
            const newHistory = new Map(prev);
            const currentHistory = newHistory.get(update.busId) || [];
            const newPosition = [update.lat, update.lon];
            
            // Ajouter la nouvelle position, garder les 50 dernières positions
            const updatedHistory = [...currentHistory, newPosition].slice(-50);
            newHistory.set(update.busId, updatedHistory);
            return newHistory;
          });
        });

        return Array.from(busMap.values());
      });
    });

    unsubscribeRef.current = unsubscribe;

    // Nettoyage lors du démontage du composant
    return () => {
      if (unsubscribeRef.current) {
        unsubscribeRef.current();
      }
    };
  }, []);

  // Affichage du chargement
  if (loading) {
    return (
      <div style={{ 
        display: "flex", 
        justifyContent: "center", 
        alignItems: "center", 
        height: "600px",
        fontSize: "18px",
        color: "#666"
      }}>
        Loading bus locations...
      </div>
    );
  }

  // Affichage des erreurs
  if (error) {
    return (
      <div style={{ 
        display: "flex", 
        justifyContent: "center", 
        alignItems: "center", 
        height: "600px",
        fontSize: "18px",
        color: "#dc2626",
        flexDirection: "column",
        gap: "10px"
      }}>
        <p>{error}</p>
        <button
          onClick={() => window.location.reload()}
          style={{
            padding: "10px 20px",
            backgroundColor: "#3b82f6",
            color: "white",
            border: "none",
            borderRadius: "5px",
            cursor: "pointer"
          }}
        >
          Retry
        </button>
      </div>
    );
  }

  return (
    <div style={{ width: "100%", height: "100%" }}>
      {/* En-tête avec titre et compteur de bus actifs */}
      <div style={{ 
        marginBottom: "20px", 
        display: "flex", 
        justifyContent: "space-between",
        alignItems: "center"
      }}>
        <h2 style={{ margin: 0, fontSize: "24px", fontWeight: "600" }}>
          Real-Time Bus Tracking
        </h2>
        <div style={{ 
          display: "flex", 
          alignItems: "center", 
          gap: "10px",
          padding: "8px 16px",
          backgroundColor: "#f0f9ff",
          borderRadius: "8px",
          fontSize: "14px"
        }}>
          <div style={{
            width: "10px",
            height: "10px",
            borderRadius: "50%",
            backgroundColor: "#10b981",
            animation: "pulse 2s infinite"
          }}></div>
          <span>{buses.length} active bus{buses.length !== 1 ? "es" : ""}</span>
        </div>
      </div>

      {/* Carte Leaflet */}
      <div style={{ 
        height: "600px", 
        borderRadius: "12px", 
        overflow: "hidden",
        border: "2px solid #e5e7eb",
        boxShadow: "0 4px 6px rgba(0, 0, 0, 0.1)"
      }}>
        <MapContainer
          center={buses.length > 0 ? [buses[0].lat, buses[0].lon] : defaultCenter}
          zoom={defaultZoom}
          style={{ height: "100%", width: "100%" }}
          scrollWheelZoom={true}
        >
          {/* Couche de tuiles OpenStreetMap */}
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          <MapUpdater buses={buses} />
          
          {/* Dessiner les routes pour chaque bus */}
          {Array.from(busRoutes.entries()).map(([busId, route]) => {
            const history = busHistory.get(busId) || [];
            // Utiliser l'historique si disponible, sinon utiliser la route directe
            const routePath = history.length > 0 
              ? history 
              : [[route.departure.lat, route.departure.lon], [route.arrival.lat, route.arrival.lon]];
            
            return (
              <div key={`route-${busId}`}>
                {/* Ligne de route (bleue, suit les routes réelles) */}
                <Polyline
                  positions={routePath}
                  pathOptions={{
                    color: "#1e40af",
                    weight: 6,
                    opacity: 0.8,
                  }}
                />
              </div>
            );
          })}
          
          {/* Afficher tous les arrêts avec leurs informations détaillées */}
          {busStops.map((stop) => (
            <Marker
              key={stop.stopId}
              position={[stop.lat, stop.lon]}
              icon={createStationIcon(stop.type === "departure")}
              eventHandlers={{
                click: () => setSelectedStop(stop),
              }}
            >
              <Popup>
                <div style={{ minWidth: "250px" }}>
                  <h3 style={{ margin: "0 0 10px 0", fontSize: "16px", fontWeight: "600", color: "#1e40af" }}>
                    🚉 {stop.name}
                  </h3>
                  <p style={{ margin: "5px 0", fontSize: "14px", color: "#666" }}>
                    <strong>Adresse:</strong> {stop.address}
                  </p>
                  {stop.description && (
                    <p style={{ margin: "5px 0", fontSize: "13px", color: "#555", fontStyle: "italic" }}>
                      {stop.description}
                    </p>
                  )}
                  {stop.type && (
                    <p style={{ margin: "5px 0", fontSize: "13px" }}>
                      <strong>Type:</strong> 
                      <span style={{ 
                        color: stop.type === "departure" ? "#10b981" : stop.type === "arrival" ? "#ef4444" : "#3b82f6",
                        marginLeft: "5px"
                      }}>
                        {stop.type === "departure" ? "Départ" : stop.type === "arrival" ? "Arrivée" : "Intermédiaire"}
                      </span>
                    </p>
                  )}
                  {stop.lineIds && stop.lineIds.length > 0 && (
                    <p style={{ margin: "5px 0", fontSize: "13px", color: "#666" }}>
                      <strong>Lignes:</strong> {stop.lineIds.join(", ")}
                    </p>
                  )}
                  {stop.facilities && (
                    <p style={{ margin: "5px 0", fontSize: "13px", color: "#666" }}>
                      <strong>Équipements:</strong> {stop.facilities}
                    </p>
                  )}
                  {stop.capacity && (
                    <p style={{ margin: "5px 0", fontSize: "13px", color: "#666" }}>
                      <strong>Capacité:</strong> {stop.capacity} personnes
                    </p>
                  )}
                </div>
              </Popup>
            </Marker>
          ))}
          
          {/* Marqueurs des bus avec la nouvelle icône moderne */}
          {buses.map((bus) => (
            <Marker
              key={bus.busId}
              position={[bus.lat, bus.lon]}
              icon={createBusIcon(bus.heading)}
              eventHandlers={{
                click: () => setSelectedBus(bus),
              }}
            >
              <Popup>
                <div style={{ minWidth: "250px" }}>
                  <h3 style={{ margin: "0 0 10px 0", fontSize: "18px", fontWeight: "600", color: "#1e40af" }}>
                    🚌 Bus {bus.busId}
                  </h3>
                  {bus.lineId && (
                    <div style={{ 
                      margin: "8px 0", 
                      padding: "5px 10px", 
                      backgroundColor: "#eff6ff", 
                      borderRadius: "5px",
                      display: "inline-block"
                    }}>
                      <strong style={{ fontSize: "14px", color: "#1e40af" }}>Ligne: {bus.lineId}</strong>
                    </div>
                  )}
                  {busRoutes.get(bus.busId) && (
                    <div style={{ marginTop: "10px", paddingTop: "10px", borderTop: "1px solid #e5e7eb" }}>
                      <p style={{ margin: "5px 0", fontSize: "14px", color: "#666" }}>
                        <strong>📍 Départ:</strong> {busRoutes.get(bus.busId).departure.name}
                      </p>
                      <p style={{ margin: "5px 0", fontSize: "14px", color: "#666" }}>
                        <strong>🎯 Destination:</strong> {busRoutes.get(bus.busId).arrival.name}
                      </p>
                    </div>
                  )}
                  <div style={{ marginTop: "10px", paddingTop: "10px", borderTop: "1px solid #e5e7eb" }}>
                    {bus.speed !== null && bus.speed !== undefined && (
                      <p style={{ margin: "5px 0", fontSize: "14px", color: "#666" }}>
                        <strong>⚡ Vitesse:</strong> <span style={{ color: "#10b981", fontWeight: "600" }}>{Math.round(bus.speed * 3.6)} km/h</span>
                      </p>
                    )}
                    {bus.heading !== null && bus.heading !== undefined && (
                      <p style={{ margin: "5px 0", fontSize: "14px", color: "#666" }}>
                        <strong>🧭 Direction:</strong> {bus.heading}°
                      </p>
                    )}
                  </div>
                  {bus.lastUpdatedAt && (
                    <p style={{ margin: "10px 0 0 0", fontSize: "12px", color: "#999", fontStyle: "italic" }}>
                      Dernière mise à jour: {new Date(bus.lastUpdatedAt).toLocaleTimeString()}
                    </p>
                  )}
                </div>
              </Popup>
            </Marker>
          ))}
        </MapContainer>
      </div>

      {/* Liste des bus actifs */}
      {buses.length > 0 && (
        <div style={{ 
          marginTop: "20px",
          backgroundColor: "white",
          borderRadius: "12px",
          padding: "20px",
          boxShadow: "0 2px 4px rgba(0, 0, 0, 0.1)"
        }}>
          <h3 style={{ margin: "0 0 15px 0", fontSize: "18px", fontWeight: "600" }}>
            Active Buses
          </h3>
          <div style={{ 
            display: "grid", 
            gridTemplateColumns: "repeat(auto-fill, minmax(250px, 1fr))",
            gap: "15px"
          }}>
            {buses.map((bus) => (
              <div
                key={bus.busId}
                onClick={() => setSelectedBus(bus)}
                style={{
                  padding: "15px",
                  border: selectedBus?.busId === bus.busId ? "2px solid #3b82f6" : "1px solid #e5e7eb",
                  borderRadius: "8px",
                  cursor: "pointer",
                  backgroundColor: selectedBus?.busId === bus.busId ? "#eff6ff" : "white",
                  transition: "all 0.2s"
                }}
              >
                <div style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "8px" }}>
                  <Bus size={20} color="#3b82f6" />
                  <strong style={{ fontSize: "16px" }}>Bus {bus.busId}</strong>
                </div>
                {bus.lineId && (
                  <p style={{ margin: "5px 0", fontSize: "14px", color: "#666" }}>
                    Line: {bus.lineId}
                  </p>
                )}
                {bus.speed !== null && bus.speed !== undefined && (
                  <p style={{ margin: "5px 0", fontSize: "14px", color: "#666" }}>
                    Speed: {Math.round(bus.speed * 3.6)} km/h
                  </p>
                )}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
