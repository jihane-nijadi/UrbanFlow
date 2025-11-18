export default function Profile({ user }) {
    const ORANGE = "#FF8C00";

    return (
        <div style={{
            padding: "1.5rem",
            backgroundColor: "#fff",
            borderRadius: "12px",
            boxShadow: "0 2px 8px rgba(0,0,0,0.05)",
            maxWidth: "500px",
            margin: "0 auto"
        }}>
            <div style={{ display: "flex", alignItems: "center", gap: "1rem", marginBottom: "1.2rem" }}>
                <div style={{
                    width: "56px",
                    height: "56px",
                    borderRadius: "50%",
                    backgroundColor: ORANGE,
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    color: "white",
                    fontWeight: "bold",
                    fontSize: "1.2rem"
                }}>
                    {user.firstName?.[0]}{user.lastName?.[0]}
                </div>
                <h2 style={{ margin: 0, fontSize: "1.4rem" }}>{user.firstName} {user.lastName}</h2>
            </div>
            <div style={{ fontSize: "1rem", color: "#444" }}>
                <p style={{ margin: "0.5rem 0" }}><strong>Email:</strong> {user.email}</p>
                <p style={{ margin: "0.5rem 0" }}><strong>Phone:</strong> {user.phoneNumber || "Not provided"}</p>
            </div>
        </div>
    );
}
