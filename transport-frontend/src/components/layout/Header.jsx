import React from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import logo from "../../logo.png";
import "../../styles/App.css";

export default function Header() {
    const location = useLocation();
    const navigate = useNavigate();

    const hash = location.hash || "";
    const pathname = location.pathname || "/";

    // Hide header when showing standalone payment page (clean payment UI in new tab)
    if (pathname.startsWith('/payment')) return null;

    const isHomeActive = pathname === "/" && (hash === "" || hash === "#" || !hash);
    const isAboutActive = hash === "#about";
    const isContactActive = hash === "#contact";

    const goTo = (section) => {
        if (!section) {
            navigate("/");
            return;
        }
        // navigate using hash so Home can react to it and scroll
        navigate(`/#${section}`);
    };

    return (
        <header className="nav-bar">
            <div className="nav-bg" style={{ '--nav-bg-image': "url('/assets/busurban.jpg')" }} aria-hidden />
            <div className="nav-container">
                {/* LEFT SIDE: logo + text */}
                <div className="nav-left">
                    <img src={logo} alt="logo" className="nav-logo" />
                    <h1 className="nav-text" style={{ color: '#ffffff' }}>UrbanFlow</h1>
                </div>

                {/* RIGHT SIDE: buttons */}
                <div className="nav-right">
                    <button
                        className={`nav-link ${isHomeActive ? "active" : ""}`}
                        onClick={() => goTo("")}
                        aria-current={isHomeActive ? "page" : undefined}
                    >
                        Home
                    </button>

                    <button
                        className={`nav-link ${isAboutActive ? "active" : ""}`}
                        onClick={() => goTo("about")}
                    >
                        About
                    </button>

                    <button
                        className={`nav-link ${isContactActive ? "active" : ""}`}
                        onClick={() => goTo("contact")}
                    >
                        Contact
                    </button>

                    {/* carte link removed per request; PassengerDashboard still contains the map tab */}

                    <Link to="/auth" className="primary-button nav-signin">
                        Sign In
                    </Link>
                </div>
            </div>
        </header>
    );
}
