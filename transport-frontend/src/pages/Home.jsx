import React, { useEffect } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import "../styles/App.css";
import logo from "../logo.png";

export default function Home() {
  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    // scroll to the anchor if hash present, offset for sticky navbar
    const hash = location.hash;
    if (hash) {
      const id = hash.replace("#", "");
      const el = document.getElementById(id);
      if (el) {
        // compute a scroll position that accounts for the sticky navbar
        const navOffset = 64; // approximate sticky navbar height in px
        const rect = el.getBoundingClientRect();
        const elTop = rect.top + window.scrollY;
        const elHeight = rect.height;
        const available = window.innerHeight - navOffset - 16; // small breathing room
        // If the element height fits in the viewport (after navbar), scroll so it's fully visible
        let targetScroll = elTop - navOffset;
        if (elHeight > available) {
          // element is taller than available space; scroll the top into view under the navbar
          targetScroll = elTop - navOffset;
        }
        window.scrollTo({ top: targetScroll, behavior: "smooth" });
      }
    } else {
      // no hash: scroll to top (home)
      window.scrollTo({ top: 0, behavior: "smooth" });
    }
  }, [location]);

  return (
    <main className="home-page">
      <section className="hero">
        <div
          className="hero-bg"
            style={{ backgroundImage: "url('/assets/busurban.jpg')" }}
          aria-hidden
        />
        <div className="hero-inner container">
          <div className="hero-left">
            <div className="hero-content">
              <h1 className="hero-title">
               
                <span className="hero-title-rest">Let's have </span>
                <span className="hero-title-rest" >a smarter transport</span>
                <span className="hero-title-rest">  with UrbanFlow</span>
              </h1>
              <p className="hero-sub">Find routes, buy tickets and travel with confidence.</p>
              <div className="hero-arrow-wrap">
                <button className="nav-link hero-about-btn" onClick={() => navigate('/#about')}>About Us</button>
              </div>
            </div>
          </div>

          {/* hero-right removed per request (no decorative image) */}
        </div>
      </section>

      <hr className="section-separator-hr" />

      <section id="about" className="about container">
        <div className="card section-card">
          <h2 className="section-title">About Us</h2>
          <div className="section-body">
            <p>
              UrbanFlow is a lightweight microservices platform to manage routes, schedules
              and ticketing for urban transport. The goal of the app is to provide a simple
              and reliable way for passengers to discover routes and purchase tickets, while
              offering operators an extensible backend to integrate services and data.
            </p>
            <p>
              Key features: fast route search, trip management, ticket history and payment
              integrations. UrbanFlow helps modernize public transport services with an
              intuitive interface and modular backend.
            </p>
          </div>
        </div>
      </section>

      <section id="contact" className="contact container">
        <div className="card section-card">
          <h2 className="section-title">Contact</h2>
          <div className="section-body">
            <p>
              For bug reports, feature requests or technical support, contact us via email
              at <strong>support@urbanflow.local</strong> or use the contact form on our
              website.
            </p>
            <p>
              Support hours: Mon-Fri 09:00 - 18:00. We aim to respond within 48 business
              hours.
            </p>
          </div>
        </div>
      </section>
    </main>
  );
}
