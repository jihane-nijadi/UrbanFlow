import { useState, useEffect } from "react";
import TicketPurchase from "../components/TicketPurchase";
import MyTickets from "./MyTickets";
import { Ticket, MapPin, Bell } from "lucide-react";
import Sidebar from "../components/Sidebar";
import MapView from "../components/MapView";


export default function PassengerDashboard() {
    // default to the ticket purchase view so users land directly on Acheter Tickets after sign-in
    const [activeTab, setActiveTab] = useState("acheterTickets");
    const [focusBusId, setFocusBusId] = useState(null);
    const user = JSON.parse(localStorage.getItem("user") || "{}");


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
        } catch (e) {
            // ignore
        }
    }, []);

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
    const roleActions = [
        { tab: "acheterTickets", icon: <Ticket size={20} />, label: "Acheter tickets" },
        { tab: "gererTickets", icon: <Ticket size={20} />, label: "Gérer mes tickets" },
        { tab: "map", icon: <MapPin size={20} />, label: "Carte" },
        { tab: "notifications", icon: <Bell size={20} />, label: "Notifications" },
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
            case "notifications": return (
                <div className="card">
                    <h3>Notifications</h3>
                    <div style={{ marginTop: 12 }}>
                        <p style={{ color: '#6b7280' }}>Vous n'avez pas de nouvelles notifications.</p>
                    </div>
                </div>
            );
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
