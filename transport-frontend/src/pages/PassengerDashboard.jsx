import { useState, useEffect } from "react";
import { useLocation } from 'react-router-dom';
import TicketPurchase from "../components/TicketPurchase";
import MyTickets from "./MyTickets";
import { Ticket, MapPin, CreditCard } from "lucide-react";
import PaymentDashboard from "./PaymentDashboard";
import Sidebar from "../components/Sidebar";
import MapView from "../components/MapView";


export default function PassengerDashboard() {
    // default to the ticket purchase view so users land directly on Acheter Tickets after sign-in
    const [activeTab, setActiveTab] = useState("acheterTickets");
    const [focusBusId, setFocusBusId] = useState(null);
    const user = JSON.parse(localStorage.getItem("user") || "{}");
    const location = useLocation();


    const handleLogout = () => {
        localStorage.removeItem("user");
        window.location.href = "/";
    };

    // On mount, check if a ticket asked to follow a bus
    useEffect(() => {
        try {
            const b = localStorage.getItem('uf_focus_bus');
            if (b) {
                setActiveTab('map');
                setFocusBusId(b);
                localStorage.removeItem('uf_focus_bus');
            }
            // removed uf_open_tab redirect handling (reverted)
        } catch (e) {
            // ignore
        }
    }, []);

    // If we are navigated to /passenger with a session query param, open the payments tab
    useEffect(() => {
        try {
            const params = new URLSearchParams(location.search);
            const session = params.get('session');
            if (session) {
                setActiveTab('payments');
            }
        } catch (e) {
            // ignore
        }
    }, [location.search]);

    // Also respond to storage events (in case follow was triggered in another tab)
    useEffect(() => {
        const handler = (e) => {
            if (e.key === 'uf_focus_bus' && e.newValue) {
                setActiveTab('map');
                setFocusBusId(e.newValue);
                localStorage.removeItem('uf_focus_bus');
            }
        };
        window.addEventListener('storage', handler);
        return () => window.removeEventListener('storage', handler);
    }, []);

    // Sidebar actions for passengers: only Acheter and Gérer tickets
    // Remove direct 'Paiement' action from sidebar; payments only available via purchase flow
    const roleActions = [
        { tab: "acheterTickets", icon: <Ticket size={20} />, label: "Acheter tickets" },
        { tab: "gererTickets", icon: <Ticket size={20} />, label: "Gérer mes tickets" },
        { tab: "map", icon: <MapPin size={20} />, label: "Carte" },
    ];

    const renderContent = () => {
        switch(activeTab){
            case "acheterTickets": return <TicketPurchase />;
            case "gererTickets": return <MyTickets />;
            case "map": return (
                <>
                    <div style={{ marginBottom: 12 }}>
                        <p style={{ color: '#ffffff', margin: 0, fontFamily: 'Georgia, "Times New Roman", Times, serif', fontWeight: 300 }}>Visualisation en temps réel des bus.</p>
                    </div>
                    <div className="card">
                        <div id="map-preview" style={{ height: 420, borderRadius: 12, overflow: 'hidden', marginTop: 12 }}>
                            <MapView focusBusId={focusBusId} />
                        </div>
                    </div>
                </>
            );
            case "payments": return <PaymentDashboard />;
            default: return <div>Bienvenue, {user.firstName}!</div>;
        }
    };

    return (
        <div className="passenger-dashboard-root" style={{ position: 'relative', minHeight: '100vh' }}>
            {/* full-dashboard runtime background so both sidebar and main share the same blurred image */}
            <div className="dashboard-bg" style={{ backgroundImage: "url('/assets/busurban.jpg')" }} aria-hidden />
            <div style={{ display: "flex", height: "100vh", position: 'relative', zIndex: 2 }}>
                <Sidebar user={user} activeTab={activeTab} setActiveTab={setActiveTab} roleActions={roleActions} handleLogout={handleLogout} />
                <main style={{ flex: 1, padding: "2.5rem", overflowY: "auto" }}>
                    {renderContent()}
                </main>
            </div>
        </div>
    );
}
