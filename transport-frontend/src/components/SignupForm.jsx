import { useState } from "react";
import authService from "../api/authService";
import { useNavigate } from "react-router-dom";
import { Mail, Lock, User, Users, ArrowRight } from "lucide-react";
import "../styles/App.css";

export default function SignupForm({ setUser }) {
    const navigate = useNavigate();
    const [data, setData] = useState({
        firstName: "",
        lastName: "",
        email: "",
        password: "",
        phoneNumber: "", // <-- ajouté
        role: "PASSENGER",
    });
    const [error, setError] = useState("");

    const submit = async (e) => {
        e.preventDefault();
        setError("");

        try {
            const user = await authService.register(data);
            setUser(user);
            navigate(`/${user.role.toLowerCase()}`);
        } catch (err) {
            setError(err.message);
        }
    };

    return (
        <div className="auth-card">
            <div className="auth-header">
                <div className="icon-circle">
                    <ArrowRight size={24} color="#FF8C00" />
                </div>
                <h2>Create Account</h2>
                <p>Sign up to get started</p>
            </div>

            {error && <div className="error-alert">{error}</div>}

            <form className="auth-form" onSubmit={submit}>
                <div className="grid-two">
                    <div className="input-group">
                        <label>First Name</label>
                        <div className="input-wrapper">
                            <User className="input-icon" size={20} />
                            <input
                                type="text"
                                placeholder="First Name"
                                value={data.firstName}
                                onChange={(e) => setData({ ...data, firstName: e.target.value })}
                                required
                            />
                        </div>
                    </div>

                    <div className="input-group">
                        <label>Last Name</label>
                        <div className="input-wrapper">
                            <Users className="input-icon" size={20} />
                            <input
                                type="text"
                                placeholder="Last Name"
                                value={data.lastName}
                                onChange={(e) => setData({ ...data, lastName: e.target.value })}
                                required
                            />
                        </div>
                    </div>
                </div>

                <div className="input-group">
                    <label>Email</label>
                    <div className="input-wrapper">
                        <Mail className="input-icon" size={20} />
                        <input
                            type="email"
                            placeholder="your.email@example.com"
                            value={data.email}
                            onChange={(e) => setData({ ...data, email: e.target.value })}
                            required
                        />
                    </div>
                </div>

                {/* Champ téléphone ajouté */}
                <div className="input-group">
                    <label>Phone Number</label>
                    <div className="input-wrapper">
                        <input
                            type="tel"
                            placeholder="+212 6 12 34 56 78"
                            value={data.phoneNumber}
                            onChange={(e) => setData({ ...data, phoneNumber: e.target.value })}
                            required
                        />
                    </div>
                </div>

                <div className="input-group">
                    <label>Password</label>
                    <div className="input-wrapper">
                        <Lock className="input-icon" size={20} />
                        <input
                            type="password"
                            placeholder="••••••••"
                            value={data.password}
                            onChange={(e) => setData({ ...data, password: e.target.value })}
                            required
                        />
                    </div>
                </div>

                <div className="input-group">
                    <label>Role</label>
                    <select
                        value={data.role}
                        onChange={(e) => setData({ ...data, role: e.target.value })}
                    >
                        <option value="PASSENGER">Passenger</option>
                        <option value="DRIVER">Driver</option>
                    </select>
                </div>

                <button type="submit" className="primary-button">
                    CREATE ACCOUNT
                </button>
            </form>
        </div>
    );
}
