import { useState, useEffect } from "react";
import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import Home from "./pages/Home";
import SignInPage from "./pages/SignInPage";
import SignUpPage from "./pages/SignUpPage";
import AdminDashboard from "./pages/AdminDashboard";
import PassengerDashboard from "./pages/PassengerDashboard";
import DriverDashboard from "./pages/DriverDashboard";
import authService from "./api/authService";
import Header from "./components/layout/Header";

function App() {
    const [user, setUser] = useState(null);

    // Charger l'utilisateur stocké au démarrage
    useEffect(() => {
        const storedUser = authService.getCurrentUser();
        if (storedUser) setUser(storedUser);
    }, []);

    // Fonction pour obtenir la route correcte selon le rôle
    const getRouteByRole = (role) => {
        switch (role) {
            case "ADMINISTRATOR":
                return "/admin";
            case "PASSENGER":
                return "/passenger";
            case "DRIVER":
                return "/driver";
            default:
                return "/";
        }
    };

    return (
        <Router>
            <Header />
            <Routes>
                {/* Public routes */}
                <Route path="/" element={<Home />} />
                <Route path="/auth" element={<SignInPage setUser={setUser} />} />
                <Route path="/signup" element={<SignUpPage setUser={setUser} />} />

                {/* Routes protégées */}
                <Route
                    path="/admin"
                    element={
                        user?.role === "ADMINISTRATOR" ? (
                            <AdminDashboard />
                        ) : (
                            <Navigate to="/" replace />
                        )
                    }
                />
                <Route
                    path="/passenger"
                    element={
                        user?.role === "PASSENGER" ? (
                            <PassengerDashboard />
                        ) : (
                            <Navigate to="/" replace />
                        )
                    }
                />
                <Route
                    path="/driver"
                    element={
                        user?.role === "DRIVER" ? (
                            <DriverDashboard />
                        ) : (
                            <Navigate to="/" replace />
                        )
                    }
                />

                {/* Catch-all */}
                <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
        </Router>
    );
}

export default App;
