import { useState } from "react";
import { MapPin, Bus, Ticket } from "lucide-react";
import Sidebar from "../components/Sidebar";
import Profile from "../components/Profile";
import BusTracker from "../components/BusTracker";
import Trajet from "../components/Trajet";

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
            case "trajets": return <Trajet />;
            case "buses": return <BusTracker />;
            case "acheterTickets": return <div>Acheter des tickets</div>;
            case "gererTickets": return <div>Gérer mes tickets</div>;
            default: return <div>Welcome, {user.firstName}!</div>;
        }
    };

    const isFullScreen = activeTab === "trajets" || activeTab === "buses";

    return (
        <div style={{ display: "flex", height: "100vh", backgroundColor: "#f9f9fb" }}>
            <Sidebar user={user} activeTab={activeTab} setActiveTab={setActiveTab} roleActions={roleActions} handleLogout={handleLogout} />
            <main style={{ flex: 1, padding: isFullScreen ? "0" : "2.5rem", overflowY: isFullScreen ? "hidden" : "auto" }}>
                {renderContent()}
            </main>
        </div>
    );
}
