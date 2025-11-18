import { useState } from "react";
import Sidebar from "../components/Sidebar";
import Profile from "../components/Profile";

export default function DriverDashboard() {
    const [activeTab, setActiveTab] = useState("dashboard");
    const user = JSON.parse(localStorage.getItem("user") || "{}");

    const handleLogout = () => {
        localStorage.removeItem("user");
        window.location.href = "/";
    };

    const roleActions = [
        { tab: "myRoutes", label: "Mes trajets" },
        { tab: "myBus", label: "Mes bus" },
        // Ajouter ici d'autres actions spécifiques pour driver
    ];

    const renderContent = () => {
        switch(activeTab){
            case "profile": return <Profile user={user} />;
            case "myRoutes": return <div>Mes trajets</div>;
            case "myBus": return <div>Mes bus</div>;
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
