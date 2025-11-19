import React from "react";
import { LogOut } from 'lucide-react';

export default function Sidebar({ user, activeTab, setActiveTab, roleActions, handleLogout }) {
    const ORANGE = "#FF8C00";
    const ORANGE_HOVER = "#E67A00";
    const ORANGE_ACTIVE = "#CC7000";

    const buttonStyle = {
        display: "flex",
        alignItems: "center",
        gap: "0.6rem",
        padding: "0.5rem 0.8rem",
        border: "none",
        borderRadius: "8px",
        fontSize: "1rem",
        fontWeight: "500",
        cursor: "pointer",
        width: "100%",
        justifyContent: "flex-start",
        transition: "all 0.2s ease",
    };

    const orangeButton = { ...buttonStyle, backgroundColor: ORANGE, color: "white" };
    const orangeButtonHover = { backgroundColor: ORANGE_HOVER, transform: "translateY(-1px)", boxShadow: "0 4px 10px rgba(255,140,0,0.3)" };
    const activeOrangeButton = { ...orangeButton, backgroundColor: ORANGE_ACTIVE, fontWeight: "600", boxShadow: `0 0 0 2px ${ORANGE} inset` };
    const logoutButton = { ...buttonStyle, backgroundColor: ORANGE, color: "white" };
    const logoutButtonHover = { backgroundColor: ORANGE_HOVER, transform: "translateY(-1px)", boxShadow: "0 4px 10px rgba(255,140,0,0.3)" };

    return (
        <aside className="sidebar" style={{ width: 280, position: 'relative', background: 'transparent' }}>
            <div className="sidebar-inner" style={{ padding: '1.5rem', paddingTop: '2.6rem', display: 'flex', flexDirection: 'column', gap: '1.2rem', position: 'relative', zIndex: 1 }}>
                {/* Only render the provided roleActions (passenger requested: Acheter / Gérer tickets) */}
                {roleActions.map(({ tab, icon, label }) => (
                    <button
                        key={tab}
                        onClick={() => setActiveTab(tab)}
                        style={activeTab === tab ? activeOrangeButton : orangeButton}
                        onMouseEnter={(e) => activeTab !== tab && Object.assign(e.currentTarget.style, orangeButtonHover)}
                        onMouseLeave={(e) => activeTab !== tab && Object.assign(e.currentTarget.style, orangeButton)}
                    >
                        {icon} {label}
                    </button>
                ))}
                {/* Logout button anchored to the bottom */}
                <div style={{ marginTop: 'auto' }}>
                    <button
                        onClick={handleLogout}
                        style={logoutButton}
                        onMouseEnter={(e) => Object.assign(e.currentTarget.style, logoutButtonHover)}
                        onMouseLeave={(e) => Object.assign(e.currentTarget.style, logoutButton)}
                        aria-label="Logout"
                    >
                        <LogOut size={18} style={{ marginRight: 8 }} /> Se déconnecter
                    </button>
                </div>
            </div>
        </aside>
    );
}
