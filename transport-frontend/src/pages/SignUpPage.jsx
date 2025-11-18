import React from "react";
import SignupForm from "../components/SignupForm";
import "../styles/App.css";

export default function SignUpPage({ setUser }) {
  return (
    <div className="auth-layout container">
      <div className="auth-two-col">
        <div className="auth-left">
          <SignupForm setUser={setUser} />
        </div>
      </div>
    </div>
  );
}
