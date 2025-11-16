import { useState } from "react";
import { MapPin, Bus, Ticket } from "lucide-react";
import Sidebar from "../components/Sidebar";
import Profile from "../components/Profile";

export default function PassengerDashboard() {
    const [activeTab, setActiveTab] = useState("dashboard");
    const user = JSON.parse(localStorage.getItem("user") || "{}");

    const handleLogout = () => {
        localStorage.removeItem("user");
        window.location.href = "http://localhost:3000";
    };

    const roleActions = [
        { tab: "trajets", icon: <MapPin size={20} />, label: "Trajets" },
        { tab: "buses", icon: <Bus size={20} />, label: "Bus disponibles" },
        { tab: "acheterTickets", icon: <Ticket size={20} />, label: "Acheter tickets" },
        { tab: "gererTickets", icon: <Ticket size={20} />, label: "Gérer mes tickets" },
    ];

    const renderContent = () => {
        switch(activeTab){
            case "profile": return <Profile user={user} />;
            case "trajets": return <div>Consulter les trajets</div>;
            case "buses": return <div>Consulter les bus disponibles</div>;
            case "acheterTickets": return <div>Acheter des tickets</div>;
            case "gererTickets": return <div>Gérer mes tickets</div>;
            default: return <div>Welcome, {user.firstName}!</div>;
        }
    };

    return (
        <div style={{ display: "flex", height: "100vh", backgroundColor: "#f9f9fb" }}>
            <Sidebar user={user} activeTab={activeTab} setActiveTab={setActiveTab} roleActions={roleActions} handleLogout={handleLogout} />
            <main style={{ flex: 1, padding: "2.5rem", overflowY: "auto" }}>
                {renderContent()}
            </main>
        </div>
    );
}
