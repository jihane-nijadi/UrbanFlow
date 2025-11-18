import React from "react";
import LoginForm from "../components/LoginForm";
import "../styles/App.css";
import { Link } from "react-router-dom";

export default function SignInPage({ setUser }) {
  return (
    <div className="auth-layout container">
      <div className="auth-two-col">
        <div className="auth-left">
          <LoginForm setUser={setUser} />
          <div className="auth-footer">
            <p>
              Don't have an account ? <Link to="/signup" className="underline-link">Sign up</Link>
            </p>
          </div>
        </div>

        <div className="auth-right">
          <div className=" illustration-box">
              <div className="merged-image large">
                <img
                  src="/merged.png"
                  alt="bus and ticket"
                  onError={(e) => {
                    if (e.target.src.indexOf("/img.png") === -1) e.target.src = "/img.png";
                  }}
                />
              </div>
          </div>
        </div>
      </div>
    </div>
  );
}
