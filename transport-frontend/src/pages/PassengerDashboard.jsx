import { useState } from "react";
import TicketPurchase from "../components/TicketPurchase";
import MyTickets from "./MyTickets";
import { Ticket, MapPin, Bell } from "lucide-react";
import Sidebar from "../components/Sidebar";


export default function PassengerDashboard() {
    // default to the ticket purchase view so users land directly on Acheter Tickets after sign-in
    const [activeTab, setActiveTab] = useState("acheterTickets");
    const user = JSON.parse(localStorage.getItem("user") || "{}");


    const handleLogout = () => {
        localStorage.removeItem("user");
        window.location.href = "/";
    };

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
                <div className="card">
                    <h3>Carte</h3>
                    <div id="map-preview" style={{ height: 420, borderRadius: 12, background: '#eef2f6', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                        <span style={{ color: '#6b7280' }}>Aperçu de la carte (placeholder)</span>
                    </div>
                </div>
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
