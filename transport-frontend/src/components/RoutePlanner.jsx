import { useState } from "react";

export default function RoutePlanner() {
    const [from, setFrom] = useState("");
    const [to, setTo] = useState("");
    const [plan, setPlan] = useState(null);

    const searchRoute = async () => {
        try {
            const resp = await fetch(`/api/routes/plan?from=${from}&to=${to}`);
            const data = await resp.json();
            setPlan(data);
        } catch (err) {
            console.error(err);
        }
    }

    return (
        <div>
            <h2>Route Planner - Casablanca</h2>
            <input placeholder="Départ" value={from} onChange={e => setFrom(e.target.value)} />
            <input placeholder="Arrivée" value={to} onChange={e => setTo(e.target.value)} />
            <button onClick={searchRoute}>Rechercher</button>

            {plan && !plan.error && (
                <div>
                    <h3>Route {plan.routeId}</h3>
                    <p>{plan.departureStop} → {plan.arrivalStop}</p>
                    <p>Départ: {plan.departureTime} - Arrivée: {plan.arrivalTime}</p>
                    <h4>Buses disponibles:</h4>
                    <ul>
                        {plan.busesAvailable.map(bus => (
                            <li key={bus.vehicleId}>{bus.vehicleId} ({bus.latitude}, {bus.longitude})</li>
                        ))}
                    </ul>
                </div>
            )}

            {plan?.error && <p style={{color:'red'}}>{plan.error}</p>}
        </div>
    )
}
