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

        <div className="auth-right">
          <div className=" illustration-box">
            <div className="merged-image large">
              <img
                src="/assets/merged.png"
                alt="bus and ticket"
                onError={(e) => {
                  if (e.target.src.indexOf("/assets/img.png") === -1) e.target.src = "/assets/img.png";
                }}
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
