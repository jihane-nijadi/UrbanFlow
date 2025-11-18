import React from "react";
import LoginForm from "../components/LoginForm";
import "../styles/App.css";
import { Link } from "react-router-dom";

export default function SignInPage({ setUser }) {
  // Right column removed per user request — simplified single-column sign-in

  return (
    <div className="auth-layout container">
      <div className="auth-two-col">
        <div className="auth-left">
          <LoginForm setUser={setUser} />
        </div>
      </div>
    </div>
  );
}
