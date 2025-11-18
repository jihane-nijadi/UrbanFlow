import { useState } from "react";
import authService from "../api/authService";
import { useNavigate } from "react-router-dom";
import { Mail, Lock, User, Users } from "lucide-react";
import "../styles/App.css";

export default function SignupForm({ setUser }) {
    const navigate = useNavigate();
    const [data, setData] = useState({
        firstName: "",
        lastName: "",
        email: "",
        password: "",
    });
    const [error, setError] = useState("");

    const submit = async (e) => {
        e.preventDefault();
        setError("");

        try {
            const user = await authService.register(data);
            setUser(user);
            navigate(`/`);
        } catch (err) {
            setError(err.message);
        }
    };

    return (
        <div className="auth-card">
            <div className="auth-header">
                <img src="/assets/logo.png" alt="logo" className="login-logo" />
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

                {/* Phone input removed per design change */}

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

                {/* Role selection removed - single user type */}

                <button type="submit" className="primary-button">
                    CREATE ACCOUNT
                </button>
            </form>
            <div className="form-footer">
                <p>
                    Already have an account? <a href="/auth" className="underline-link">Sign in</a>
                </p>
            </div>
        </div>
    );
}
