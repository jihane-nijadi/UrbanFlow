
import { useEffect, useState } from "react";
import { MapContainer, TileLayer, Marker, Popup, Polyline, useMap } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { 
  MapPin, 
  Bus, 
  Clock, 
  Navigation, 
  Search, 
  ArrowRight, 
  Route,
  Zap,
  TrendingUp,
  CheckCircle2,
  Loader2,
  AlertCircle,
  Star,
  X
} from "lucide-react";

// Fix pour les icônes par défaut de Leaflet
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png",
  iconUrl: "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png",
  shadowUrl: "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png",
});

// Mock du service API
const routeService = {
  getAllStops: async () => {
    return [
      { stopId: "STOP-1", name: "Place Mohammed V", address: "Centre-ville, Rabat", lat: 33.9716, lon: -6.8498 },
      { stopId: "STOP-2", name: "Agdal", address: "Quartier Agdal, Rabat", lat: 33.9716, lon: -6.8598 },
      { stopId: "STOP-3", name: "Hassan", address: "Tour Hassan, Rabat", lat: 34.0209, lon: -6.8247 },
      { stopId: "STOP-4", name: "Orangers", address: "Les Orangers, Rabat", lat: 33.9816, lon: -6.8698 },
      { stopId: "STOP-5", name: "Hay Riad", address: "Hay Riad, Rabat", lat: 33.9516, lon: -6.8798 },
    ];
  },
  searchRoutes: async (from, to) => {
    await new Promise(resolve => setTimeout(resolve, 1000));
    return {
      availableBuses: [
        {
          busId: "BUS-001",
          lineId: "LINE-1",
          estimatedDepartureTime: "14:30",
          estimatedArrivalTime: "15:00",
          estimatedDurationMinutes: 30,
          currentSpeed: 35,
          departureStop: { stopId: from, name: "Départ", lat: 33.9716, lon: -6.8498 },
          arrivalStop: { stopId: to, name: "Arrivée", lat: 34.0209, lon: -6.8247 },
          intermediateStops: [
            { name: "Arrêt 1", address: "Rue 1", lat: 33.9866, lon: -6.8373 },
            { name: "Arrêt 2", address: "Rue 2", lat: 34.0037, lon: -6.8310 }
          ],
          routePoints: [
            { lat: 33.9716, lon: -6.8498 },
            { lat: 33.9866, lon: -6.8373 },
            { lat: 34.0037, lon: -6.8310 },
            { lat: 34.0209, lon: -6.8247 }
          ]
        },
        {
          busId: "BUS-002",
          lineId: "LINE-2",
          estimatedDepartureTime: "14:45",
          estimatedArrivalTime: "15:20",
          estimatedDurationMinutes: 35,
          currentSpeed: 32,
          departureStop: { stopId: from, name: "Départ", lat: 33.9716, lon: -6.8498 },
          arrivalStop: { stopId: to, name: "Arrivée", lat: 34.0209, lon: -6.8247 },
          intermediateStops: [],
          routePoints: [
            { lat: 33.9716, lon: -6.8498 },
            { lat: 34.0209, lon: -6.8247 }
          ]
        }
      ]
    };
  }
};

const createStopIcon = (color = "#3b82f6", size = 20) => {
  return L.divIcon({
    className: "stop-icon",
    html: `<div style="
      background: linear-gradient(135deg, ${color} 0%, ${color}dd 100%);
      width: ${size}px;
      height: ${size}px;
      border-radius: 50%;
      border: 3px solid white;
      box-shadow: 0 4px 12px rgba(0,0,0,0.3), 0 0 0 4px ${color}33;
      animation: pulse 2s infinite;
    ">
      <div style="
        position: absolute;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        width: 8px;
        height: 8px;
        background: white;
        border-radius: 50%;
      "></div>
    </div>`,
    iconSize: [size, size],
    iconAnchor: [size / 2, size / 2],
  });
};

function MapBoundsUpdater({ bounds }) {
  const map = useMap();
  
  useEffect(() => {
    if (bounds && bounds.length > 0) {
      const latLngBounds = L.latLngBounds(bounds);
      map.fitBounds(latLngBounds, { padding: [50, 50], maxZoom: 15 });
    }
  }, [bounds, map]);

  return null;
}

