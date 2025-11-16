import { useState } from "react";
import { User, MapPin, Bus, Ticket } from "lucide-react";
import Sidebar from "../components/Sidebar";
import Profile from "../components/Profile";
import { AjouterAdmin } from "./AjouterAdmin";

export default function AdminDashboard() {
    const [activeTab, setActiveTab] = useState("dashboard");
    const user = JSON.parse(localStorage.getItem("user") || "{}");

    const handleLogout = () => {
        localStorage.removeItem("user");
        window.location.href = "http://localhost:3000";
    };

    const roleActions = [
        { tab: "stats", icon: <MapPin size={20} />, label: "Statistiques" },
        { tab: "users", icon: <User size={20} />, label: "Utilisateurs" },
        { tab: "buses", icon: <Bus size={20} />, label: "Buses" },
        { tab: "tickets", icon: <Ticket size={20} />, label: "Tickets achetés" },
        { tab: "addAdmin", icon: <User size={20} />, label: "Ajouter Admin" },
    ];

    const renderContent = () => {
        switch(activeTab){
            case "profile": return <Profile user={user} />;
            case "stats": return <div>Statistiques</div>;
            case "users": return <div>Gestion utilisateurs</div>;
            case "buses": return <div>Gestion buses</div>;
            case "tickets": return <div>Tickets achetés</div>;
            case "addAdmin": return <AjouterAdmin />;
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
