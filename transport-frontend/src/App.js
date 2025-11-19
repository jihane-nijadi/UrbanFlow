import { useState, useEffect } from "react";
import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import Home from "./pages/Home";
import SignInPage from "./pages/SignInPage";
import SignUpPage from "./pages/SignUpPage";
import AdminDashboard from "./pages/AdminDashboard";
import PassengerDashboard from "./pages/PassengerDashboard";
import PaymentDashboard from "./pages/PaymentDashboard";
import DriverDashboard from "./pages/DriverDashboard";
import authService from "./api/authService";
import Header from "./components/layout/Header";
import MapPage from "./pages/MapPage";

function App() {
    const [user, setUser] = useState(null);

    // Charger l'utilisateur stocké au démarrage
    useEffect(() => {
        const storedUser = authService.getCurrentUser();
        if (storedUser) setUser(storedUser);
    }, []);

    // Role-based routing removed: routes will be guarded by authentication only.

    return (
        <Router>
            <Header />
            <Routes>
                {/* Public routes */}
                <Route path="/" element={<Home />} />
                <Route path="/auth" element={<SignInPage setUser={setUser} />} />
                <Route path="/signup" element={<SignUpPage setUser={setUser} />} />

                {/* Routes protégées */}
                <Route path="/admin" element={user ? <AdminDashboard /> : <Navigate to="/" replace />} />
                <Route path="/passenger" element={user ? <PassengerDashboard /> : <Navigate to="/" replace />} />
                {/* Standalone payment route used when opening the payment form in a separate tab */}
                <Route path="/payment" element={<PaymentDashboard />} />
                <Route path="/payment/:ticketId" element={<PaymentDashboard />} />
                <Route path="/driver" element={user ? <DriverDashboard /> : <Navigate to="/" replace />} />
                <Route path="/carte" element={<MapPage />} />

                {/* Catch-all */}
                <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
        </Router>
    );
}

export default App;
