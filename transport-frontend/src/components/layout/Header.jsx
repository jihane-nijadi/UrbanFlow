import React from "react";
import logo from "../../logo.png";
import "../../styles/App.css";

export default function Header() {
    return (
        <header className="nav-bar">
            <div className="nav-container">
                {/* LEFT SIDE: logo + text */}
                <div className="nav-left">
                    <img src={logo} alt="logo" className="nav-logo" />
                    <h1 className="nav-text">UrbanFlow</h1>
                </div>

                {/* RIGHT SIDE: reserved for buttons / actions */}
                <div className="nav-right">
                    {/* future buttons */}
                </div>
            </div>
        </header>
    );
}
