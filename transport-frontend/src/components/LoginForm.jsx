import { useState } from "react";
import authService from "../api/authService";
import { useNavigate } from "react-router-dom";
import { Mail, Lock, ArrowRight } from "lucide-react";
import "../styles/App.css";

export default function LoginForm({ setUser }) {
    const navigate = useNavigate();
    const [data, setData] = useState({ email: "", password: "" });
    const [error, setError] = useState("");

    const submit = async (e) => {
        e.preventDefault();
        setError("");

        try {
            const user = await authService.login(data);
            setUser(user); // important: update user state
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
                <h2>Welcome Back</h2>
                <p>Sign in to continue</p>
            </div>

            {error && <div className="error-alert">{error}</div>}

            <form className="auth-form" onSubmit={submit}>
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

                <button type="submit" className="primary-button">
                    SIGN IN
                </button>
            </form>
        </div>
    );
}