function Trajet() {
  const [stops, setStops] = useState([]);
  const [departureStop, setDepartureStop] = useState(null);
  const [arrivalStop, setArrivalStop] = useState(null);
  const [departureInput, setDepartureInput] = useState("");
  const [arrivalInput, setArrivalInput] = useState("");
  const [filteredDepartureStops, setFilteredDepartureStops] = useState([]);
  const [filteredArrivalStops, setFilteredArrivalStops] = useState([]);
  const [showDepartureDropdown, setShowDepartureDropdown] = useState(false);
  const [showArrivalDropdown, setShowArrivalDropdown] = useState(false);
  const [availableBuses, setAvailableBuses] = useState([]);
  const [selectedBus, setSelectedBus] = useState(null);
  const [routePoints, setRoutePoints] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    loadStops();
  }, []);

  const loadStops = async () => {
    try {
      const data = await routeService.getAllStops();
      setStops(data);
    } catch (err) {
      setError("Erreur lors du chargement des arrêts");
      console.error(err);
    }
  };

  const handleDepartureChange = (value) => {
    setDepartureInput(value);
    if (value.length > 0) {
      setFilteredDepartureStops(
        stops.filter(stop => 
          stop.name.toLowerCase().includes(value.toLowerCase()) ||
          stop.address.toLowerCase().includes(value.toLowerCase())
        )
      );
      setShowDepartureDropdown(true);
    } else {
      setFilteredDepartureStops([]);
      setShowDepartureDropdown(false);
      setDepartureStop(null);
    }
  };

  const handleArrivalChange = (value) => {
    setArrivalInput(value);
    if (value.length > 0) {
      setFilteredArrivalStops(
        stops.filter(stop => 
          stop.name.toLowerCase().includes(value.toLowerCase()) ||
          stop.address.toLowerCase().includes(value.toLowerCase())
        )
      );
      setShowArrivalDropdown(true);
    } else {
      setFilteredArrivalStops([]);
      setShowArrivalDropdown(false);
      setArrivalStop(null);
    }
  };

  const handleDepartureSelect = (stop) => {
    setDepartureStop(stop);
    setDepartureInput(stop.name);
    setShowDepartureDropdown(false);
  };

  const handleArrivalSelect = (stop) => {
    setArrivalStop(stop);
    setArrivalInput(stop.name);
    setShowArrivalDropdown(false);
  };

  const handleSearch = async () => {
    if (!departureStop || !arrivalStop) {
      setError("Veuillez sélectionner un point de départ et d'arrivée");
      return;
    }

    if (departureStop.stopId === arrivalStop.stopId) {
      setError("Le point de départ et d'arrivée doivent être différents");
      return;
    }

    setLoading(true);
    setError(null);
    setSelectedBus(null);
    setRoutePoints([]);

    try {
      const response = await routeService.searchRoutes(
        departureStop.stopId,
        arrivalStop.stopId
      );
      setAvailableBuses(response.availableBuses || []);
      
      if (response.availableBuses.length === 0) {
        setError("Aucun bus disponible pour cet itinéraire");
      }
    } catch (err) {
      setError("Erreur lors de la recherche");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleBusSelect = (busSchedule) => {
    setSelectedBus(busSchedule);
    
    if (busSchedule.routePoints && busSchedule.routePoints.length > 0) {
      const points = busSchedule.routePoints.map((point) => [point.lat, point.lon]);
      setRoutePoints(points);
    }
  };

  const getMapBounds = () => {
    const bounds = [];
    if (departureStop) bounds.push([departureStop.lat, departureStop.lon]);
    if (arrivalStop) bounds.push([arrivalStop.lat, arrivalStop.lon]);
    if (selectedBus?.routePoints) {
      selectedBus.routePoints.forEach((point) => bounds.push([point.lat, point.lon]));
    }
    return bounds;
  };

  const formatTime = (time) => {
    if (!time) return "N/A";
    return typeof time === "string" ? time.substring(0, 5) : time;
  };

  const getLineColor = (lineId) => {
    const colors = {
      "LINE-1": "#3b82f6",
      "LINE-2": "#10b981",
      "LINE-3": "#f59e0b",
    };
    return colors[lineId] || "#6366f1";
  };

  return (
    <div className="flex flex-col h-screen w-full bg-gradient-to-br from-indigo-50 via-purple-50 to-pink-50 relative overflow-hidden">
      {/* Background decoration */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute -top-40 -right-40 w-80 h-80 bg-gradient-to-br from-blue-400/20 to-purple-400/20 rounded-full blur-3xl animate-blob"></div>
        <div className="absolute -bottom-40 -left-40 w-80 h-80 bg-gradient-to-br from-pink-400/20 to-indigo-400/20 rounded-full blur-3xl animate-blob animation-delay-2000"></div>
        <div className="absolute top-1/2 left-1/2 w-80 h-80 bg-gradient-to-br from-purple-400/20 to-blue-400/20 rounded-full blur-3xl animate-blob animation-delay-4000"></div>
      </div>

      {/* Header */}
      <div className="relative bg-white/80 backdrop-blur-xl border-b border-white/20 shadow-lg">
        <div className="p-6">
          <div className="flex items-center gap-4">
            <div className="relative">
              <div className="absolute inset-0 bg-gradient-to-r from-blue-600 to-purple-600 rounded-2xl blur-lg opacity-50 animate-pulse"></div>
              <div className="relative p-3 bg-gradient-to-br from-blue-600 via-indigo-600 to-purple-600 rounded-2xl shadow-xl">
                <Navigation className="w-7 h-7 text-white" />
              </div>
            </div>
            <div>
              <h1 className="text-3xl font-bold bg-gradient-to-r from-blue-600 via-purple-600 to-pink-600 bg-clip-text text-transparent">
                Planificateur d'itinéraires
              </h1>
              <p className="text-sm text-slate-600 mt-1">Découvrez votre trajet idéal en quelques clics ✨</p>
            </div>
          </div>
        </div>
      </div>

      <div className="flex-1 flex overflow-hidden relative">
        {/* Left Panel */}
        <div className="w-96 bg-white/70 backdrop-blur-xl shadow-2xl overflow-y-auto border-r border-white/30 relative z-10">
          <div className="p-6 space-y-6">
            {/* Search Card */}
            <div className="bg-white/90 backdrop-blur-sm rounded-2xl border border-white/50 shadow-xl overflow-hidden transform transition-all hover:shadow-2xl hover:scale-[1.02]">
              <div className="p-5 bg-gradient-to-r from-blue-500/10 via-purple-500/10 to-pink-500/10 border-b border-white/30">
                <h2 className="font-bold text-slate-900 flex items-center gap-2">
                  <div className="p-2 bg-gradient-to-br from-blue-500 to-purple-600 rounded-lg shadow-lg">
                    <MapPin className="w-5 h-5 text-white" />
                  </div>
                  <span className="bg-gradient-to-r from-blue-600 to-purple-600 bg-clip-text text-transparent">
                    Détails du voyage
                  </span>
                </h2>
              </div>
              
              <div className="p-5 space-y-5">
                {/* Departure Input */}
                <div className="relative group">
                  <label className="text-sm font-bold text-slate-700 mb-2 block flex items-center gap-2">
                    <div className="w-2 h-2 bg-gradient-to-r from-green-500 to-emerald-500 rounded-full animate-pulse"></div>
                    Arrêt de départ
                  </label>
                  <div className="relative">
                    <input
                      type="text"
                      placeholder="Où partez-vous ? 🚀"
                      value={departureInput}
                      onChange={(e) => handleDepartureChange(e.target.value)}
                      onFocus={() => departureInput && setShowDepartureDropdown(true)}
                      className="w-full px-4 py-3 pl-11 rounded-xl border-2 border-slate-200 bg-white/90 text-slate-900 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all shadow-sm hover:shadow-md"
                    />
                    <div className="absolute left-3 top-1/2 -translate-y-1/2 p-1 bg-green-100 rounded-lg">
                      <MapPin className="w-4 h-4 text-green-600" />
                    </div>
                    {departureInput && (
                      <button
                        onClick={() => {
                          setDepartureInput("");
                          setDepartureStop(null);
                          setShowDepartureDropdown(false);
                        }}
                        className="absolute right-3 top-1/2 -translate-y-1/2 p-1 hover:bg-slate-100 rounded-lg transition-colors"
                      >
                        <X className="w-4 h-4 text-slate-400 hover:text-slate-600" />
                      </button>
                    )}
                  </div>
                  
                  {showDepartureDropdown && filteredDepartureStops.length > 0 && (
                    <div className="absolute top-full left-0 right-0 mt-2 bg-white/95 backdrop-blur-xl border-2 border-blue-200 rounded-2xl shadow-2xl z-20 max-h-64 overflow-y-auto">
                      {filteredDepartureStops.map((stop, idx) => (
                        <button
                          key={stop.stopId}
                          onClick={() => handleDepartureSelect(stop)}
                          className="w-full text-left px-4 py-3 hover:bg-gradient-to-r hover:from-blue-50 hover:to-purple-50 flex items-start gap-3 border-b border-slate-100 last:border-b-0 transition-all group"
                          style={{ animationDelay: `${idx * 50}ms` }}
                        >
                          <div className="p-2 bg-blue-100 rounded-lg group-hover:bg-blue-200 transition-colors">
                            <MapPin className="h-4 w-4 text-blue-600" />
                          </div>
                          <div className="flex-1">
                            <div className="font-semibold text-sm text-slate-900">{stop.name}</div>
                            <div className="text-xs text-slate-500 mt-0.5">{stop.address}</div>
                          </div>
                        </button>
                      ))}
                    </div>
                  )}
                  
                  {departureStop && (
                    <div className="mt-3 p-4 bg-gradient-to-r from-green-50 to-emerald-50 border-2 border-green-200 rounded-xl shadow-md transform transition-all animate-slide-in">
                      <div className="flex items-start gap-3">
                        <div className="p-2 bg-green-500 rounded-lg shadow-lg">
                          <MapPin className="w-4 h-4 text-white" />
                        </div>
                        <div>
                          <p className="text-sm font-bold text-green-900">{departureStop.name}</p>
                          <p className="text-xs text-green-700 mt-1">{departureStop.address}</p>
                        </div>
                      </div>
                    </div>
                  )}
                </div>

                {/* Arrow with animation */}
                <div className="flex justify-center my-3">
                  <div className="relative">
                    <div className="absolute inset-0 bg-gradient-to-r from-blue-600 to-purple-600 rounded-full blur-lg opacity-50 animate-pulse"></div>
                    <div className="relative p-3 bg-gradient-to-r from-blue-600 via-purple-600 to-pink-600 rounded-full shadow-xl animate-bounce-slow">
                      <ArrowRight className="w-5 h-5 text-white" />
                    </div>
                  </div>
                </div>

                {/* Arrival Input */}
                <div className="relative group">
                  <label className="text-sm font-bold text-slate-700 mb-2 block flex items-center gap-2">
                    <div className="w-2 h-2 bg-gradient-to-r from-red-500 to-pink-500 rounded-full animate-pulse"></div>
                    Arrêt d'arrivée
                  </label>
                  <div className="relative">
                    <input
                      type="text"
                      placeholder="Où allez-vous ? 🎯"
                      value={arrivalInput}
                      onChange={(e) => handleArrivalChange(e.target.value)}
                      onFocus={() => arrivalInput && setShowArrivalDropdown(true)}
                      className="w-full px-4 py-3 pl-11 rounded-xl border-2 border-slate-200 bg-white/90 text-slate-900 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-purple-500 transition-all shadow-sm hover:shadow-md"
                    />
                    <div className="absolute left-3 top-1/2 -translate-y-1/2 p-1 bg-red-100 rounded-lg">
                      <MapPin className="w-4 h-4 text-red-600" />
                    </div>
                    {arrivalInput && (
                      <button
                        onClick={() => {
                          setArrivalInput("");
                          setArrivalStop(null);
                          setShowArrivalDropdown(false);
                        }}
                        className="absolute right-3 top-1/2 -translate-y-1/2 p-1 hover:bg-slate-100 rounded-lg transition-colors"
                      >
                        <X className="w-4 h-4 text-slate-400 hover:text-slate-600" />
                      </button>
                    )}
                  </div>
                  
                  {showArrivalDropdown && filteredArrivalStops.length > 0 && (
                    <div className="absolute top-full left-0 right-0 mt-2 bg-white/95 backdrop-blur-xl border-2 border-purple-200 rounded-2xl shadow-2xl z-20 max-h-64 overflow-y-auto">
                      {filteredArrivalStops.map((stop, idx) => (
                        <button
                          key={stop.stopId}
                          onClick={() => handleArrivalSelect(stop)}
                          className="w-full text-left px-4 py-3 hover:bg-gradient-to-r hover:from-purple-50 hover:to-pink-50 flex items-start gap-3 border-b border-slate-100 last:border-b-0 transition-all group"
                          style={{ animationDelay: `${idx * 50}ms` }}
                        >
                          <div className="p-2 bg-purple-100 rounded-lg group-hover:bg-purple-200 transition-colors">
                            <MapPin className="h-4 w-4 text-purple-600" />
                          </div>
                          <div className="flex-1">
                            <div className="font-semibold text-sm text-slate-900">{stop.name}</div>
                            <div className="text-xs text-slate-500 mt-0.5">{stop.address}</div>
                          </div>
                        </button>
                      ))}
                    </div>
                  )}
                  
                  {arrivalStop && (
                    <div className="mt-3 p-4 bg-gradient-to-r from-red-50 to-pink-50 border-2 border-red-200 rounded-xl shadow-md transform transition-all animate-slide-in">
                      <div className="flex items-start gap-3">
                        <div className="p-2 bg-red-500 rounded-lg shadow-lg">
                          <MapPin className="w-4 h-4 text-white" />
                        </div>
                        <div>
                          <p className="text-sm font-bold text-red-900">{arrivalStop.name}</p>
                          <p className="text-xs text-red-700 mt-1">{arrivalStop.address}</p>
                        </div>
                      </div>
                    </div>
                  )}
                </div>

                {/* Search Button */}
                <button
                  onClick={handleSearch}
                  disabled={loading || !departureStop || !arrivalStop}
                  className={`w-full py-4 px-6 rounded-xl font-bold text-white shadow-xl transform transition-all duration-300 ${
                    loading || !departureStop || !arrivalStop
                      ? "bg-slate-400 cursor-not-allowed"
                      : "bg-gradient-to-r from-blue-600 via-purple-600 to-pink-600 hover:scale-105 hover:shadow-2xl active:scale-95 hover:from-blue-700 hover:via-purple-700 hover:to-pink-700"
                  } flex items-center justify-center gap-3 relative overflow-hidden group`}
                >
                  <div className="absolute inset-0 bg-gradient-to-r from-white/0 via-white/20 to-white/0 transform -translate-x-full group-hover:translate-x-full transition-transform duration-1000"></div>
                  {loading ? (
                    <>
                      <Loader2 className="w-5 h-5 animate-spin relative z-10" />
                      <span className="relative z-10">Recherche magique en cours...</span>
                    </>
                  ) : (
                    <>
                      <Search className="w-5 h-5 relative z-10" />
                      <span className="relative z-10">Trouver mon itinéraire ✨</span>
                    </>
                  )}
                </button>
              </div>
            </div>

            {/* Error Message */}
            {error && (
              <div className="bg-gradient-to-r from-red-50 to-pink-50 border-2 border-red-300 rounded-2xl p-5 shadow-xl animate-shake">
                <div className="flex items-start gap-3">
                  <div className="p-2 bg-red-500 rounded-lg shadow-lg">
                    <AlertCircle className="w-5 h-5 text-white" />
                  </div>
                  <div>
                    <p className="text-sm font-bold text-red-900 mb-1">Oups ! 😕</p>
                    <p className="text-sm text-red-800">{error}</p>
                  </div>
                </div>
              </div>
            )}

            {/* Available Routes */}
            {availableBuses.length > 0 && (
              <div className="bg-white/90 backdrop-blur-sm rounded-2xl border border-white/50 shadow-xl overflow-hidden animate-slide-up">
                <div className="p-5 bg-gradient-to-r from-purple-500/10 via-blue-500/10 to-pink-500/10 border-b border-white/30">
                  <div className="flex items-center justify-between">
                    <h2 className="font-bold text-slate-900 flex items-center gap-2">
                      <div className="p-2 bg-gradient-to-br from-purple-500 to-pink-600 rounded-lg shadow-lg">
                        <Clock className="w-5 h-5 text-white" />
                      </div>
                      <span className="bg-gradient-to-r from-purple-600 to-pink-600 bg-clip-text text-transparent">
                        Itinéraires magiques
                      </span>
                    </h2>
                    <div className="relative">
                      <div className="absolute inset-0 bg-gradient-to-r from-blue-500 to-purple-500 rounded-full blur-md opacity-50"></div>
                      <span className="relative px-3 py-1.5 bg-gradient-to-r from-blue-500 to-purple-600 text-white rounded-full text-xs font-bold shadow-lg">
                        {availableBuses.length} trouvé{availableBuses.length > 1 ? 's' : ''}
                      </span>
                    </div>
                  </div>
                </div>
                
                <div className="p-5 space-y-4">
                  {availableBuses.map((busSchedule, idx) => {
                    const lineColor = getLineColor(busSchedule.lineId);
                    const isSelected = selectedBus?.busId === busSchedule.busId;
                    
                    return (
                      <button
                        key={busSchedule.busId}
                        onClick={() => handleBusSelect(busSchedule)}
                        className={`w-full text-left p-5 rounded-2xl border-2 transition-all duration-300 transform hover:scale-[1.02] ${
                          isSelected
                            ? "border-purple-500 bg-gradient-to-r from-purple-50 via-blue-50 to-pink-50 shadow-xl scale-[1.02]"
                            : "border-slate-200 bg-white/80 hover:border-purple-300 hover:shadow-lg"
                        }`}
                        style={{ 
                          animationDelay: `${idx * 100}ms`,
                          animation: 'fadeInUp 0.5s ease-out both'
                        }}
                      >
                        <div className="flex items-start justify-between mb-4">
                          <div className="flex items-center gap-3">
                            <div className="relative">
                              <div className="absolute inset-0 rounded-xl blur-md opacity-50" style={{ backgroundColor: lineColor }}></div>
                              <div 
                                className="relative p-3 rounded-xl text-white shadow-lg"
                                style={{ backgroundColor: lineColor }}
                              >
                                <Bus className="w-6 h-6" />
                              </div>
                            </div>
                            <div>
                              <div className="flex items-center gap-2 mb-1">
                                <span className="font-bold text-slate-900 text-lg">
                                  {busSchedule.busId}
                                </span>
                                {isSelected && (
                                  <div className="relative">
                                    <div className="absolute inset-0 bg-green-500 rounded-full blur-sm animate-pulse"></div>
                                    <CheckCircle2 className="relative w-5 h-5 text-green-600" />
                                  </div>
                                )}
                              </div>
                              <span 
                                className="text-xs font-bold px-3 py-1 rounded-full text-white shadow-md"
                                style={{ backgroundColor: lineColor }}
                              >
                                {busSchedule.lineId}
                              </span>
                            </div>
                          </div>
                          {isSelected && (
                            <Star className="w-6 h-6 text-yellow-400 fill-yellow-400 animate-pulse" />
                          )}
                        </div>

                        <div className="space-y-3 text-sm">
                          <div className="flex items-center gap-3 p-3 bg-green-50 rounded-xl">
                            <div className="p-2 bg-green-500 rounded-lg shadow-md">
                              <Clock className="w-4 h-4 text-white" />
                            </div>
                            <div>
                              <p className="text-xs text-green-700 font-semibold">Départ</p>
                              <p className="font-bold text-green-900">{formatTime(busSchedule.estimatedDepartureTime)}</p>
                            </div>
                          </div>
                          
                          <div className="flex items-center gap-3 p-3 bg-red-50 rounded-xl">
                            <div className="p-2 bg-red-500 rounded-lg shadow-md">
                              <Clock className="w-4 h-4 text-white" />
                            </div>
                            <div>
                              <p className="text-xs text-red-700 font-semibold">Arrivée</p>
                              <p className="font-bold text-red-900">{formatTime(busSchedule.estimatedArrivalTime)}</p>
                            </div>
                          </div>
                          
                          {busSchedule.estimatedDurationMinutes && (
                            <div className="flex items-center gap-3 p-3 bg-blue-50 rounded-xl">
                              <div className="p-2 bg-blue-500 rounded-lg shadow-md">
                                <Zap className="w-4 h-4 text-white" />
                              </div>
                              <div>
                                <p className="text-xs text-blue-700 font-semibold">Durée estimée</p>
                                <p className="font-bold text-blue-900">~{busSchedule.estimatedDurationMinutes} minutes</p>
                              </div>
                            </div>
                          )}
                        </div>
                      </button>
                    );
                  })}
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Map */}
        <div className="flex-1 relative overflow-hidden">
          <div className="absolute inset-0 bg-gradient-to-br from-blue-100/50 via-purple-100/50 to-pink-100/50"></div>
          <MapContainer
            center={[33.9716, -6.8498]}
            zoom={13}
            style={{ height: "100%", width: "100%" }}
            className="rounded-l-3xl shadow-2xl relative z-10"
          >
            <TileLayer
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              attribution='&copy; OpenStreetMap contributors'
            />
            
            <MapBoundsUpdater bounds={getMapBounds()} />

            {departureStop && (
              <Marker
                position={[departureStop.lat, departureStop.lon]}
                icon={createStopIcon("#10b981", 24)}
              >
                <Popup>
                  <div className="p-2">
                    <strong className="text-green-600">Départ</strong>
                    <p className="font-semibold">{departureStop.name}</p>
                    <p className="text-xs text-slate-600">{departureStop.address}</p>
                  </div>
                </Popup>
              </Marker>
            )}

            {arrivalStop && (
              <Marker
                position={[arrivalStop.lat, arrivalStop.lon]}
                icon={createStopIcon("#ef4444", 24)}
              >
                <Popup>
                  <div className="p-2">
                    <strong className="text-red-600">Arrivée</strong>
                    <p className="font-semibold">{arrivalStop.name}</p>
                    <p className="text-xs text-slate-600">{arrivalStop.address}</p>
                  </div>
                </Popup>
              </Marker>
            )}

            {selectedBus?.intermediateStops?.map((stop, idx) => (
              <Marker
                key={idx}
                position={[stop.lat, stop.lon]}
                icon={createStopIcon("#3b82f6", 20)}
              >
                <Popup>
                  <div className="p-2">
                    <strong className="text-blue-600">Arrêt {idx + 1}</strong>
                    <p className="font-semibold">{stop.name}</p>
                    <p className="text-xs text-slate-600">{stop.address}</p>
                  </div>
                </Popup>
              </Marker>
            ))}

            {routePoints.length > 0 && (
              <Polyline
                positions={routePoints}
                pathOptions={{
                  color: "#3b82f6",
                  weight: 4,
                  opacity: 0.8,
                }}
              />
            )}
          </MapContainer>
        </div>
      </div>

      <style>{`
        @keyframes pulse {
          0%, 100% { transform: scale(1); opacity: 1; }
          50% { transform: scale(1.1); opacity: 0.8; }
        }
        
        @keyframes blob {
          0%, 100% { transform: translate(0, 0) scale(1); }
          33% { transform: translate(30px, -50px) scale(1.1); }
          66% { transform: translate(-20px, 20px) scale(0.9); }
        }
        
        @keyframes fadeInUp {
          from {
            opacity: 0;
            transform: translateY(20px);
          }
          to {
            opacity: 1;
            transform: translateY(0);
          }
        }
        
        @keyframes slideIn {
          from {
            opacity: 0;
            transform: translateX(-20px);
          }
          to {
            opacity: 1;
            transform: translateX(0);
          }
        }
        
        @keyframes slideUp {
          from {
            opacity: 0;
            transform: translateY(30px);
          }
          to {
            opacity: 1;
            transform: translateY(0);
          }
        }
        
        @keyframes shake {
          0%, 100% { transform: translateX(0); }
          10%, 30%, 50%, 70%, 90% { transform: translateX(-5px); }
          20%, 40%, 60%, 80% { transform: translateX(5px); }
        }
        
        @keyframes bounce-slow {
          0%, 100% { transform: translateY(0); }
          50% { transform: translateY(-10px); }
        }
        
        .animate-blob {
          animation: blob 7s infinite;
        }
        
        .animation-delay-2000 {
          animation-delay: 2s;
        }
        
        .animation-delay-4000 {
          animation-delay: 4s;
        }
        
        .animate-slide-in {
          animation: slideIn 0.4s ease-out;
        }
        
        .animate-slide-up {
          animation: slideUp 0.5s ease-out;
        }
        
        .animate-shake {
          animation: shake 0.5s ease-out;
        }
        
        .animate-bounce-slow {
          animation: bounce-slow 2s ease-in-out infinite;
        }
        
        .stop-icon {
          background: transparent !important;
          border: none !important;
        }
        
        /* Scrollbar styling */
        ::-webkit-scrollbar {
          width: 8px;
        }
        
        ::-webkit-scrollbar-track {
          background: rgba(255, 255, 255, 0.1);
          border-radius: 10px;
        }
        
        ::-webkit-scrollbar-thumb {
          background: linear-gradient(180deg, #3b82f6, #a855f7);
          border-radius: 10px;
        }
        
        ::-webkit-scrollbar-thumb:hover {
          background: linear-gradient(180deg, #2563eb, #9333ea);
        }
      `}</style>
    </div>
  );
}

export default Trajet;