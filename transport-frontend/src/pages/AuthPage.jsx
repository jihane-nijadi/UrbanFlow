import { useState } from "react";
import LoginForm from "../components/LoginForm";
import SignupForm from "../components/SignupForm";
import "../styles/App.css";

export default function AuthPage({ setUser }) {
    const [tab, setTab] = useState("login");

    return (
        <div className="auth-page">
            {/* Onglets Sign In / Sign Up */}
            <div className="auth-tabs">
                <button
                    className={tab === "login" ? "tab active" : "tab"}
                    onClick={() => setTab("login")}
                >
                    Sign In
                </button>
                <button
                    className={tab === "signup" ? "tab active" : "tab"}
                    onClick={() => setTab("signup")}
                >
                    Sign Up
                </button>
            </div>

            {/* Formulaire */}
            {tab === "login" ? (
                <LoginForm setUser={setUser} />
            ) : (
                <SignupForm setUser={setUser} />
            )}
        </div>
    );
}
