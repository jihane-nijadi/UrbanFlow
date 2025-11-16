import { useState } from "react";
import authService from "../api/authService";

export function AjouterAdmin() {
    const [data, setData] = useState({
        firstName: "",
        lastName: "",
        email: "",
        phoneNumber: "",
        password: "",
        role: "ADMINISTRATOR",
    });
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    const submit = async (e) => {
        e.preventDefault();
        setError("");
        setSuccess("");

        try {
            await authService.register(data);
            setSuccess("Administrateur ajouté avec succès !");
            setData({
                firstName: "",
                lastName: "",
                email: "",
                phoneNumber: "",
                password: "",
                role: "ADMINISTRATOR",
            });
        } catch (err) {
            setError(err.message);
        }
    };

    return (
        <div style={{ maxWidth: "500px", margin: "0 auto", background: "#fff", padding: "2rem", borderRadius: "12px", boxShadow: "0 2px 8px rgba(0,0,0,0.05)" }}>
            <h2>Ajouter un Administrateur</h2>

            {error && <p style={{ color: "red" }}>{error}</p>}
            {success && <p style={{ color: "green" }}>{success}</p>}

            <form onSubmit={submit} style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
                <input
                    type="text"
                    placeholder="First Name"
                    value={data.firstName}
                    onChange={(e) => setData({ ...data, firstName: e.target.value })}
                    required
                />
                <input
                    type="text"
                    placeholder="Last Name"
                    value={data.lastName}
                    onChange={(e) => setData({ ...data, lastName: e.target.value })}
                    required
                />
                <input
                    type="email"
                    placeholder="Email"
                    value={data.email}
                    onChange={(e) => setData({ ...data, email: e.target.value })}
                    required
                />
                <input
                    type="text"
                    placeholder="Phone Number"
                    value={data.phoneNumber}
                    onChange={(e) => setData({ ...data, phoneNumber: e.target.value })}
                />
                <input
                    type="password"
                    placeholder="Password"
                    value={data.password}
                    onChange={(e) => setData({ ...data, password: e.target.value })}
                    required
                />

                <button type="submit" style={{ padding: "0.75rem", backgroundColor: "#FF8C00", color: "white", border: "none", borderRadius: "8px", cursor: "pointer" }}>
                    Ajouter Admin
                </button>
            </form>
        </div>
    );
}
