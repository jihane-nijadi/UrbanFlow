// src/components/BusTracker.jsx
import { useEffect, useState } from "react";
import { MapContainer, TileLayer, Marker, Popup } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";

// Fix icônes par défaut de Leaflet
import markerIcon2x from "leaflet/dist/images/marker-icon-2x.png";
import markerIcon from "leaflet/dist/images/marker-icon.png";
import markerShadow from "leaflet/dist/images/marker-shadow.png";

delete L.Icon.Default.prototype._getIconUrl;

L.Icon.Default.mergeOptions({
    iconRetinaUrl: markerIcon2x,
    iconUrl: markerIcon,
    shadowUrl: markerShadow,
});

export default function BusTracker() {
    const [busLocations, setBusLocations] = useState([]);

    // URL de ton geolocation-service
    const API_URL = "/api/location/all";


    useEffect(() => {
        // Fonction pour récupérer les positions
        const fetchBusLocations = async () => {
            try {
                const response = await fetch(API_URL);
                const data = await response.json();
                setBusLocations(data);
            } catch (error) {
                console.error("Erreur lors de la récupération des positions des bus:", error);
            }
        };

        fetchBusLocations();

        // Rafraîchir toutes les 10 secondes
        const interval = setInterval(fetchBusLocations, 10000);
        return () => clearInterval(interval);
    }, []);

    // Centre de la carte (ajuste selon ta ville)
    const center = [33.5731, -7.5898]; // Casablanca

    return (
        <div style={{ height: "600px", width: "100%" }}>
            <MapContainer center={center} zoom={13} scrollWheelZoom style={{ height: "100%", width: "100%" }}>
                <TileLayer
                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                    attribution='&copy; <a href="http://osm.org/copyright">OpenStreetMap</a>'
                />
                {busLocations.map((bus) => (
                    <Marker
                        key={bus.vehicleId}
                        position={[bus.latitude, bus.longitude]}
                    >
                        <Popup>
                            Bus: {bus.vehicleId} <br />
                            Route: {bus.routeId || "N/A"} <br />
                            Status: {bus.status || "ACTIVE"}
                        </Popup>
                    </Marker>
                ))}
            </MapContainer>
        </div>
    );
}
