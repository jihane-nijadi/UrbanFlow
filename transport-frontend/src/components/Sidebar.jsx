import { User, LogOut } from "lucide-react";

export default function Sidebar({ user, activeTab, setActiveTab, roleActions, handleLogout }) {
    const ORANGE = "#FF8C00";
    const ORANGE_HOVER = "#E67A00";
    const ORANGE_ACTIVE = "#CC7000";

    const buttonStyle = {
        display: "flex",
        alignItems: "center",
        gap: "0.6rem",
        padding: "0.75rem 1rem",
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
    const logoutButton = { ...buttonStyle, backgroundColor: "#f8f8f8", color: "#333" };
    const logoutButtonHover = { backgroundColor: "#eee" };

    return (
        <aside style={{
            width: "240px",
            backgroundColor: "white",
            padding: "1.5rem",
            display: "flex",
            flexDirection: "column",
            gap: "1.2rem",
            boxShadow: "2px 0 8px rgba(0,0,0,0.05)"
        }}>
            <h3 style={{ margin: 0, color: ORANGE, fontSize: "1.3rem", fontWeight: "bold" }}>Menu</h3>

            {/* Profile */}
            <button
                onClick={() => setActiveTab("profile")}
                style={activeTab === "profile" ? activeOrangeButton : orangeButton}
                onMouseEnter={(e) => activeTab !== "profile" && Object.assign(e.currentTarget.style, orangeButtonHover)}
                onMouseLeave={(e) => activeTab !== "profile" && Object.assign(e.currentTarget.style, orangeButton)}
            >
                <User size={20} /> Profile
            </button>

            {/* Logout */}
            <button
                onClick={handleLogout}
                style={logoutButton}
                onMouseEnter={(e) => Object.assign(e.currentTarget.style, logoutButtonHover)}
                onMouseLeave={(e) => Object.assign(e.currentTarget.style, logoutButton)}
            >
                <LogOut size={20} /> Logout
            </button>

            <hr style={{ border: "none", borderTop: "1px solid #eee", margin: "0.8rem 0" }} />

            {/* Role-specific actions */}
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
        </aside>
    );
}
